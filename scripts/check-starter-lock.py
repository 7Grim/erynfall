#!/usr/bin/env python3
"""
scripts/check-starter-lock.py

Programmatic check for every item in docs/STARTER_VISUAL_LOCK.md.

Starter area: x=20–95, y=62–130 in main_world.

Exit code: 0 (all pass) or 1 (one or more failures).

Usage:
    python3 scripts/check-starter-lock.py
    python3 scripts/check-starter-lock.py --json
    python3 scripts/check-starter-lock.py --exit-zero
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT              = Path(__file__).resolve().parents[1]
SCRIPTS           = Path(__file__).parent
ENTITY_VISUALS    = ROOT / "art" / "world"  / "entity_visuals.yaml"
NPCS_YAML         = ROOT / "assets" / "data" / "npcs.yaml"
MAIN_SCENE        = ROOT / "art" / "worlds" / "main_world" / "scene.yaml"
SANDBOX_SCENE     = ROOT / "art" / "worlds" / "sandbox"    / "scene.yaml"
MODELS_DIR        = ROOT / "art" / "models"
MANIFEST_PATH     = ROOT / "art" / "models" / "manifest.yaml"
REVIEW_DIR        = ROOT / "review" / "screenshots" / "actor"

STARTER_X_MIN, STARTER_X_MAX = 20, 95
STARTER_Y_MIN, STARTER_Y_MAX = 62, 130


# ── Loaders ────────────────────────────────────────────────────────────────────

def _load_pipeline_common():
    src = SCRIPTS / "blender" / "_pipeline_common.py"
    if not src.exists():
        raise FileNotFoundError(f"Required helper not found: {src}")
    spec = importlib.util.spec_from_file_location("_pipeline_common", src)
    mod  = importlib.util.module_from_spec(spec)
    sys.modules["_pipeline_common"] = mod
    spec.loader.exec_module(mod)  # type: ignore[union-attr]
    return mod

def _load_validate_models():
    src = SCRIPTS / "validate-models.py"
    spec = importlib.util.spec_from_file_location("_validate_models", src)
    mod  = importlib.util.module_from_spec(spec)
    sys.modules["_validate_models"] = mod
    spec.loader.exec_module(mod)  # type: ignore[union-attr]
    return mod

_pc = _load_pipeline_common()
load_yaml = _pc.load_yaml

_vm = _load_validate_models()
parse_manifest = _vm.parse_manifest


# ── Check result ───────────────────────────────────────────────────────────────

@dataclass
class CheckResult:
    name:    str
    passed:  bool
    detail:  str


def _pass(name: str, detail: str = "") -> CheckResult:
    return CheckResult(name=name, passed=True, detail=detail)

def _fail(name: str, detail: str) -> CheckResult:
    return CheckResult(name=name, passed=False, detail=detail)


# ── Individual checks ──────────────────────────────────────────────────────────

def check_no_billboard_actors_in_starter() -> CheckResult:
    name = "No billboard actors in starter area"
    try:
        npcs_data = load_yaml(NPCS_YAML)
        ev_data   = load_yaml(ENTITY_VISUALS)
    except Exception as exc:
        return _fail(name, f"Cannot load YAML: {exc}")

    npcs = npcs_data.get("npcs", [])
    ev   = {e["definition_id"]: e for e in ev_data.get("entity_visuals", [])
            if "definition_id" in e}

    try:
        manifest_entries = parse_manifest(MANIFEST_PATH)
        manifest_keys = {e.key for e in manifest_entries}
    except Exception:
        manifest_keys = set()

    problems: list[str] = []
    for npc in npcs:
        sx = npc.get("spawn_x", -1)
        sy = npc.get("spawn_y", -1)
        if not (STARTER_X_MIN <= sx <= STARTER_X_MAX and STARTER_Y_MIN <= sy <= STARTER_Y_MAX):
            continue
        did    = npc.get("definition_id", -1)
        entry  = ev.get(did, {})
        key3d  = entry.get("model_key_3d") or None
        if key3d is None:
            problems.append(f"def_id={did} '{npc.get('name')}' at ({sx},{sy}): no model_key_3d")
            continue
        glb = MODELS_DIR / f"{key3d}.glb"
        if not glb.exists() and key3d not in manifest_keys:
            problems.append(f"def_id={did} '{npc.get('name')}' at ({sx},{sy}): GLB missing for '{key3d}'")

    if problems:
        return _fail(name, "; ".join(problems))
    return _pass(name, "All starter-area NPCs have valid 3D models")


def check_no_g3dj_assets() -> CheckResult:
    name = "No G3DJ assets in use"
    try:
        manifest_entries = parse_manifest(MANIFEST_PATH)
    except Exception as exc:
        return _fail(name, f"Cannot parse manifest: {exc}")

    g3dj = [e.key for e in manifest_entries if e.format == "g3dj"]
    if g3dj:
        return _fail(name, f"{len(g3dj)} G3DJ entries still in manifest: {', '.join(g3dj[:8])}")
    return _pass(name, "No G3DJ entries in manifest")


def check_building_shells_pass_scene_validator() -> CheckResult:
    name = "Building shells pass validate-scene.py (main_world)"
    cmd  = [sys.executable, str(SCRIPTS / "validate-scene.py"),
            "--world-id", "main_world", "--json", "--exit-zero"]
    result = subprocess.run(cmd, capture_output=True, text=True, cwd=str(ROOT))
    try:
        findings = json.loads(result.stdout)
    except json.JSONDecodeError:
        return _fail(name, "validate-scene.py produced non-JSON output")

    shell_codes = {"SHELL_SCALE_BELOW_MIN", "SHELL_BASE_WITHOUT_ROOF", "SHELL_ROOF_WITHOUT_BASE"}
    critical_shell = [f for f in findings
                      if f.get("code") in shell_codes and f.get("level") == "CRITICAL"]
    if critical_shell:
        codes = ", ".join(f"{f['code']}:{f.get('key','')} " for f in critical_shell[:5])
        return _fail(name, f"{len(critical_shell)} critical shell finding(s): {codes}")
    return _pass(name, "No critical shell findings in main_world")


def check_resource_entities_have_3d() -> CheckResult:
    name = "Resource entities have 3D models"
    try:
        ev_data = load_yaml(ENTITY_VISUALS)
    except Exception as exc:
        return _fail(name, f"Cannot load entity_visuals.yaml: {exc}")

    resource_def_ids = set(range(100, 400))  # trees (100-199) + resources (200-399)
    problems: list[str] = []

    for e in ev_data.get("entity_visuals", []):
        did = e.get("definition_id", -1)
        if did not in resource_def_ids:
            continue
        key3d = e.get("model_key_3d") or None
        if key3d is None:
            problems.append(f"def_id={did} '{e.get('name')}': no model_key_3d")
            continue
        glb = MODELS_DIR / f"{key3d}.glb"
        if not glb.exists():
            problems.append(f"def_id={did}: GLB missing for '{key3d}'")

    if problems:
        return _fail(name, "; ".join(problems[:5]))
    return _pass(name, "All resource/tree entities have 3D models on disk")


def check_player_base_no_criticals() -> CheckResult:
    name = "player_base has no CRITICAL audit findings"
    cmd  = [sys.executable, str(SCRIPTS / "audit-assets.py"),
            "--key", "player_base", "--json", "--exit-zero"]
    result = subprocess.run(cmd, capture_output=True, text=True, cwd=str(ROOT))
    try:
        findings = json.loads(result.stdout)
    except json.JSONDecodeError:
        return _fail(name, "audit-assets.py produced non-JSON output")

    criticals = [f for f in findings if f.get("level") == "CRITICAL"]
    if criticals:
        codes = ", ".join(f["code"] for f in criticals[:5])
        return _fail(name, f"{len(criticals)} CRITICAL finding(s): {codes}")
    return _pass(name, "player_base passes audit with no CRITICAL findings")


def check_screenshot_set_exists() -> CheckResult:
    name = "player_base screenshot set exists"
    required = [
        REVIEW_DIR / "player_base_front.png",
        REVIEW_DIR / "player_base_right.png",
        REVIEW_DIR / "player_base_rear.png",
        REVIEW_DIR / "player_base_iso.png",
        REVIEW_DIR / "player_base.json",
    ]
    missing = [p.name for p in required if not p.exists()]
    if missing:
        return _fail(name, f"Missing in review/screenshots/actor/: {', '.join(missing)}")
    return _pass(name, f"All 4 views + sidecar JSON present in {REVIEW_DIR.relative_to(ROOT)}")


def check_sandbox_scene_passes() -> CheckResult:
    name = "Sandbox scene.yaml passes validate-scene.py (zero CRITICAL, no TERRAIN_MISSING_DEFAULT)"
    cmd  = [sys.executable, str(SCRIPTS / "validate-scene.py"),
            "--world-id", "sandbox", "--json", "--exit-zero"]
    result = subprocess.run(cmd, capture_output=True, text=True, cwd=str(ROOT))
    try:
        findings = json.loads(result.stdout)
    except json.JSONDecodeError:
        return _fail(name, "validate-scene.py produced non-JSON output")

    criticals = [f for f in findings if f.get("level") == "CRITICAL"]
    missing_default = [f for f in findings if f.get("code") == "TERRAIN_MISSING_DEFAULT"]
    problems: list[str] = []
    if criticals:
        problems.append(f"{len(criticals)} CRITICAL finding(s)")
    if missing_default:
        problems.append("TERRAIN_MISSING_DEFAULT warning present")
    if problems:
        return _fail(name, "; ".join(problems))
    return _pass(name, "Sandbox scene passes all checks")


def check_sandbox_prop_coverage() -> CheckResult:
    name = "Sandbox scene has prop category coverage"
    try:
        scene = _pc.load_scene(SANDBOX_SCENE)
    except Exception as exc:
        return _fail(name, f"Cannot load sandbox scene.yaml: {exc}")

    keys = {p.key for p in scene.static_props}

    required_categories = {
        "tree":    any(k.startswith("tree") for k in keys),
        "rock":    any(k.startswith("rock_") for k in keys),
        "shell":   any("building_shell" in k for k in keys),
        "station": any(k in {"furnace", "cooking_range", "anvil"} for k in keys),
    }

    missing = [cat for cat, present in required_categories.items() if not present]
    if missing:
        return _fail(name, f"Missing prop categories in sandbox: {', '.join(missing)}")
    return _pass(name, "tree, rock, shell, and crafting station all present in sandbox")


# ── Runner ─────────────────────────────────────────────────────────────────────

ALL_CHECKS = [
    check_no_billboard_actors_in_starter,
    check_no_g3dj_assets,
    check_building_shells_pass_scene_validator,
    check_resource_entities_have_3d,
    check_player_base_no_criticals,
    check_screenshot_set_exists,
    check_sandbox_scene_passes,
    check_sandbox_prop_coverage,
]


def print_text_report(results: list[CheckResult]) -> None:
    width = 72
    passed = sum(1 for r in results if r.passed)
    failed = len(results) - passed

    print("═" * width)
    print("ERYNFALL  STARTER VISUAL LOCK CHECK")
    print("═" * width)
    print(f"Checks: {len(results)}  |  Passed: {passed}  |  Failed: {failed}")
    print()

    for r in results:
        icon = "✓" if r.passed else "✗"
        print(f"  {icon}  {r.name}")
        if r.detail:
            print(f"       {r.detail}")

    print()
    print("─" * width)
    if failed == 0:
        print("✅  All starter-area visual lock checks pass.")
    else:
        print(f"❌  {failed} check(s) failed. See docs/STARTER_VISUAL_LOCK.md.")
    print("═" * width)


def print_json_report(results: list[CheckResult]) -> None:
    out = [{"name": r.name, "passed": r.passed, "detail": r.detail} for r in results]
    print(json.dumps(out, indent=2))


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> int:
    parser = argparse.ArgumentParser(
        description="Check docs/STARTER_VISUAL_LOCK.md criteria programmatically.",
    )
    parser.add_argument("--json",      action="store_true", help="Emit JSON")
    parser.add_argument("--exit-zero", action="store_true", help="Always exit 0")
    args = parser.parse_args()

    results = [check() for check in ALL_CHECKS]

    if args.json:
        print_json_report(results)
    else:
        print_text_report(results)

    if args.exit_zero:
        return 0
    return 0 if all(r.passed for r in results) else 1


if __name__ == "__main__":
    sys.exit(main())
