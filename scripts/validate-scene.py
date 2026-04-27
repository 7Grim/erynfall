#!/usr/bin/env python3
"""
scripts/validate-scene.py

Validates scene.yaml files against:
  1. Terrain palette vocabulary (docs/TERRAIN_PALETTE_SPEC.md)
  2. Building shell placement rules (docs/BUILDING_CLASS_SPEC.md)

Produces a ranked report: CRITICAL → WARNING → INFO

Does NOT modify any file.

Usage:
    python3 scripts/validate-scene.py
    python3 scripts/validate-scene.py --world-id sandbox
    python3 scripts/validate-scene.py --scene path/to/scene.yaml
    python3 scripts/validate-scene.py --json
    python3 scripts/validate-scene.py --exit-zero
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any


# ── Paths ─────────────────────────────────────────────────────────────────────

ROOT       = Path(__file__).resolve().parents[1]
WORLDS_DIR = ROOT / "art" / "worlds"


# ── Import shared YAML loader from _pipeline_common.py ───────────────────────

def _load_pipeline_common():
    src = Path(__file__).parent / "blender" / "_pipeline_common.py"
    if not src.exists():
        raise FileNotFoundError(f"Required helper not found: {src}")
    name = "_pipeline_common"
    spec = importlib.util.spec_from_file_location(name, src)
    mod  = importlib.util.module_from_spec(spec)
    sys.modules[name] = mod
    spec.loader.exec_module(mod)  # type: ignore[union-attr]
    return mod

_pc = _load_pipeline_common()
load_yaml = _pc.load_yaml


# ── Finding ───────────────────────────────────────────────────────────────────

LEVELS      = ("CRITICAL", "WARNING", "INFO")
LEVEL_ORDER = {l: i for i, l in enumerate(LEVELS)}


@dataclass
class Finding:
    level:   str
    context: str    # e.g. "terrain_visual/regions[2]" or "static_props[5]"
    code:    str
    message: str

    def __lt__(self, other: "Finding") -> bool:
        return (LEVEL_ORDER[self.level], self.context, self.code) < \
               (LEVEL_ORDER[other.level], other.context, other.code)


# ── Terrain constants ─────────────────────────────────────────────────────────

# All accepted type strings for terrain regions / tile_overrides.
VALID_TERRAIN_TYPES: frozenset[str] = frozenset({
    "0", "grass",
    "1", "water",
    "2", "path", "dirt",
    "3", "wall", "rock",
    "4", "sand",
})

# Numeric type for display in messages.
_TYPE_LABELS: dict[str, str] = {
    "0": "grass", "grass": "grass",
    "1": "water", "water": "water",
    "2": "path",  "path": "path", "dirt": "path",
    "3": "wall",  "wall": "wall", "rock": "wall",
    "4": "sand",  "sand": "sand",
}

# Acceptable tint component range — outside this the palette drifts too far.
TINT_MIN = 0.30
TINT_MAX = 1.50


# ── Building constants ────────────────────────────────────────────────────────

# Model-space dimensions (half_width, half_depth, wall_height) before scale.
# Source: docs/BUILDING_CLASS_SPEC.md and scripts/gen_osrs_buildings.py
SHELL_MODEL_DIMS: dict[str, tuple[float, float, float]] = {
    "building_shell_small":   (2.10, 1.90, 1.50),
    "building_shell_service": (2.50, 2.10, 1.55),
    "building_shell_coastal": (2.10, 1.90, 1.25),
}

# Class scale bands: (min_inclusive, max_inclusive) in placement scale units.
# A shell placement should fall within at least one band.
BUILDING_CLASS_BANDS: list[tuple[str, float, float]] = [
    ("tiny_hut",        1.40, 1.65),
    ("small_service",   1.65, 1.95),
    ("forge_workshop",  1.80, 2.25),
    ("medium_hub",      2.00, 2.55),
]

# Hard limits outside which findings are raised.
SHELL_SCALE_MIN      = 1.40   # below this walls risk not clearing the player
SHELL_SCALE_MAX      = 2.50   # above this building dwarfs the OSRS zone feel
SHELL_SCALE_CRITICAL = 1.20   # below this is always an error regardless of shell
PLAYER_HEIGHT_WU     = 1.80   # WorldScale.PLAYER_HEIGHT

# Shell key suffixes that form base/roof pairs.
_SHELL_BASE_SUFFIXES = ("_base",)
_SHELL_ROOF_SUFFIXES = ("_roof",)


def _shell_variant(key: str) -> str | None:
    """Return the base model name from a shell key (strips _base/_roof suffix)."""
    for sfx in ("_base", "_roof"):
        if key.endswith(sfx):
            stripped = key[: -len(sfx)]
            if stripped in SHELL_MODEL_DIMS:
                return stripped
    if key in SHELL_MODEL_DIMS:
        return key
    return None


def _is_shell_key(key: str) -> bool:
    return (key in SHELL_MODEL_DIMS
            or any(key.endswith(s) for s in ("_base", "_roof"))
            and _shell_variant(key) is not None)


def _class_for_scale(scale: float) -> str | None:
    """Return the name of the matching class band or None if outside all bands."""
    for name, lo, hi in BUILDING_CLASS_BANDS:
        if lo <= scale <= hi:
            return name
    return None


# ── Terrain validation ────────────────────────────────────────────────────────

def validate_terrain(scene: dict[str, Any], findings: list[Finding]) -> None:
    terrain = scene.get("terrain_visual")
    if not terrain or not isinstance(terrain, dict):
        return  # No terrain_visual block — not an error, just nothing to validate.

    themes_block: dict[str, Any] = scene.get("themes") or {}

    def add(level: str, ctx: str, code: str, msg: str) -> None:
        findings.append(Finding(level=level, context=ctx, code=code, message=msg))

    # default_type ──────────────────────────────────────────────────────────
    default_raw = terrain.get("default_type")
    if default_raw is None:
        add("WARNING", "terrain_visual",
            "TERRAIN_MISSING_DEFAULT",
            "No default_type declared. Every terrain_visual block should set "
            "default_type so the background tile type is unambiguous. "
            "Add: default_type: water  (or grass, path, etc.)")
    elif str(default_raw).strip().lower() not in VALID_TERRAIN_TYPES:
        add("CRITICAL", "terrain_visual.default_type",
            "TERRAIN_INVALID_TYPE",
            f"default_type '{default_raw}' is not a recognised terrain type. "
            f"Valid: grass, water, path (or dirt), wall (or rock), sand.")

    # Regions ───────────────────────────────────────────────────────────────
    regions = terrain.get("regions") or []
    if not isinstance(regions, list):
        regions = []

    referenced_themes: set[str] = set()
    for idx, region in enumerate(regions):
        if not isinstance(region, dict):
            continue
        ctx = f"terrain_visual/regions[{idx}]"
        name_hint = region.get("name", f"region[{idx}]")
        ctx_display = f"terrain_visual/regions '{name_hint}'"

        # type value
        type_raw = region.get("type")
        if "polygon" in region:
            # Polygon region — type still required.
            pass
        if type_raw is not None:
            type_str = str(type_raw).strip().lower()
            if type_str not in VALID_TERRAIN_TYPES:
                add("CRITICAL", ctx_display,
                    "TERRAIN_INVALID_TYPE",
                    f"type '{type_raw}' is not a recognised terrain type. "
                    f"Valid: grass, water, path (or dirt), wall (or rock), sand.")

        # theme reference
        theme_ref = region.get("theme")
        if theme_ref is not None:
            referenced_themes.add(str(theme_ref))
            if themes_block and str(theme_ref) not in themes_block:
                add("WARNING", ctx_display,
                    "TERRAIN_UNKNOWN_THEME_REF",
                    f"theme '{theme_ref}' not declared in the themes: block. "
                    "Add it or remove the theme reference. Undeclared themes are "
                    "silently ignored by the renderer — tint will not be applied.")

    # tile_overrides ────────────────────────────────────────────────────────
    overrides = terrain.get("tile_overrides") or []
    if not isinstance(overrides, list):
        overrides = []
    for idx, ovr in enumerate(overrides):
        if not isinstance(ovr, dict):
            continue
        type_raw = ovr.get("type")
        if type_raw is not None:
            type_str = str(type_raw).strip().lower()
            if type_str not in VALID_TERRAIN_TYPES:
                x = ovr.get("x", "?")
                y = ovr.get("y", "?")
                add("CRITICAL", f"terrain_visual/tile_overrides[{idx}] at ({x},{y})",
                    "TERRAIN_INVALID_TYPE",
                    f"type '{type_raw}' is not a recognised terrain type. "
                    f"Valid: grass, water, path (or dirt), wall (or rock), sand.")

    # Themes block validation ───────────────────────────────────────────────
    if not isinstance(themes_block, dict):
        themes_block = {}

    # Warn if regions reference themes but no themes block exists.
    if referenced_themes and not themes_block:
        add("WARNING", "themes",
            "TERRAIN_UNKNOWN_THEME_REF",
            f"Regions reference theme(s) {sorted(referenced_themes)} but no "
            "themes: block is declared. Add the themes: block with terrain_tint "
            "for each referenced theme ID.")

    for theme_id, theme_def in themes_block.items():
        if not isinstance(theme_def, dict):
            continue
        ctx = f"themes/{theme_id}"

        # terrain_tint present
        tint = theme_def.get("terrain_tint")
        if tint is None:
            add("WARNING", ctx,
                "TERRAIN_THEME_MISSING_TINT",
                f"Theme '{theme_id}' has no terrain_tint. "
                "Without terrain_tint the theme applies no colour shift — "
                "this is legal but usually a mistake. "
                "Add: terrain_tint: [1.0, 1.0, 1.0]  (or desired multipliers).")
        elif isinstance(tint, list) and len(tint) == 3:
            for i, component in enumerate(tint):
                try:
                    v = float(component)
                except (TypeError, ValueError):
                    v = None
                if v is not None and (v < TINT_MIN or v > TINT_MAX):
                    channel = ["R", "G", "B"][i]
                    add("WARNING", ctx,
                        "TERRAIN_THEME_EXTREME_TINT",
                        f"Theme '{theme_id}' terrain_tint[{channel}] = {v:.3f} is "
                        f"outside acceptable range [{TINT_MIN}, {TINT_MAX}]. "
                        "Extreme tint shifts colour too far from the base vocabulary. "
                        "Keep each component within [0.30, 1.50].")


# ── Building validation ───────────────────────────────────────────────────────

def validate_buildings(scene: dict[str, Any], findings: list[Finding]) -> None:
    props: list[Any] = scene.get("static_props") or []
    if not isinstance(props, list):
        return

    def add(level: str, ctx: str, code: str, msg: str) -> None:
        findings.append(Finding(level=level, context=ctx, code=code, message=msg))

    # Index shell placements by position signature for pairing check.
    # Key: (x, y, scale, rot) → {visibility_group: prop_index}
    shell_positions: dict[tuple, dict[str, int]] = {}

    for idx, prop in enumerate(props):
        if not isinstance(prop, dict):
            continue
        key = prop.get("key", "")
        if not isinstance(key, str):
            continue

        variant = _shell_variant(key)
        if variant is None:
            continue  # Not a building shell — skip.

        ctx = f"static_props[{idx}] key='{key}'"

        try:
            scale = float(prop.get("scale", 1.0))
        except (TypeError, ValueError):
            scale = 1.0

        x   = prop.get("x", "?")
        y   = prop.get("y", "?")
        rot = prop.get("rotation_y_degrees", 0.0)
        vis = prop.get("visibility_group", "")

        # Collect for pairing check.
        pos_key = (x, y, scale, rot)
        if pos_key not in shell_positions:
            shell_positions[pos_key] = {}
        shell_positions[pos_key][str(vis)] = idx

        # Scale: critical below hard floor, warning above soft ceiling.
        if scale < SHELL_SCALE_CRITICAL:
            add("CRITICAL", ctx,
                "SHELL_SCALE_BELOW_MIN",
                f"scale={scale:.2f} is critically low (minimum {SHELL_SCALE_CRITICAL}). "
                "Building shells at this scale will be toy-sized — walls will "
                "likely not clear the player. Raise scale to ≥ 1.40.")
        elif scale < SHELL_SCALE_MIN:
            add("CRITICAL", ctx,
                "SHELL_SCALE_BELOW_MIN",
                f"scale={scale:.2f} is below minimum {SHELL_SCALE_MIN:.2f}. "
                "Walls may not clear player height (1.80 WU). "
                "Raise scale to ≥ 1.40 (coastal shells: ≥ 1.50).")

        if scale > SHELL_SCALE_MAX:
            add("WARNING", ctx,
                "SHELL_SCALE_ABOVE_MAX",
                f"scale={scale:.2f} exceeds recommended maximum {SHELL_SCALE_MAX:.2f}. "
                "Buildings larger than this dominate an OSRS-sized screen zone. "
                "Consider splitting into multiple smaller buildings.")

        # Scale outside all class bands.
        if _class_for_scale(scale) is None:
            bands_str = ", ".join(f"{lo}–{hi} ({n})" for n, lo, hi in BUILDING_CLASS_BANDS)
            add("WARNING", ctx,
                "SHELL_UNKNOWN_CLASS_SCALE",
                f"scale={scale:.2f} does not fall within any canonical class band. "
                f"Defined bands: {bands_str}. "
                "See docs/BUILDING_CLASS_SPEC.md — pick the scale closest to your "
                "intended class, or add a notes: field explaining the deviation.")

        # Wall height check (only for _base entries; _roof is not a walkable volume).
        if key.endswith("_base"):
            hw, hd, wall_h = SHELL_MODEL_DIMS[variant]
            actual_wall = wall_h * scale
            if actual_wall < PLAYER_HEIGHT_WU:
                add("CRITICAL", ctx,
                    "SHELL_WALL_TOO_SHORT",
                    f"Wall height at scale {scale:.2f} = {actual_wall:.3f} WU — "
                    f"shorter than the player ({PLAYER_HEIGHT_WU:.2f} WU). "
                    "Player character will visually clip through the roof. "
                    "Increase scale or use a shell variant with taller walls.")
            elif actual_wall < PLAYER_HEIGHT_WU * 1.05:
                add("WARNING", ctx,
                    "SHELL_WALL_TOO_SHORT",
                    f"Wall height at scale {scale:.2f} = {actual_wall:.3f} WU — "
                    f"only {actual_wall / PLAYER_HEIGHT_WU:.2f}× player height. "
                    "Walls should be at least 1.25× player height to feel OSRS-scaled. "
                    "Consider raising scale or using a shell with taller walls.")

    # Pairing check: base without roof, roof without base.
    for pos_key, groups in shell_positions.items():
        has_base = "base" in groups
        has_roof = "roof" in groups
        x, y, scale, rot = pos_key
        ctx = f"static_props at ({x},{y}) scale={scale}"
        if has_base and not has_roof:
            add("WARNING", ctx,
                "SHELL_BASE_WITHOUT_ROOF",
                f"Building shell at ({x},{y}) has a 'base' entry but no matching "
                "'roof' at the same position/scale/rotation. "
                "The interior visibility system requires a _roof entry to hide "
                "when the player enters the building. "
                "Add the matching *_roof entry to scene.yaml.")
        elif has_roof and not has_base:
            add("WARNING", ctx,
                "SHELL_ROOF_WITHOUT_BASE",
                f"Building shell at ({x},{y}) has a 'roof' entry but no matching "
                "'base' at the same position/scale/rotation. "
                "The roof has nothing to sit on. "
                "Add the matching *_base entry to scene.yaml.")


# ── Report ────────────────────────────────────────────────────────────────────

_LEVEL_BARS = {"CRITICAL": "██", "WARNING": "▓▓", "INFO": "░░"}


def print_text_report(findings: list[Finding], scene_path: Path) -> None:
    findings = sorted(findings)
    by_level: dict[str, list[Finding]] = {l: [] for l in LEVELS}
    for f in findings:
        by_level[f.level].append(f)

    counts = {l: len(by_level[l]) for l in LEVELS}
    total  = sum(counts.values())
    width  = 72

    print("═" * width)
    print("ERYNFALL  SCENE VALIDATION")
    print("═" * width)
    print(f"Scene: {scene_path}")
    print(f"Results:  {counts['CRITICAL']} CRITICAL  "
          f"{counts['WARNING']} WARNING  "
          f"{counts['INFO']} INFO  ({total} total)")
    print()

    for level in LEVELS:
        level_findings = by_level[level]
        if not level_findings:
            continue
        bar = _LEVEL_BARS[level]
        print(f"{bar} {level}  ({len(level_findings)})  {bar}")
        print("─" * width)
        for f in level_findings:
            print(f"\n  ▸ [{f.code}]  {f.context}")
            _print_wrapped("    " + f.message, width, 8)
        print()

    print("─" * width)
    if counts["CRITICAL"] == 0 and counts["WARNING"] == 0:
        print("✓  Scene passes all terrain and building validation checks.")
    elif counts["CRITICAL"] == 0:
        print(f"  {counts['WARNING']} warning(s) — review before shipping world changes.")
    else:
        print(f"  {counts['CRITICAL']} CRITICAL issue(s) must be resolved.")
    print("═" * width)


def _print_wrapped(line: str, width: int, continuation_indent: int) -> None:
    if len(line) <= width:
        print(line)
        return
    while len(line) > width:
        break_at = line.rfind(" ", 0, width)
        if break_at == -1:
            break_at = width
        print(line[:break_at])
        line = " " * continuation_indent + line[break_at:].lstrip()
    if line.strip():
        print(line)


def print_json_report(findings: list[Finding]) -> None:
    out = [
        {"level": f.level, "context": f.context, "code": f.code, "message": f.message}
        for f in sorted(findings)
    ]
    print(json.dumps(out, indent=2))


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> int:
    parser = argparse.ArgumentParser(
        description="Validate scene.yaml terrain types and building shell placements.",
        epilog=(
            "Checks terrain type vocabulary and building class scale rules. "
            "Does not modify any file. "
            "Exit code: 1 if CRITICAL issues found, 0 otherwise (--exit-zero to suppress)."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument(
        "--scene",
        metavar="PATH",
        help="Explicit path to scene.yaml (overrides --world-id)",
    )
    parser.add_argument(
        "--world-id",
        default="main_world",
        metavar="ID",
        help="World ID to validate (default: main_world). "
             "Resolves to art/worlds/{id}/scene.yaml",
    )
    parser.add_argument("--json",      action="store_true",
                        help="Emit machine-readable JSON instead of human report")
    parser.add_argument("--exit-zero", action="store_true",
                        help="Always exit 0 (informational CI mode)")
    parser.add_argument("--level",     metavar="LEVEL",
                        choices=LEVELS,
                        help="Show only findings at this level or above")
    args = parser.parse_args()

    if args.scene:
        scene_path = Path(args.scene).resolve()
    else:
        scene_path = WORLDS_DIR / args.world_id / "scene.yaml"

    if not scene_path.exists():
        print(f"ERROR: scene file not found: {scene_path}", file=sys.stderr)
        return 2

    try:
        scene = load_yaml(scene_path)
    except Exception as exc:
        print(f"ERROR: cannot parse scene.yaml: {exc}", file=sys.stderr)
        return 2

    if not isinstance(scene, dict):
        print("ERROR: scene.yaml top level is not a mapping.", file=sys.stderr)
        return 2

    findings: list[Finding] = []
    validate_terrain(scene, findings)
    validate_buildings(scene, findings)

    if args.level:
        min_order = LEVEL_ORDER[args.level]
        findings = [f for f in findings if LEVEL_ORDER[f.level] <= min_order]

    if args.json:
        print_json_report(findings)
    else:
        print_text_report(findings, scene_path)

    if args.exit_zero:
        return 0
    return 1 if any(f.level == "CRITICAL" for f in findings) else 0


if __name__ == "__main__":
    sys.exit(main())
