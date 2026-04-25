#!/usr/bin/env python3
"""
scripts/audit-assets.py

Visual parity audit for Erynfall 3D assets.

Checks every entry in art/models/manifest.yaml against the rules defined in
docs/VISUAL_PARITY_CHECKLIST.md, docs/GRAPHICS_STYLE.md, and docs/SCALE_SPEC.md.

Produces a ranked report:  CRITICAL → WARNING → INFO

Does NOT modify any asset or manifest file.

Usage:
    python3 scripts/audit-assets.py
    python3 scripts/audit-assets.py --key player_base
    python3 scripts/audit-assets.py --category actor
    python3 scripts/audit-assets.py --json
    python3 scripts/audit-assets.py --exit-zero   # non-blocking CI mode
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import struct
import sys
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional


# ── Paths ─────────────────────────────────────────────────────────────────────

ROOT            = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST  = ROOT / "art" / "models" / "manifest.yaml"
DEFAULT_MODELS_DIR = ROOT / "art" / "models"
DEFAULT_BLENDER_DIR = ROOT / "art" / "blender"


# ── Import shared manifest parser from validate-models.py ────────────────────

def _load_validate_models():
    src = Path(__file__).parent / "validate-models.py"
    if not src.exists():
        raise FileNotFoundError(f"Required sibling script not found: {src}")
    name = "_validate_models"
    spec = importlib.util.spec_from_file_location(name, src)
    mod  = importlib.util.module_from_spec(spec)
    # Register before exec so dataclass __module__ lookup via sys.modules succeeds.
    sys.modules[name] = mod
    spec.loader.exec_module(mod)  # type: ignore[union-attr]
    return mod

_vm = _load_validate_models()
parse_manifest         = _vm.parse_manifest
ModelEntry             = _vm.ModelEntry
CANONICAL_PLAYER_CLIPS = _vm.CANONICAL_PLAYER_CLIPS
CANONICAL_NPC_CLIPS    = _vm.CANONICAL_NPC_CLIPS
ALL_CANONICAL_CLIPS    = _vm.ALL_CANONICAL_CLIPS


# ── Finding ───────────────────────────────────────────────────────────────────

LEVELS      = ("CRITICAL", "WARNING", "INFO")
LEVEL_ORDER = {l: i for i, l in enumerate(LEVELS)}


@dataclass
class Finding:
    level:   str    # CRITICAL | WARNING | INFO
    key:     str    # manifest key
    code:    str    # short machine-readable code
    message: str    # human-readable detail

    def __lt__(self, other: "Finding") -> bool:
        return (LEVEL_ORDER[self.level], self.key, self.code) < \
               (LEVEL_ORDER[other.level], other.key, other.code)


# ── GLB inspection ────────────────────────────────────────────────────────────

@dataclass
class GlbInfo:
    """Parsed facts extracted from the JSON chunk of a .glb file."""
    path:              Path
    mesh_count:        int          = 0
    node_count:        int          = 0
    mesh_node_count:   int          = 0    # nodes that reference a mesh
    skin_count:        int          = 0    # should be 0 (rigid parenting)
    material_count:    int          = 0
    texture_count:     int          = 0
    image_count:       int          = 0
    has_vertex_colors: bool         = False
    has_pbr_metallic:  bool         = False
    has_normal_map:    bool         = False
    has_occlusion_map: bool         = False
    triangle_count:    int          = 0
    y_min:             float        = 0.0
    y_max:             float        = 0.0
    clip_names:        list[str]    = field(default_factory=list)
    clip_durations:    list[float]  = field(default_factory=list)   # seconds
    parse_error:       Optional[str] = None

    @property
    def height(self) -> float:
        return self.y_max - self.y_min


def _extract_glb_json(path: Path) -> dict:
    """Return the parsed glTF JSON from a binary .glb file."""
    data = path.read_bytes()
    if len(data) < 12:
        raise ValueError("File too short to be a GLB")
    magic, _version, total_len = struct.unpack_from("<III", data, 0)
    if magic != 0x46546C67:
        raise ValueError(f"Invalid GLB magic (0x{magic:08X}) — not a GLB file")
    offset = 12
    while offset + 8 <= len(data):
        chunk_len, chunk_type = struct.unpack_from("<II", data, offset)
        offset += 8
        chunk_data = data[offset : offset + chunk_len]
        offset += chunk_len
        if chunk_type == 0x4E4F534A:   # 0x4E4F534A = "JSON"
            return json.loads(chunk_data.decode("utf-8"))
    raise ValueError("No JSON chunk found in GLB")


def inspect_glb(path: Path) -> GlbInfo:
    """Inspect a GLB file and return a GlbInfo summary."""
    info = GlbInfo(path=path)
    try:
        g = _extract_glb_json(path)
    except Exception as exc:
        info.parse_error = str(exc)
        return info

    nodes     = g.get("nodes", [])
    meshes    = g.get("meshes", [])
    accessors = g.get("accessors", [])
    materials = g.get("materials", [])

    info.node_count     = len(nodes)
    info.mesh_count     = len(meshes)
    info.skin_count     = len(g.get("skins", []))
    info.material_count = len(materials)
    info.texture_count  = len(g.get("textures", []))
    info.image_count    = len(g.get("images", []))

    for node in nodes:
        if "mesh" in node:
            info.mesh_node_count += 1

    # Materials: PBR, normal map, occlusion
    for mat in materials:
        pbr = mat.get("pbrMetallicRoughness", {})
        if pbr.get("metallicFactor", 0.0) > 0.01:
            info.has_pbr_metallic = True
        if "normalTexture" in mat:
            info.has_normal_map = True
        if "occlusionTexture" in mat:
            info.has_occlusion_map = True

    # Meshes: vertex colors, triangle count, bounding box
    total_tris = 0
    y_mins, y_maxs = [], []

    for mesh in meshes:
        for prim in mesh.get("primitives", []):
            attrs = prim.get("attributes", {})

            if "COLOR_0" in attrs:
                info.has_vertex_colors = True

            # Triangle count
            mode = prim.get("mode", 4)   # 4 = GL_TRIANGLES
            if mode == 4:
                idx_acc = prim.get("indices")
                if idx_acc is not None and idx_acc < len(accessors):
                    total_tris += accessors[idx_acc].get("count", 0) // 3
                else:
                    pos_acc = attrs.get("POSITION")
                    if pos_acc is not None and pos_acc < len(accessors):
                        total_tris += accessors[pos_acc].get("count", 0) // 3

            # Y bounds from POSITION accessor min/max (stored in JSON)
            pos_idx = attrs.get("POSITION")
            if pos_idx is not None and pos_idx < len(accessors):
                acc = accessors[pos_idx]
                mn = acc.get("min")
                mx = acc.get("max")
                if mn and len(mn) >= 2:
                    y_mins.append(mn[1])
                if mx and len(mx) >= 2:
                    y_maxs.append(mx[1])

    info.triangle_count = total_tris
    if y_mins:
        info.y_min = min(y_mins)
    if y_maxs:
        info.y_max = max(y_maxs)

    # Animations: names and durations (max input-accessor value = clip length)
    for anim in g.get("animations", []):
        name = (anim.get("name") or "").strip()
        info.clip_names.append(name)
        duration = 0.0
        for sampler in anim.get("samplers", []):
            inp = sampler.get("input")
            if inp is not None and inp < len(accessors):
                mx = accessors[inp].get("max")
                if mx and len(mx) >= 1:
                    duration = max(duration, float(mx[0]))
        info.clip_durations.append(duration)

    return info


# ── Thresholds (from docs/VISUAL_PARITY_CHECKLIST.md) ────────────────────────

# (warn_triangles, critical_triangles) per category
POLY_BUDGET: dict[str, tuple[int, int]] = {
    "actor":     (2000, 3500),
    "equipment": (400,  650),
    "prop":      (800,  1300),
    "shell":     (2000, 3200),
    "resource":  (600,  950),
}

ACTOR_HEIGHT_TARGET  = 1.80   # WU (WorldScale.PLAYER_HEIGHT)
ACTOR_HEIGHT_TOL     = 0.08   # WU — acceptable deviation from target
ACTOR_HEIGHT_HARD_MIN = 1.45  # WU — below this is definitely wrong
ACTOR_HEIGHT_HARD_MAX = 2.10  # WU — above this is too tall

# Props/resources: bottom of model should be near Y=0 (tile ground plane)
ORIGIN_FLOAT_THRESHOLD = 0.15   # WU — y_min above this → floating
ORIGIN_SINK_THRESHOLD  = -0.15  # WU — y_min below this → sunk

TICK_DURATION = 0.600           # seconds
TICK_TOLERANCE = 0.025          # seconds — tolerance for clip duration check

VALID_ANCHOR_NAMES = frozenset({
    "head_anchor", "cape_anchor", "ammo_anchor",
    "weapon_anchor", "shield_anchor", "body_anchor",
    "legs_anchor", "hands_anchor", "feet_anchor",
})

# Equipment slots that definitely need hide_nodes (would z-fight without)
EQUIP_SLOTS_NEEDING_HIDE = {
    0,   # HEAD
    6,   # BODY
    7,   # LEGS
    8,   # HANDS
    9,   # FEET
}

# Actor key suffixes that indicate a fragmented pose-file workflow.
# These should be consolidated into a single animated _base GLB.
FRAGMENTED_SUFFIXES = (
    "_idle", "_walk", "_action", "_run",
    "_pickup", "_chop", "_mine", "_fish",
    "_smith", "_cook", "_sword", "_spear",
    "_death", "_block",
)


# ── Per-entry audit ──────────────────────────────────────────────────────────

def audit_entry(
    entry: ModelEntry,
    models_dir: Path,
    blender_dir: Path,
    all_keys: set[str],
) -> list[Finding]:
    findings: list[Finding] = []

    def add(level: str, code: str, message: str) -> None:
        findings.append(Finding(level=level, key=entry.key, code=code, message=message))

    model_path = models_dir / entry.file

    # ═══════════════════════════════════════════════════════════════════════════
    # MANIFEST-LEVEL CHECKS  (no file read needed)
    # ═══════════════════════════════════════════════════════════════════════════

    # Legacy formats ──────────────────────────────────────────────────────────
    if entry.format == "g3db":
        # This entry would have failed parse_manifest, but keep for completeness.
        add("CRITICAL", "LEGACY_G3DB",
            "Format 'g3db' is unsupported — the G3DB loader was removed. "
            "Re-export as GLB from Blender.")

    if entry.format == "g3dj":
        add("WARNING", "LEGACY_G3DJ",
            "Format 'g3dj' is legacy. Migrate to GLB next time this asset is "
            "touched. G3DJ will be removed in a future clean-up pass.")

    # source_blend missing ────────────────────────────────────────────────────
    if entry.format in {"glb", "gltf"} and not entry.source_blend:
        add("WARNING", "NO_SOURCE_BLEND",
            "No source_blend path declared. The Blender source file for this "
            "model cannot be located for future edits. "
            "Add: source_blend: <relative-path-under-art/blender/>")

    # source_blend declared but file absent ───────────────────────────────────
    if entry.source_blend:
        bp = blender_dir / entry.source_blend
        if not bp.exists():
            add("WARNING", "SOURCE_BLEND_MISSING",
                f"source_blend '{entry.source_blend}' not found on disk. "
                "Commit the .blend file to art/blender/ or correct the path.")

    # Suspicious scale ────────────────────────────────────────────────────────
    if entry.category not in {"shell"} and abs(entry.scale - 1.0) > 0.001:
        add("WARNING", "SUSPICIOUS_SCALE",
            f"scale = {entry.scale} (expected 1.0 for {entry.category}). "
            "Non-unity manifest scale distorts the asset in-game. "
            "Bake the scale into the GLB in Blender (Apply Scale) and set scale: 1.0.")

    # Equipment: anchor_name ──────────────────────────────────────────────────
    if entry.category == "equipment":
        if not entry.anchor_name:
            add("CRITICAL", "EQUIP_NO_ANCHOR",
                "Equipment has no anchor_name. The renderer places this piece at "
                "world origin (0, 0, 0) — it will appear under the map. "
                f"Valid anchors: {', '.join(sorted(VALID_ANCHOR_NAMES))}")
        elif entry.anchor_name not in VALID_ANCHOR_NAMES:
            add("WARNING", "EQUIP_UNKNOWN_ANCHOR",
                f"anchor_name '{entry.anchor_name}' is not a recognised anchor node. "
                f"Valid: {', '.join(sorted(VALID_ANCHOR_NAMES))}. "
                "Verify the node exists in player_base.glb.")

    # Equipment: hide_nodes missing on body-covering slots ───────────────────
    if entry.category == "equipment" and not entry.hide_nodes:
        if entry.equip_slot in EQUIP_SLOTS_NEEDING_HIDE:
            slot_name = {0: "HEAD", 6: "BODY", 7: "LEGS", 8: "HANDS", 9: "FEET"
                         }.get(entry.equip_slot, str(entry.equip_slot))
            add("WARNING", "EQUIP_NO_HIDE_NODES",
                f"Equipment slot {slot_name} ({entry.equip_slot}) has empty "
                "hide_nodes. Body-covering equipment must hide the underlying "
                "player_base nodes (e.g. head, hair, torso) to prevent z-fighting. "
                "Tune in EQUIPMENT_FIT mode then save with P.")

    # Equipment: missing transform fields ────────────────────────────────────
    if entry.category == "equipment":
        missing = [f for f in ("offset_x","offset_y","offset_z","rot_x","rot_y","rot_z")
                   if getattr(entry, f) is None]
        if missing:
            add("WARNING", "EQUIP_MISSING_TRANSFORMS",
                f"Missing transform fields: {', '.join(missing)}. "
                "All six offset/rotation fields are required for correct placement. "
                "Use EQUIPMENT_FIT (P to save) to populate them.")

    # Actor: animated flag ────────────────────────────────────────────────────
    if entry.category == "actor" and entry.format in {"glb", "gltf"} and not entry.animated:
        add("WARNING", "ACTOR_NOT_ANIMATED",
            "Actor GLB has animated: false (or field absent). If this model "
            "contains animation clips, set animated: true or the clips will be ignored.")

    # Actor: missing animation_clips list ────────────────────────────────────
    if entry.category == "actor" and entry.format in {"glb", "gltf"} and not entry.animation_clips:
        add("WARNING", "ACTOR_NO_CLIP_LIST",
            "No animation_clips list declared in manifest. "
            "Add the list so clip names can be validated. "
            "Player: idle walk run pickup chop mine fish smith cook "
            "attack_slash attack_stab attack_shoot attack_throw block death. "
            "NPC: idle walk action.")

    # Actor: non-canonical clips declared ────────────────────────────────────
    if entry.animation_clips:
        bad = [c for c in entry.animation_clips if c not in ALL_CANONICAL_CLIPS]
        if bad:
            add("CRITICAL", "ACTOR_BAD_CLIP_NAMES",
                f"Non-canonical animation_clips declared: {', '.join(bad)}. "
                "Rename NLA tracks in Blender to match exactly. "
                f"Player clips: {', '.join(sorted(CANONICAL_PLAYER_CLIPS))}. "
                f"NPC clips: {', '.join(sorted(CANONICAL_NPC_CLIPS))}.")

    # Fragmented actor files (separate pose GLBs) ────────────────────────────
    if entry.category == "actor":
        for suffix in FRAGMENTED_SUFFIXES:
            if entry.key.endswith(suffix):
                base_key = entry.key[: -len(suffix)] + "_base"
                if base_key in all_keys:
                    add("INFO", "FRAGMENTED_ACTOR",
                        f"Separate pose file. '{base_key}' exists — "
                        "consolidate all clips as NLA tracks in the _base GLB "
                        "then remove this pose file and its manifest entry.")
                else:
                    add("INFO", "FRAGMENTED_ACTOR_NO_BASE",
                        f"Separate pose file. No '{base_key}' consolidated "
                        "animated GLB exists yet. Author a single _base GLB "
                        "with all required NLA clips to replace these pose fragments.")
                break

    # ═══════════════════════════════════════════════════════════════════════════
    # FILE-LEVEL CHECKS  (requires GLB on disk)
    # ═══════════════════════════════════════════════════════════════════════════

    if not model_path.exists():
        # validate-models.py already reports missing files; skip GLB inspection.
        return findings

    if entry.format not in {"glb", "gltf"}:
        return findings   # GLB-only inspection

    info = inspect_glb(model_path)

    if info.parse_error:
        add("CRITICAL", "GLB_PARSE_FAIL",
            f"GLB parse failed: {info.parse_error}. "
            "The file may be corrupt or truncated.")
        return findings

    # Skinning ────────────────────────────────────────────────────────────────
    if info.skin_count > 0:
        add("CRITICAL", "SKINNING_PRESENT",
            f"GLB contains {info.skin_count} skin(s). "
            "OSRS-style animation requires rigid bone-parented mesh objects, "
            "NOT vertex-weight skinning. "
            "In Blender: remove the Armature modifier, parent each body part mesh "
            "to its bone with Ctrl+P → Bone (not Bone Relative). "
            "Re-export with Skinning: OFF in glTF settings.")

    # PBR metallic ────────────────────────────────────────────────────────────
    if info.has_pbr_metallic:
        add("CRITICAL", "PBR_METALLIC",
            "One or more materials use metallicFactor > 0. "
            "LibGDX has no PBR shader — metallic shading renders as flat or broken. "
            "Set Metallic = 0 on all Blender materials.")

    # Normal map ──────────────────────────────────────────────────────────────
    if info.has_normal_map:
        add("CRITICAL", "NORMAL_MAP",
            "Material references a normalTexture. "
            "No normal map support in the renderer — the map is silently ignored "
            "but indicates wrong material setup. Remove normalTexture.")

    # Occlusion map ───────────────────────────────────────────────────────────
    if info.has_occlusion_map:
        add("WARNING", "OCCLUSION_MAP",
            "Material uses an occlusionTexture. "
            "Not supported — flat/matte shading only. Remove occlusionTexture.")

    # Textures present (prefer vertex colors) ─────────────────────────────────
    if info.texture_count > 0 or info.image_count > 0:
        add("WARNING", "TEXTURE_PRESENT",
            f"{info.texture_count} texture(s) / {info.image_count} embedded image(s). "
            "Prefer vertex colors (COLOR_0) over texture maps. "
            "If textures are required: max 256×256 px, pixel-art style, no gradients. "
            "Verify dimensions in Blender → Image Properties.")

    # No vertex colors and no textures ────────────────────────────────────────
    if not info.has_vertex_colors and info.texture_count == 0 and info.mesh_count > 0:
        add("WARNING", "NO_SURFACE_COLOR",
            "No COLOR_0 vertex colors and no textures — model renders as flat grey. "
            "Add vertex colors in Blender: Edit Mode → Mesh → Vertex Colors → Add, "
            "then paint with Face Map or Vertex Paint mode.")

    # ── Actor-specific file checks ────────────────────────────────────────────
    if entry.category == "actor":

        # Height check
        if info.mesh_count > 0:
            h = info.height
            if h < ACTOR_HEIGHT_HARD_MIN or h > ACTOR_HEIGHT_HARD_MAX:
                add("CRITICAL", "ACTOR_HEIGHT_WRONG",
                    f"Actor height = {h:.3f} WU "
                    f"(y_min={info.y_min:.3f}, y_max={info.y_max:.3f}). "
                    f"Expected {ACTOR_HEIGHT_TARGET} WU "
                    f"(hard range {ACTOR_HEIGHT_HARD_MIN}–{ACTOR_HEIGHT_HARD_MAX} WU). "
                    "Normalise height in Blender: scale mesh to 1.80 WU tall, "
                    "apply scale, move origin to bottom-center.")
            elif abs(h - ACTOR_HEIGHT_TARGET) > ACTOR_HEIGHT_TOL:
                add("WARNING", "ACTOR_HEIGHT_OFF_TARGET",
                    f"Actor height = {h:.3f} WU "
                    f"(target {ACTOR_HEIGHT_TARGET} ± {ACTOR_HEIGHT_TOL} WU). "
                    "Minor deviation — verify against player_base reference "
                    "in MODEL_PREVIEW (F7 overlay).")

        # Origin check: bottom of actor should be at or near Y=0
        if info.mesh_count > 0 and info.y_min < -0.20:
            add("WARNING", "ACTOR_ORIGIN_CENTERED",
                f"Actor y_min = {info.y_min:.3f} WU — origin appears to be at model "
                "centre rather than ground level. "
                "Move origin to bottom-centre in Blender so the character stands on "
                "the tile ground plane (Y=0). "
                "Current height measurement may be unreliable.")

        # Single mesh node (should be rigid multi-part)
        if info.mesh_node_count == 1:
            add("WARNING", "ACTOR_SINGLE_MESH",
                f"Actor has only 1 mesh node. "
                "OSRS-style characters use separate mesh objects per body segment "
                "(head, torso, upper/lower arms, upper/lower legs, feet), each "
                "parented rigidly to its bone. "
                "A single merged mesh breaks the rigid body-part animation look.")

        # animated: true but GLB has no clips
        if entry.animated and len(info.clip_names) == 0:
            add("CRITICAL", "ACTOR_NO_CLIPS_IN_GLB",
                "Manifest sets animated: true but GLB contains no animation data. "
                "In Blender: author NLA tracks with the required clip names, "
                "then export with Animation → Export NLA Strips: ON.")

        # Non-canonical clip names actually in the GLB
        if info.clip_names:
            bad_in_glb = [c for c in info.clip_names if c and c not in ALL_CANONICAL_CLIPS]
            if bad_in_glb:
                add("CRITICAL", "GLB_NON_CANONICAL_CLIPS",
                    f"GLB contains non-canonical animation names: "
                    f"{', '.join(repr(c) for c in bad_in_glb)}. "
                    "These clips will not be played by the runtime. "
                    "Rename NLA tracks in Blender to match canonical names exactly "
                    f"(case-sensitive). Valid: {', '.join(sorted(ALL_CANONICAL_CLIPS))}.")

        # Clip duration not a tick multiple
        for clip_name, dur in zip(info.clip_names, info.clip_durations):
            if dur <= 0 or clip_name not in ALL_CANONICAL_CLIPS:
                continue
            remainder = dur % TICK_DURATION
            off_by = min(remainder, TICK_DURATION - remainder)
            if off_by > TICK_TOLERANCE:
                nearest = round(dur / TICK_DURATION) * TICK_DURATION
                add("WARNING", "CLIP_NON_TICK_DURATION",
                    f"Clip '{clip_name}': duration {dur:.4f}s is not a multiple "
                    f"of {TICK_DURATION}s (nearest tick: {nearest:.1f}s, "
                    f"off by {off_by:.4f}s). "
                    "Adjust the NLA track length in Blender so it snaps to a "
                    "whole tick boundary.")

    # ── Prop / Resource / Shell file checks ───────────────────────────────────
    if entry.category in {"prop", "resource", "shell"} and info.mesh_count > 0:

        if info.y_min > ORIGIN_FLOAT_THRESHOLD:
            add("WARNING", "PROP_FLOATING",
                f"y_min = {info.y_min:.3f} WU — bottom of model is above the ground "
                "plane. In Blender: move the mesh down and set origin to "
                "bottom-centre so it sits flush on the tile.")

        if info.y_min < ORIGIN_SINK_THRESHOLD:
            add("WARNING", "PROP_SUNK",
                f"y_min = {info.y_min:.3f} WU — bottom of model extends below the "
                "ground plane. Geometry is buried underground. "
                "Move origin to bottom-centre in Blender.")

    # ── Poly count ────────────────────────────────────────────────────────────
    if entry.category in POLY_BUDGET:
        warn_limit, crit_limit = POLY_BUDGET[entry.category]
        if info.triangle_count == 0 and info.mesh_count > 0:
            add("WARNING", "POLY_COUNT_ZERO",
                f"Triangle count = 0 despite {info.mesh_count} mesh(es). "
                "Non-indexed geometry or non-TRIANGLES primitive mode. "
                "Apply all modifiers in Blender and re-export.")
        elif info.triangle_count > crit_limit:
            add("CRITICAL", "POLY_OVER_BUDGET",
                f"Triangle count = {info.triangle_count:,} — exceeds critical limit "
                f"({crit_limit:,}) for category '{entry.category}'. "
                "Decimate or re-model to reduce poly count before this ships.")
        elif info.triangle_count > warn_limit:
            add("WARNING", "POLY_HIGH",
                f"Triangle count = {info.triangle_count:,} — above recommended "
                f"limit ({warn_limit:,}) for category '{entry.category}'. "
                "Consider decimation.")

    return findings


# ── Inventory summary (INFO) ──────────────────────────────────────────────────

def build_inventory_lines(entries: list[ModelEntry], models_dir: Path) -> list[str]:
    cat_fmt: Counter = Counter()
    missing_count = 0
    for e in entries:
        cat_fmt[(e.category, e.format)] += 1
        if not (models_dir / e.file).exists():
            missing_count += 1

    lines = [f"Manifest entries: {len(entries)}"]
    for (cat, fmt), count in sorted(cat_fmt.items()):
        lines.append(f"  {cat:<12} {fmt:<6} × {count}")
    if missing_count:
        lines.append(f"  {missing_count} entries reference files not found on disk")
    return lines


# ── Report output ─────────────────────────────────────────────────────────────

_LEVEL_BARS = {"CRITICAL": "██", "WARNING": "▓▓", "INFO": "░░"}


def print_text_report(findings: list[Finding], entries: list[ModelEntry],
                      models_dir: Path) -> None:
    findings = sorted(findings)
    by_level: dict[str, list[Finding]] = {l: [] for l in LEVELS}
    for f in findings:
        by_level[f.level].append(f)

    counts = {l: len(by_level[l]) for l in LEVELS}
    total  = sum(counts.values())

    # ── Header ────────────────────────────────────────────────────────────────
    width = 72
    print("═" * width)
    print("ERYNFALL  VISUAL PARITY AUDIT")
    print("═" * width)
    for line in build_inventory_lines(entries, models_dir):
        print(line)
    print()
    print(f"Results:  {counts['CRITICAL']} CRITICAL  "
          f"{counts['WARNING']} WARNING  "
          f"{counts['INFO']} INFO  "
          f"({total} total)")
    print()

    # ── Per-level sections ────────────────────────────────────────────────────
    for level in LEVELS:
        level_findings = by_level[level]
        if not level_findings:
            continue

        bar = _LEVEL_BARS[level]
        print(f"{bar} {level}  ({len(level_findings)})  {bar}")
        print("─" * width)

        current_key = None
        for f in level_findings:
            if f.key != current_key:
                current_key = f.key
                print(f"\n  ▸ {f.key}")
            # Word-wrap message at ~66 chars, indented 8 spaces
            msg_line = f"    [{f.code}]  {f.message}"
            _print_wrapped(msg_line, width, continuation_indent=12)

        print()

    # ── Footer ────────────────────────────────────────────────────────────────
    print("─" * width)
    if counts["CRITICAL"] == 0 and counts["WARNING"] == 0:
        print("✓  No issues found — all audited assets pass visual parity checks.")
    elif counts["CRITICAL"] == 0:
        print(f"  {counts['WARNING']} warning(s) — review before shipping assets.")
    else:
        print(f"  {counts['CRITICAL']} CRITICAL issue(s) must be resolved before "
              "these assets can ship.")
    print("═" * width)


def _print_wrapped(line: str, width: int, continuation_indent: int) -> None:
    """Print a line, hard-wrapping at width with an indent for continuation."""
    if len(line) <= width:
        print(line)
        return
    # Find a good break point
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
        {"level": f.level, "key": f.key, "code": f.code, "message": f.message}
        for f in sorted(findings)
    ]
    print(json.dumps(out, indent=2))


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> int:
    parser = argparse.ArgumentParser(
        description="Visual parity audit for Erynfall 3D assets.",
        epilog=(
            "Checks each manifest entry against docs/VISUAL_PARITY_CHECKLIST.md. "
            "Does not modify any file. "
            "Exit code: 1 if CRITICAL issues found, 0 otherwise (use --exit-zero to suppress)."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--manifest",    default=str(DEFAULT_MANIFEST),
                        help="Path to manifest.yaml (default: art/models/manifest.yaml)")
    parser.add_argument("--models-dir",  default=str(DEFAULT_MODELS_DIR),
                        help="Path to model files directory (default: art/models/)")
    parser.add_argument("--blender-dir", default=str(DEFAULT_BLENDER_DIR),
                        help="Path to Blender source directory (default: art/blender/)")
    parser.add_argument("--key",         metavar="KEY",
                        help="Audit only the entry with this manifest key")
    parser.add_argument("--category",    metavar="CAT",
                        choices=["actor","equipment","prop","shell","resource"],
                        help="Audit only entries in this category")
    parser.add_argument("--level",       metavar="LEVEL",
                        choices=LEVELS,
                        help="Show only findings at this level or above")
    parser.add_argument("--json",        action="store_true",
                        help="Emit machine-readable JSON instead of human report")
    parser.add_argument("--exit-zero",   action="store_true",
                        help="Always exit 0 (use for informational CI runs)")
    args = parser.parse_args()

    manifest_path = Path(args.manifest).resolve()
    models_dir    = Path(args.models_dir).resolve()
    blender_dir   = Path(args.blender_dir).resolve()

    if not models_dir.exists():
        print(f"ERROR: models directory does not exist: {models_dir}", file=sys.stderr)
        return 2

    try:
        entries = parse_manifest(manifest_path)
    except (ValueError, FileNotFoundError) as exc:
        print(f"ERROR: cannot parse manifest: {exc}", file=sys.stderr)
        return 2

    # Apply filters
    if args.key:
        entries = [e for e in entries if e.key == args.key]
        if not entries:
            print(f"ERROR: no manifest entry with key '{args.key}'", file=sys.stderr)
            return 2
    if args.category:
        entries = [e for e in entries if e.category == args.category]
        if not entries:
            print(f"No entries found for category '{args.category}'.")
            return 0

    all_keys = {e.key for e in entries}

    findings: list[Finding] = []
    for entry in entries:
        findings.extend(audit_entry(entry, models_dir, blender_dir, all_keys))

    # Level filter for output
    if args.level:
        min_order = LEVEL_ORDER[args.level]
        findings = [f for f in findings if LEVEL_ORDER[f.level] <= min_order]

    if args.json:
        print_json_report(findings)
    else:
        print_text_report(findings, entries, models_dir)

    if args.exit_zero:
        return 0
    return 1 if any(f.level == "CRITICAL" for f in findings) else 0


if __name__ == "__main__":
    sys.exit(main())
