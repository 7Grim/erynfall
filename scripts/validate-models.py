#!/usr/bin/env python3
"""Validate art/models entries against art/models/manifest.yaml."""

from __future__ import annotations

import argparse
from collections.abc import Iterable
import json
import re
import struct
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = ROOT / "art" / "models" / "manifest.yaml"
DEFAULT_MODELS_DIR = ROOT / "art" / "models"
DEFAULT_BLENDER_DIR = ROOT / "art" / "blender"


@dataclass(frozen=True)
class ModelEntry:
    key: str
    file: str
    category: str
    format: str
    scale: float
    origin: str
    required: bool
    animated: bool
    notes: str
    equip_slot: int | None
    item_id: int | None
    attach_to_state: str | None
    anchor_name: str | None
    offset_x: float | None
    offset_y: float | None
    offset_z: float | None
    rot_x: float | None
    rot_y: float | None
    rot_z: float | None
    hide_nodes: tuple[str, ...]
    source_blend: str | None
    animation_clips: tuple[str, ...]   # optional declared clip names; validated against canonical set
    material_zones: tuple[str, ...]    # optional declared zone names; validated against GLB materials


# Canonical clip names from docs/GRAPHICS_STYLE.md — Animation Clip Standard.
# These are the names GLB NLA tracks must use exactly.
CANONICAL_PLAYER_CLIPS: frozenset[str] = frozenset({
    "idle", "walk", "run",
    "pickup", "chop", "mine", "fish", "smith", "cook",
    "attack_slash", "attack_stab", "attack_shoot", "attack_throw",
    "block", "death",
})

CANONICAL_NPC_CLIPS: frozenset[str] = frozenset({
    "idle", "walk", "action",
})

# All known canonical names across all actor types (used for open-ended warning).
ALL_CANONICAL_CLIPS: frozenset[str] = CANONICAL_PLAYER_CLIPS | CANONICAL_NPC_CLIPS

# Expected durations in seconds from docs/GRAPHICS_STYLE.md — Animation Clip Standard.
# Every value is an exact OSRS tick multiple (1 tick = 0.600 s).
# These constants are the machine-readable authority; the GRAPHICS_STYLE table is the narrative.
CANONICAL_CLIP_DURATIONS: dict[str, float] = {
    "idle":         2.4,   # 4 ticks — player and NPC
    "walk":         1.2,   # 2 ticks — player and NPC
    "run":          0.6,   # 1 tick
    "pickup":       0.6,   # 1 tick
    "chop":         1.2,   # 2 ticks
    "mine":         2.4,   # 4 ticks
    "fish":         2.4,   # 4 ticks
    "smith":        1.8,   # 3 ticks
    "cook":         1.8,   # 3 ticks
    "attack_slash": 1.8,   # 3 ticks
    "attack_stab":  1.8,   # 3 ticks
    "attack_shoot": 1.8,   # 3 ticks
    "attack_throw": 1.8,   # 3 ticks
    "block":        0.6,   # 1 tick
    "death":        1.8,   # 3 ticks
    "action":       1.8,   # 3 ticks — NPC generic action
}

# Required clip sets: ALL entries must be present for the respective actor type.
# Absence of a required clip causes silent runtime fallback to idle or no animation.
REQUIRED_PLAYER_CLIPS: frozenset[str] = CANONICAL_PLAYER_CLIPS   # all 15
REQUIRED_NPC_CLIPS:    frozenset[str] = CANONICAL_NPC_CLIPS       # idle, walk, action


SLOT_NAME_TO_ID = {
    "HEAD": 0,
    "CAPE": 1,
    "NECK": 2,
    "AMMO": 3,
    "WEAPON": 4,
    "SHIELD": 5,
    "BODY": 6,
    "LEGS": 7,
    "HANDS": 8,
    "FEET": 9,
    "RING": 10,
}


def parse_scalar(raw: str):
    value = raw.strip()
    if value == "":
        return ""
    if value.startswith('"') and value.endswith('"') and len(value) >= 2:
        return value[1:-1]
    if value.startswith("'") and value.endswith("'") and len(value) >= 2:
        return value[1:-1]
    if value == "true":
        return True
    if value == "false":
        return False
    if re.fullmatch(r"-?\d+\.\d+", value):
        return float(value)
    if re.fullmatch(r"-?\d+", value):
        return int(value)
    return value


