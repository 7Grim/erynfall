#!/usr/bin/env python3
"""
scripts/report-entity-visuals.py

Reports every entity in entity_visuals.yaml that lacks a 3D model or
falls back to billboard / colored-rectangle rendering.

Categories of interest:
  BILLBOARD   — model_key_3d absent; falls back to sprite billboard (expected for some)
  MISSING_GLB — model_key_3d declared but GLB file not on disk
  NOT_ANIMATED — actor-class entity with animated_3d: false (no idle/walk clips)
  NO_SPRITE   — no model_key_3d and no sprite_key_2d (renders as colored rect)

Usage:
    python3 scripts/report-entity-visuals.py
    python3 scripts/report-entity-visuals.py --json
    python3 scripts/report-entity-visuals.py --exit-zero
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT              = Path(__file__).resolve().parents[1]
ENTITY_VISUALS    = ROOT / "art" / "world" / "entity_visuals.yaml"
MODELS_DIR        = ROOT / "art" / "models"


# ── Import pipeline YAML loader ───────────────────────────────────────────────

def _load_pipeline_common():
    src = Path(__file__).parent / "blender" / "_pipeline_common.py"
    if not src.exists():
        raise FileNotFoundError(f"Required sibling script not found: {src}")
    spec = importlib.util.spec_from_file_location("_pipeline_common", src)
    mod  = importlib.util.module_from_spec(spec)
    sys.modules["_pipeline_common"] = mod
    spec.loader.exec_module(mod)  # type: ignore[union-attr]
    return mod

_pc = _load_pipeline_common()
load_yaml = _pc.load_yaml


# ── Import manifest parser ────────────────────────────────────────────────────

def _load_validate_models():
    src = Path(__file__).parent / "validate-models.py"
    if not src.exists():
        raise FileNotFoundError(f"Required sibling script not found: {src}")
    spec = importlib.util.spec_from_file_location("_validate_models", src)
    mod  = importlib.util.module_from_spec(spec)
    sys.modules["_validate_models"] = mod
    spec.loader.exec_module(mod)  # type: ignore[union-attr]
    return mod

_vm = _load_validate_models()
parse_manifest = _vm.parse_manifest


# ── Data ──────────────────────────────────────────────────────────────────────

# Actor definition_id ranges (these should always have 3D models + animations).
# Based on entity_visuals.yaml conventions: IDs < 100 are NPCs/actors.
NPC_ACTOR_IDS_MAX = 99

# Known-intentional billboards (no 3D model planned — noted here to suppress noise).
KNOWN_BILLBOARDS: dict[int, str] = {
    400: "Cooking Fire — particle/world-object; no GLB planned",
}


@dataclass
class EntityReport:
    definition_id: int
    name:          str
    model_key_3d:  str | None
    sprite_key_2d: str | None
    animated_3d:   bool
    status:        str   # BILLBOARD | MISSING_GLB | NOT_ANIMATED | NO_SPRITE | OK
    note:          str


# ── Logic ─────────────────────────────────────────────────────────────────────

def _is_actor(entry: dict) -> bool:
    """Heuristic: definition_id < 100 and animated_3d is expected true."""
    did = entry.get("definition_id", 9999)
    return did <= NPC_ACTOR_IDS_MAX


def report_entities(
    entity_visuals_path: Path,
    models_dir: Path,
) -> list[EntityReport]:
    data = load_yaml(entity_visuals_path)
    entries = data.get("entity_visuals", [])

    manifest_path = ROOT / "art" / "models" / "manifest.yaml"
    try:
        manifest_entries = parse_manifest(manifest_path)
        manifest_keys = {e.key for e in manifest_entries}
    except Exception:
        manifest_keys = set()

    reports: list[EntityReport] = []

    for entry in entries:
        did       = entry.get("definition_id", -1)
        name      = entry.get("name", f"id:{did}")
        key3d     = entry.get("model_key_3d") or None
        sprite    = entry.get("sprite_key_2d") or None
        animated  = bool(entry.get("animated_3d", False))

        status = "OK"
        note   = ""

        if key3d is None:
            if sprite is None:
                status = "NO_SPRITE"
                note   = "No model_key_3d and no sprite_key_2d — renders as colored rectangle"
            else:
                status = "BILLBOARD"
                if did in KNOWN_BILLBOARDS:
                    note = f"Known intentional: {KNOWN_BILLBOARDS[did]}"
                else:
                    note = "Sprite-only fallback. Add model_key_3d when GLB is ready."
        else:
            # GLB presence check (check models dir + manifest)
            glb_path = models_dir / f"{key3d}.glb"
            in_manifest = key3d in manifest_keys
            if not glb_path.exists():
                if not in_manifest:
                    status = "MISSING_GLB"
                    note   = f"model_key_3d '{key3d}' not in manifest and GLB not on disk"
                else:
                    status = "MISSING_GLB"
                    note   = f"model_key_3d '{key3d}' in manifest but GLB file absent"

            if status == "OK" and _is_actor(entry) and not animated:
                status = "NOT_ANIMATED"
                note   = "Actor-class entity with animated_3d: false — no walk/idle clips"

        reports.append(EntityReport(
            definition_id=did,
            name=name,
            model_key_3d=key3d,
            sprite_key_2d=sprite,
            animated_3d=animated,
            status=status,
            note=note,
        ))

    return reports


# ── Output ─────────────────────────────────────────────────────────────────────

_STATUS_LEVEL = {
    "NO_SPRITE":    0,  # most severe — totally invisible
    "MISSING_GLB":  1,
    "NOT_ANIMATED": 2,
    "BILLBOARD":    3,
    "OK":           4,
}

STATUS_LABELS = {
    "NO_SPRITE":    "NO_SPRITE    (colored rect)",
    "MISSING_GLB":  "MISSING_GLB  (broken 3D ref)",
    "NOT_ANIMATED": "NOT_ANIMATED (static actor)",
    "BILLBOARD":    "BILLBOARD    (sprite only)",
    "OK":           "OK",
}


def print_text_report(reports: list[EntityReport]) -> None:
    issues = [r for r in reports if r.status != "OK"]
    issues.sort(key=lambda r: (_STATUS_LEVEL[r.status], r.definition_id))

    total   = len(reports)
    n_ok    = sum(1 for r in reports if r.status == "OK")
    n_issue = len(issues)

    width = 72
    print("═" * width)
    print("ERYNFALL  ENTITY VISUAL FALLBACK REPORT")
    print("═" * width)
    print(f"Total entities: {total}  |  OK: {n_ok}  |  Issues: {n_issue}")
    print()

    if not issues:
        print("✓  All entities have valid 3D models.")
        print("═" * width)
        return

    # Group by status
    from collections import defaultdict
    by_status: dict[str, list[EntityReport]] = defaultdict(list)
    for r in issues:
        by_status[r.status].append(r)

    for status in ["NO_SPRITE", "MISSING_GLB", "NOT_ANIMATED", "BILLBOARD"]:
        group = by_status.get(status, [])
        if not group:
            continue
        label = STATUS_LABELS[status]
        print(f"── {label}  ({len(group)}) ──")
        for r in group:
            key_str = r.model_key_3d or "(none)"
            spr_str = r.sprite_key_2d or "(none)"
            print(f"  [{r.definition_id:>4}]  {r.name:<28}  3d={key_str:<24}  2d={spr_str}")
            if r.note:
                print(f"          {r.note}")
        print()

    print("─" * width)
    print(f"  {n_issue} entity/entities need attention before visual quality freeze.")
    print("─" * width)
    known_bb = sum(1 for r in issues if r.status == "BILLBOARD" and r.definition_id in KNOWN_BILLBOARDS)
    if known_bb:
        print(f"  ({known_bb} billboard(s) are known-intentional — see KNOWN_BILLBOARDS in script)")
    print("═" * width)


def print_json_report(reports: list[EntityReport]) -> None:
    out = []
    for r in reports:
        out.append({
            "definition_id": r.definition_id,
            "name":          r.name,
            "model_key_3d":  r.model_key_3d,
            "sprite_key_2d": r.sprite_key_2d,
            "animated_3d":   r.animated_3d,
            "status":        r.status,
            "note":          r.note,
        })
    out.sort(key=lambda x: (_STATUS_LEVEL[x["status"]], x["definition_id"]))
    print(json.dumps(out, indent=2))


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> int:
    parser = argparse.ArgumentParser(
        description="Report Erynfall entities relying on billboard or missing 3D fallback.",
    )
    parser.add_argument("--entity-visuals", default=str(ENTITY_VISUALS),
                        help="Path to entity_visuals.yaml")
    parser.add_argument("--models-dir",     default=str(MODELS_DIR),
                        help="Path to art/models/ directory")
    parser.add_argument("--json",           action="store_true",
                        help="Emit machine-readable JSON")
    parser.add_argument("--exit-zero",      action="store_true",
                        help="Always exit 0 (informational CI mode)")
    args = parser.parse_args()

    reports = report_entities(
        Path(args.entity_visuals),
        Path(args.models_dir),
    )

    if args.json:
        print_json_report(reports)
    else:
        print_text_report(reports)

    if args.exit_zero:
        return 0
    severe = {"NO_SPRITE", "MISSING_GLB"}
    return 1 if any(r.status in severe for r in reports) else 0


if __name__ == "__main__":
    sys.exit(main())