def as_bool(value: object, field_name: str, key: str) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, str):
        lower = value.strip().lower()
        if lower == "true":
            return True
        if lower == "false":
            return False
    raise ValueError(f"Field '{field_name}' for key '{key}' must be true/false")


def as_float(value: object, field_name: str, key: str) -> float:
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, str) and re.fullmatch(r"-?\d+(\.\d+)?", value.strip()):
        return float(value)
    raise ValueError(f"Field '{field_name}' for key '{key}' must be numeric")


def as_int(value: object, field_name: str, key: str) -> int:
    if isinstance(value, int):
        return value
    if isinstance(value, float) and value.is_integer():
        return int(value)
    if isinstance(value, str) and re.fullmatch(r"-?\d+", value.strip()):
        return int(value)
    raise ValueError(f"Field '{field_name}' for key '{key}' must be integer")


def as_optional_float(value: object, field_name: str, key: str) -> float | None:
    if value is None:
        return None
    if isinstance(value, str) and value.strip() == "":
        return None
    return as_float(value, field_name, key)


def as_optional_int(value: object, field_name: str, key: str) -> int | None:
    if value is None:
        return None
    if isinstance(value, str) and value.strip() == "":
        return None
    return as_int(value, field_name, key)


def parse_equip_slot(value: object, key: str) -> int | None:
    if value is None:
        return None
    if isinstance(value, str):
        stripped = value.strip()
        if stripped == "":
            return None
        upper = stripped.upper()
        if upper in SLOT_NAME_TO_ID:
            return SLOT_NAME_TO_ID[upper]
        if re.fullmatch(r"-?\d+", stripped):
            slot = int(stripped)
            if 0 <= slot <= 10:
                return slot
        raise ValueError(f"Field 'equip_slot' for key '{key}' must be one of {', '.join(SLOT_NAME_TO_ID.keys())} or an integer 0-10")
    slot = as_int(value, "equip_slot", key)
    if 0 <= slot <= 10:
        return slot
    raise ValueError(f"Field 'equip_slot' for key '{key}' must be in range 0..10")


def _glb_material_names(path: Path) -> list[str] | None:
    """Return list of material names from a GLB file, or None if unreadable/not GLB."""
    try:
        data = path.read_bytes()
        if len(data) < 12:
            return None
        magic, _version, _total = struct.unpack_from("<III", data, 0)
        if magic != 0x46546C67:
            return None
        offset = 12
        while offset + 8 <= len(data):
            chunk_len, chunk_type = struct.unpack_from("<II", data, offset)
            offset += 8
            chunk_data = data[offset : offset + chunk_len]
            offset += chunk_len
            if chunk_type == 0x4E4F534A:  # JSON chunk
                doc = json.loads(chunk_data.decode("utf-8"))
                materials = doc.get("materials")
                if not isinstance(materials, list):
                    return []
                return [
                    m.get("name", "")
                    for m in materials
                    if isinstance(m, dict)
                ]
        return []
    except Exception:
        return None


def parse_manifest(manifest_path: Path) -> list[ModelEntry]:
    if not manifest_path.exists():
        raise ValueError(f"Manifest does not exist: {manifest_path}")

    lines = manifest_path.read_text(encoding="utf-8").splitlines()
    in_assets = False
    items: list[dict[str, object]] = []
    current: dict[str, object] | None = None
    active_list_field: str | None = None

    for index, raw_line in enumerate(lines, start=1):
        line = raw_line.split("#", 1)[0].rstrip()
        if not line.strip():
            continue

        stripped = line.strip()
        if stripped == "assets:":
            in_assets = True
            continue

        if not in_assets:
            continue

        entry_start = re.match(r"^\s*-\s+key:\s*(.+)$", line)
        if entry_start:
            active_list_field = None
            if current is not None:
                items.append(current)
            current = {"key": parse_scalar(entry_start.group(1))}
            continue

        list_item = re.match(r"^\s{6}-\s*(.+)$", line)
        if list_item and active_list_field is not None:
            if current is None:
                raise ValueError(f"Line {index}: found list item before list field: {raw_line}")
            values = current.get(active_list_field)
            if not isinstance(values, list):
                raise ValueError(f"Line {index}: list field '{active_list_field}' is not a list")
            values.append(parse_scalar(list_item.group(1)))
            continue

        active_list_field = None

        pair = re.match(r"^\s{4}([a-z_]+):\s*(.*)$", line)
        if pair:
            if current is None:
                raise ValueError(f"Line {index}: found field before list item: {raw_line}")
            field_name = pair.group(1)
            field_value = pair.group(2)
            if field_value == "":
                current[field_name] = []
                active_list_field = field_name
            else:
                current[field_name] = parse_scalar(field_value)
            continue

        raise ValueError(f"Line {index}: unsupported manifest syntax: {raw_line}")

    if current is not None:
        items.append(current)

    if not items:
        raise ValueError("No entries found under assets:")

    required_fields = {"key", "file", "category", "format", "scale", "origin", "required", "notes"}
    entries: list[ModelEntry] = []
    seen_keys: set[str] = set()
    seen_files: set[str] = set()
    seen_keys_lower: set[str] = set()
    seen_files_lower: set[str] = set()

    for item in items:
        missing = sorted(required_fields - set(item.keys()))
        if missing:
            raise ValueError(f"Entry for key '{item.get('key')}' is missing fields: {', '.join(missing)}")

        key = str(item["key"])
        file_name = str(item["file"])
        fmt = str(item["format"]).lower()
        if key in seen_keys:
            raise ValueError(f"Duplicate key in manifest: {key}")
        if file_name in seen_files:
            raise ValueError(f"Duplicate file in manifest: {file_name}")
        key_lower = key.lower()
        file_lower = file_name.lower()
        if key_lower in seen_keys_lower:
            raise ValueError(f"Duplicate key differs only by case: {key}")
        if file_lower in seen_files_lower:
            raise ValueError(f"Duplicate file differs only by case: {file_name}")

        if fmt not in {"g3dj", "glb", "gltf"}:
            # g3db removed: loader was deleted (see docs/FORMAT_MIGRATION.md)
            raise ValueError(f"Unsupported format for '{key}': '{fmt}' — allowed: g3dj (legacy), glb, gltf")
        if not file_name.lower().endswith(f".{fmt}"):
            raise ValueError(f"File extension does not match format for '{key}': {file_name} vs {fmt}")

        hide_nodes_raw = item.get("hide_nodes")
        hide_nodes_list = hide_nodes_raw if isinstance(hide_nodes_raw, list) else []

        animated_raw = item.get("animated")
        animated = as_bool(animated_raw, "animated", key) if animated_raw is not None else False

        animation_clips_raw = item.get("animation_clips")
        if animation_clips_raw is not None and not isinstance(animation_clips_raw, list):
            raise ValueError(f"Field 'animation_clips' for key '{key}' must be a list of strings")
        animation_clips_list: list[str] = [
            str(c).strip() for c in (animation_clips_raw or []) if str(c).strip()
        ]

        material_zones_raw = item.get("material_zones")
        if material_zones_raw is not None and not isinstance(material_zones_raw, list):
            raise ValueError(f"Field 'material_zones' for key '{key}' must be a list of strings")
        material_zones_list: list[str] = [
            str(z).strip() for z in (material_zones_raw or []) if str(z).strip()
        ]

        entry = ModelEntry(
            key=key,
            file=file_name,
            category=str(item["category"]),
            format=fmt,
            scale=as_float(item["scale"], "scale", key),
            origin=str(item["origin"]),
            required=as_bool(item["required"], "required", key),
            animated=animated,
            notes=str(item["notes"]),
            equip_slot=parse_equip_slot(item.get("equip_slot"), key),
            item_id=as_optional_int(item.get("item_id"), "item_id", key),
            attach_to_state=str(item["attach_to_state"]).strip() if item.get("attach_to_state") not in (None, "") else None,
            anchor_name=str(item["anchor_name"]).strip() if item.get("anchor_name") not in (None, "") else None,
            offset_x=as_optional_float(item.get("offset_x"), "offset_x", key),
            offset_y=as_optional_float(item.get("offset_y"), "offset_y", key),
            offset_z=as_optional_float(item.get("offset_z"), "offset_z", key),
            rot_x=as_optional_float(item.get("rot_x"), "rot_x", key),
            rot_y=as_optional_float(item.get("rot_y"), "rot_y", key),
            rot_z=as_optional_float(item.get("rot_z"), "rot_z", key),
            hide_nodes=tuple(
                str(node).strip()
                for node in hide_nodes_list
                if str(node).strip()
            ),
            source_blend=str(item["source_blend"]).strip() if item.get("source_blend") not in (None, "") else None,
            animation_clips=tuple(animation_clips_list),
            material_zones=tuple(material_zones_list),
        )
        if entry.scale <= 0:
            raise ValueError(f"Scale must be > 0 for key '{key}'")
        if entry.category == "equipment":
            if entry.equip_slot is None:
                raise ValueError(f"Equipment model '{key}' must define equip_slot")
            if entry.item_id is None or entry.item_id <= 0:
                raise ValueError(f"Equipment model '{key}' must define item_id > 0")
            if hide_nodes_raw is not None and not isinstance(hide_nodes_raw, list):
                raise ValueError(f"Equipment model '{key}' field 'hide_nodes' must be a list when present")
        elif item.get("hide_nodes") is not None:
            raise ValueError(f"Only equipment models may define 'hide_nodes' (key '{key}')")

        seen_keys.add(key)
        seen_files.add(file_name)
        seen_keys_lower.add(key_lower)
        seen_files_lower.add(file_lower)
        entries.append(entry)

    return entries


def inspect_g3dj(model_path: Path) -> tuple[set[str], set[str]]:
    data = json.loads(model_path.read_text(encoding="utf-8"))
    node_ids: set[str] = set()
    clip_ids: set[str] = set()

    def walk_nodes(nodes: object) -> None:
        if not isinstance(nodes, list):
            return
        for node in nodes:
            if not isinstance(node, dict):
                continue
            node_id = node.get("id")
            if isinstance(node_id, str) and node_id.strip():
                node_ids.add(node_id.strip())
            walk_nodes(node.get("children"))

    walk_nodes(data.get("nodes"))
    animations = data.get("animations")
    if isinstance(animations, list):
        for animation in animations:
            if not isinstance(animation, dict):
                continue
            clip_id = animation.get("id")
            if isinstance(clip_id, str) and clip_id.strip():
                clip_ids.add(clip_id.strip())

    return node_ids, clip_ids


def validate(entries: list[ModelEntry], models_dir: Path, blender_dir: Path) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []

    manifest_files = {entry.file for entry in entries}
    by_key = {entry.key: entry for entry in entries}
    g3dj_cache: dict[Path, tuple[set[str], set[str]]] = {}
    supported_categories = {"prop", "resource", "shell", "actor", "equipment"}

    # ── Equipment (slot, item_id) uniqueness ─────────────────────────────────
    seen_slot_item: dict[tuple[int, int], str] = {}
    for entry in entries:
        if entry.category == "equipment" and entry.equip_slot is not None and entry.item_id is not None:
            combo = (entry.equip_slot, entry.item_id)
            if combo in seen_slot_item:
                errors.append(
                    f"duplicate (equip_slot={entry.equip_slot}, item_id={entry.item_id}): "
                    f"keys '{seen_slot_item[combo]}' and '{entry.key}' — second will silently overwrite first at runtime"
                )
            else:
                seen_slot_item[combo] = entry.key

    # ── Shell base/roof pairing ───────────────────────────────────────────────
    # Collect all keys whose file ends in _base.<ext> or _roof.<ext>
    shell_bases: set[str] = set()
    shell_roofs: set[str] = set()
    for entry in entries:
        if entry.category != "shell":
            continue
        stem = Path(entry.file).stem   # e.g. building_shell_small_base
        if stem.endswith("_base"):
            shell_bases.add(stem[:-5])   # strip "_base"
        elif stem.endswith("_roof"):
            shell_roofs.add(stem[:-5])   # strip "_roof"
    for prefix in sorted(shell_bases - shell_roofs):
        errors.append(
            f"shell '{prefix}_base' has no matching '{prefix}_roof' entry — "
            "roof GLB must be declared separately for interior visibility to work"
        )
    for prefix in sorted(shell_roofs - shell_bases):
        errors.append(
            f"shell '{prefix}_roof' has no matching '{prefix}_base' entry"
        )

    # ── Per-entry checks ─────────────────────────────────────────────────────
    for entry in entries:
        model_path = models_dir / entry.file
        if not model_path.exists():
            msg = (
                f"missing model file for key '{entry.key}': {entry.file} — "
                f"run the appropriate generator script or export from Blender"
            )
            if entry.required:
                errors.append(msg)
            else:
                warnings.append(msg)
            continue

        if entry.category not in supported_categories:
            errors.append(
                f"unknown category '{entry.category}' for key '{entry.key}' — "
                f"allowed: {', '.join(sorted(supported_categories))}"
            )

        if entry.format in {"g3dj"}:
            warnings.append(
                f"DEPRECATED: entry '{entry.key}' uses legacy format '{entry.format}' — migrate to GLB"
            )

        # animated: true on non-actor is almost certainly wrong
        if entry.animated and entry.category not in {"actor"}:
            errors.append(
                f"key '{entry.key}' (category={entry.category}) sets animated: true — "
                "only actor models carry animation clips; remove this flag"
            )

        if entry.category == "equipment" and not entry.anchor_name:
            errors.append(
                f"equipment key '{entry.key}' is missing anchor_name — "
                "renderer uses this name to find the attachment node in player_base"
            )

        if entry.category == "equipment" and entry.origin != "actor-center":
            warnings.append(
                f"equipment key '{entry.key}' uses origin '{entry.origin}' (expected actor-center for attachment workflows)"
            )

        if entry.category == "actor" and entry.format in {"glb", "gltf"}:
            if not entry.animated:
                warnings.append(
                    f"GLB actor '{entry.key}' should set animated: true if it contains animation clips"
                )
            if not entry.animation_clips:
                warnings.append(
                    f"GLB actor '{entry.key}' has no animation_clips declared — "
                    "add 'animation_clips:' list to manifest so clip names can be validated. "
                    f"Expected player clips: {', '.join(sorted(CANONICAL_PLAYER_CLIPS))}"
                )
            else:
                non_canonical = sorted(c for c in entry.animation_clips if c not in ALL_CANONICAL_CLIPS)
                if non_canonical:
                    errors.append(
                        f"actor '{entry.key}' declares non-canonical animation_clips: {', '.join(non_canonical)} — "
                        f"valid player clips: {', '.join(sorted(CANONICAL_PLAYER_CLIPS))}; "
                        f"valid NPC clips: {', '.join(sorted(CANONICAL_NPC_CLIPS))}. "
                        "Rename NLA tracks in Blender to match exactly."
                    )

        if entry.format in {"glb", "gltf"}:
            if not entry.source_blend:
                warnings.append(
                    f"GLB/GLTF entry '{entry.key}' is missing source_blend (expected path under art/blender/)"
                )

        if entry.source_blend:
            source_rel = Path(entry.source_blend)
            if source_rel.is_absolute() or ".." in source_rel.parts:
                errors.append(
                    f"source_blend for key '{entry.key}' must be a relative path under art/blender/: {entry.source_blend}"
                )
            else:
                if source_rel.suffix.lower() != ".blend":
                    warnings.append(
                        f"source_blend for key '{entry.key}' should end with .blend: {entry.source_blend}"
                    )
                source_path = blender_dir / source_rel
                if not source_path.exists():
                    warnings.append(
                        f"source_blend file missing for key '{entry.key}': {entry.source_blend} — "
                        "commit the .blend source file to art/blender/ or update the path"
                    )

    # ── Equipment anchor cross-check against player_base ─────────────────────
    referenced_anchors = {
        entry.anchor_name.strip()
        for entry in entries
        if entry.category == "equipment" and entry.anchor_name
    }
    if referenced_anchors:
        player_base = by_key.get("player_base")
        if player_base is None:
            errors.append("equipment anchor validation requires 'player_base' manifest entry")
        else:
            player_base_path = models_dir / player_base.file
            if player_base.format in {"g3dj"} and player_base_path.exists():
                try:
                    if player_base_path not in g3dj_cache:
                        g3dj_cache[player_base_path] = inspect_g3dj(player_base_path)
                    nodes, _ = g3dj_cache[player_base_path]
                    missing_anchors = sorted(anchor for anchor in referenced_anchors if anchor not in nodes)
                    if missing_anchors:
                        errors.append(
                            "player_base is missing anchor nodes referenced by equipment entries: "
                            + ", ".join(missing_anchors)
                            + " — add these bones/empties to the player_base armature"
                        )
                except Exception as exc:
                    warnings.append(f"could not inspect player_base anchors: {exc}")

    # ── Material zone validation ──────────────────────────────────────────────
    for entry in entries:
        if not entry.material_zones:
            continue
        model_path = models_dir / entry.file
        if not model_path.exists():
            continue  # missing-file error already emitted above
        if entry.format not in {"glb", "gltf"}:
            warnings.append(
                f"material_zones declared for '{entry.key}' but format is '{entry.format}' — "
                "zone validation only supported for GLB/GLTF"
            )
            continue
        glb_materials = _glb_material_names(model_path)
        if glb_materials is None:
            warnings.append(
                f"could not read GLB material names for '{entry.key}': {entry.file}"
            )
            continue
        glb_mat_set = set(glb_materials)
        missing_zones = [z for z in entry.material_zones if z not in glb_mat_set]
        if missing_zones:
            errors.append(
                f"actor '{entry.key}' declares material_zones {list(entry.material_zones)} "
                f"but GLB is missing materials: {missing_zones} — "
                "add named materials per zone to the Blender source and re-export"
            )

    # ── Orphaned files not declared in manifest ───────────────────────────────
    known_extensions = {".g3dj", ".g3db", ".glb", ".gltf"}
    for model_path in sorted(models_dir.iterdir()):
        if model_path.is_dir():
            continue
        if model_path.name == "manifest.yaml":
            continue
        if model_path.suffix.lower() not in known_extensions:
            continue
        if model_path.name not in manifest_files:
            errors.append(
                f"model file present but not declared in manifest: {model_path.name} — "
                "add a manifest entry or delete the file"
            )

    return errors, warnings


def write_runtime_key_list(entries: Iterable[ModelEntry], output_path: Path) -> None:
    keys = sorted(entry.key for entry in entries)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    content = "# Auto-generated by scripts/validate-models.py\n" + "\n".join(keys) + "\n"
    output_path.write_text(content, encoding="utf-8")


def write_runtime_metadata(entries: Iterable[ModelEntry], output_path: Path) -> None:
    assets = []
    for entry in sorted(entries, key=lambda e: e.key):
        asset = {
            "key": entry.key,
            "file": entry.file,
            "category": entry.category,
            "format": entry.format,
            "scale": entry.scale,
            "origin": entry.origin,
            "required": entry.required,
            "animated": entry.animated,
        }
        if entry.equip_slot is not None:
            asset["equip_slot"] = entry.equip_slot
        if entry.item_id is not None:
            asset["item_id"] = entry.item_id
        if entry.attach_to_state:
            asset["attach_to_state"] = entry.attach_to_state
        if entry.anchor_name:
            asset["anchor_name"] = entry.anchor_name
        if entry.offset_x is not None:
            asset["offset_x"] = entry.offset_x
        if entry.offset_y is not None:
            asset["offset_y"] = entry.offset_y
        if entry.offset_z is not None:
            asset["offset_z"] = entry.offset_z
        if entry.rot_x is not None:
            asset["rot_x"] = entry.rot_x
        if entry.rot_y is not None:
            asset["rot_y"] = entry.rot_y
        if entry.rot_z is not None:
            asset["rot_z"] = entry.rot_z
        if entry.hide_nodes:
            asset["hide_nodes"] = list(entry.hide_nodes)
        if entry.animation_clips:
            asset["animation_clips"] = list(entry.animation_clips)
        if entry.material_zones:
            asset["material_zones"] = list(entry.material_zones)
        assets.append(asset)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps({"assets": assets}, sort_keys=True, separators=(",", ":")), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate static model assets against manifest.yaml")
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST), help="Path to model manifest YAML")
    parser.add_argument("--models-dir", default=str(DEFAULT_MODELS_DIR), help="Path to model asset directory")
    parser.add_argument("--blender-dir", default=str(DEFAULT_BLENDER_DIR), help="Path to Blender source directory")
    parser.add_argument("--write-key-list", help="Optional output path for runtime model key list")
    parser.add_argument("--write-runtime-meta", help="Optional output path for runtime model metadata JSON")
    args = parser.parse_args()

    manifest_path = Path(args.manifest).resolve()
    models_dir = Path(args.models_dir).resolve()
    blender_dir = Path(args.blender_dir).resolve()

    if not models_dir.exists():
        print(f"ERROR: model directory does not exist: {models_dir}")
        return 2

    try:
        entries = parse_manifest(manifest_path)
        errors, warnings = validate(entries, models_dir, blender_dir)
    except ValueError as exc:
        print(f"ERROR: {exc}")
        return 2

    for warning in warnings:
        print(f"WARN: {warning}")

    if errors:
        print("MODEL VALIDATION FAILED")
        for error in errors:
            print(f" - {error}")
        return 1

    if args.write_key_list:
        write_runtime_key_list(entries, Path(args.write_key_list).resolve())
    if args.write_runtime_meta:
        write_runtime_metadata(entries, Path(args.write_runtime_meta).resolve())

    print("MODEL VALIDATION OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
