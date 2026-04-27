"""Shared helpers for Blender world-authoring scripts.

Import-only module. Not a Blender script on its own. Each real script
(`generate_world_blend.py`, `export_world_glb.py`, `bake_world_lighting.py`)
adds this module's directory to sys.path and imports from it.

Kept out of the `scripts/` top level so the non-Blender Python scripts
already there don't accidentally pick it up.
"""

from __future__ import annotations

import argparse
import dataclasses
import re
import sys
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Tuple


# --------------------------------------------------------------------------
# Path resolution
# --------------------------------------------------------------------------

def repo_root_from_script(script_file: str) -> Path:
    """Return the repo root given a script file that lives under scripts/blender/."""
    return Path(script_file).resolve().parents[2]


def world_dir(repo_root: Path, world_id: str) -> Path:
    return repo_root / "art" / "worlds" / world_id


def world_blend_path(repo_root: Path, world_id: str) -> Path:
    return world_dir(repo_root, world_id) / "world.blend"


def world_glb_path(repo_root: Path, world_id: str) -> Path:
    return world_dir(repo_root, world_id) / "world.glb"


def scene_yaml_path(repo_root: Path, world_id: str) -> Path:
    return world_dir(repo_root, world_id) / "scene.yaml"


def manifest_path(repo_root: Path) -> Path:
    return repo_root / "art" / "models" / "manifest.yaml"


# --------------------------------------------------------------------------
# Argument parsing
# --------------------------------------------------------------------------

def blender_argv_after_double_dash() -> List[str]:
    """Return argv after the `--` separator that Blender uses to pass through.

    When `--` is absent (direct `python3 script.py` invocation), return the normal
    argv tail so dry-run / planning paths work without Blender.
    """
    if "--" in sys.argv:
        idx = sys.argv.index("--")
        return sys.argv[idx + 1:]
    return sys.argv[1:]


def parse_world_arg(extra: Optional[Iterable[argparse.Action]] = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Blender world script")
    parser.add_argument("--world", default="main_world", help="world id (default: main_world)")
    parser.add_argument("--dry-run", action="store_true", help="print plan without writing files")
    if extra:
        for action in extra:
            parser._add_action(action)
    return parser.parse_args(blender_argv_after_double_dash())


# --------------------------------------------------------------------------
# Minimal YAML reader (so these scripts don't require PyYAML inside Blender's
# bundled Python). Good enough for our scene.yaml / manifest.yaml shape.
# --------------------------------------------------------------------------

_NUMBER_RE = re.compile(r"^-?\d+(?:\.\d+)?$")


def _coerce_scalar(raw: str) -> Any:
    s = raw.strip()
    if not s:
        return ""
    if (s.startswith('"') and s.endswith('"')) or (s.startswith("'") and s.endswith("'")):
        return s[1:-1]
    if s in ("true", "True"):
        return True
    if s in ("false", "False"):
        return False
    if s in ("null", "~", "None"):
        return None
    if _NUMBER_RE.match(s):
        return float(s) if "." in s else int(s)
    return s


def _split_inline_mapping(body: str) -> Dict[str, Any]:
    """Parse a simple `{ key: value, key: value }` inline mapping."""
    inner = body.strip()
    if inner.startswith("{") and inner.endswith("}"):
        inner = inner[1:-1]
    out: Dict[str, Any] = {}
    for chunk in _split_commas_respecting_braces(inner):
        if ":" not in chunk:
            continue
        k, v = chunk.split(":", 1)
        out[k.strip()] = _coerce_scalar(v)
    return out


def _split_commas_respecting_braces(s: str) -> List[str]:
    parts: List[str] = []
    depth = 0
    buf: List[str] = []
    for ch in s:
        if ch in "{[":
            depth += 1
        elif ch in "}]":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(buf))
            buf = []
            continue
        buf.append(ch)
    if buf:
        parts.append("".join(buf))
    return [p.strip() for p in parts if p.strip()]


def load_yaml(path: Path) -> Any:
    """Load a subset of YAML sufficient for scene.yaml and manifest.yaml.

    Supports:
    - block mappings (nested)
    - block lists of mappings with `- key: value` syntax
    - inline `{ k: v, k: v }` mappings for single-line entries
    - scalar types: int, float, bool, null, string (with/without quotes)
    - `#` comments
    - blank lines

    Does NOT support: anchors, aliases, multi-line strings, block scalars.
    """
    lines = path.read_text().splitlines()
    return _parse_block(lines, 0, 0)[0]


def _leading_spaces(s: str) -> int:
    return len(s) - len(s.lstrip(" "))


def _strip_comment(s: str) -> str:
    # Strip trailing # comment only when not inside quotes.
    in_quote = None
    for i, ch in enumerate(s):
        if ch in ('"', "'"):
            if in_quote is None:
                in_quote = ch
            elif in_quote == ch:
                in_quote = None
        elif ch == "#" and in_quote is None:
            return s[:i].rstrip()
    return s


def _parse_block(lines: List[str], start: int, indent: int) -> Tuple[Any, int]:
    """Return (parsed_value, next_line_index)."""
    # Skip leading blanks/comments
    i = start
    while i < len(lines):
        raw = lines[i]
        stripped = _strip_comment(raw).rstrip()
        if not stripped.strip():
            i += 1
            continue
        break
    else:
        return (None, i)

    first = _strip_comment(lines[i]).rstrip()
    first_indent = _leading_spaces(first)
    if first_indent < indent:
        return (None, i)

    content = first.lstrip()
    if content.startswith("- "):
        return _parse_list(lines, i, first_indent)
    return _parse_mapping(lines, i, first_indent)


def _parse_mapping(lines: List[str], start: int, indent: int) -> Tuple[Dict[str, Any], int]:
    out: Dict[str, Any] = {}
    i = start
    while i < len(lines):
        raw = _strip_comment(lines[i]).rstrip()
        if not raw.strip():
            i += 1
            continue
        cur_indent = _leading_spaces(raw)
        if cur_indent < indent:
            break
        if cur_indent > indent:
            # Shouldn't happen if caller passed right indent, but be tolerant.
            i += 1
            continue
        content = raw.lstrip()
        if content.startswith("- "):
            break
        if ":" not in content:
            i += 1
            continue
        key, _, rest = content.partition(":")
        key = key.strip()
        rest = rest.strip()
        if rest:
            if rest.startswith("{"):
                out[key] = _split_inline_mapping(rest)
                i += 1
                continue
            out[key] = _coerce_scalar(rest)
            i += 1
            continue
        # Nested block
        value, i = _parse_block(lines, i + 1, indent + 2)
        out[key] = value if value is not None else {}
    return out, i


def _parse_list(lines: List[str], start: int, indent: int) -> Tuple[List[Any], int]:
    out: List[Any] = []
    i = start
    while i < len(lines):
        raw = _strip_comment(lines[i]).rstrip()
        if not raw.strip():
            i += 1
            continue
        cur_indent = _leading_spaces(raw)
        if cur_indent < indent:
            break
        if cur_indent > indent:
            i += 1
            continue
        content = raw.lstrip()
        if not content.startswith("- "):
            break
        body = content[2:].strip()
        if not body:
            # List-of-blocks: next lines form a mapping at indent + 2
            value, i = _parse_block(lines, i + 1, indent + 2)
            out.append(value if value is not None else {})
            continue
        if ":" in body:
            # Inline mapping starting with `- key: value` — treat as first key of a mapping
            # whose subsequent keys appear at indent + 2.
            synthetic = [" " * (indent + 2) + body] + lines[i + 1:]
            # Simpler: re-parse from this item's embedded line forward.
            value, consumed = _parse_mapping([" " * (indent + 2) + body] + lines[i + 1:], 0, indent + 2)
            out.append(value)
            # consumed is relative to the synthetic list; first synthetic line stood in for lines[i],
            # so real next-index is i + consumed.
            i = i + consumed
            continue
        out.append(_coerce_scalar(body))
        i += 1
    return out, i


# --------------------------------------------------------------------------
# Manifest + scene data shapes
# --------------------------------------------------------------------------

@dataclasses.dataclass(frozen=True)
class ManifestEntry:
    key: str
    file: str
    category: str
    format: str
    scale: float
    source_blend: Optional[str]


@dataclasses.dataclass(frozen=True)
class TerrainHeightRegion:
    name: str
    min_x: int
    min_y: int
    max_x: int
    max_y: int
    level: int


@dataclasses.dataclass(frozen=True)
class TerrainVisualRegion:
    name: str
    min_x: int
    min_y: int
    max_x: int
    max_y: int
    type: str


@dataclasses.dataclass(frozen=True)
class TileOverride:
    x: int
    y: int
    type: str


@dataclasses.dataclass(frozen=True)
class StaticPropEntry:
    key: str
    x: float
    y: float
    rotation_y_degrees: float
    scale: float
    visibility_group: str


@dataclasses.dataclass
class SceneData:
    height_step: float
    height_regions: List[TerrainHeightRegion]
    visual_default: str
    visual_regions: List[TerrainVisualRegion]
    tile_overrides: List[TileOverride]
    static_props: List[StaticPropEntry]


def load_manifest(path: Path) -> Dict[str, ManifestEntry]:
    data = load_yaml(path)
    assets = (data or {}).get("assets", []) if isinstance(data, dict) else []
    out: Dict[str, ManifestEntry] = {}
    for a in assets:
        if not isinstance(a, dict):
            continue
        key = str(a.get("key", "")).strip()
        if not key:
            continue
        out[key] = ManifestEntry(
            key=key,
            file=str(a.get("file", "")).strip(),
            category=str(a.get("category", "")).strip(),
            format=str(a.get("format", "")).strip(),
            scale=float(a.get("scale", 1.0) or 1.0),
            source_blend=(str(a["source_blend"]).strip() if a.get("source_blend") else None),
        )
    return out


def load_scene(path: Path) -> SceneData:
    data = load_yaml(path) or {}
    th = data.get("terrain_height", {}) or {}
    tv = data.get("terrain_visual", {}) or {}
    sp = data.get("static_props", []) or []

    height_regions: List[TerrainHeightRegion] = []
    for r in th.get("regions", []) or []:
        if not isinstance(r, dict):
            continue
        height_regions.append(TerrainHeightRegion(
            name=str(r.get("name", "")),
            min_x=int(r.get("min_x", 0)),
            min_y=int(r.get("min_y", 0)),
            max_x=int(r.get("max_x", 0)),
            max_y=int(r.get("max_y", 0)),
            level=int(r.get("level", 0)),
        ))

    visual_regions: List[TerrainVisualRegion] = []
    for r in tv.get("regions", []) or []:
        if not isinstance(r, dict):
            continue
        visual_regions.append(TerrainVisualRegion(
            name=str(r.get("name", "")),
            min_x=int(r.get("min_x", 0)),
            min_y=int(r.get("min_y", 0)),
            max_x=int(r.get("max_x", 0)),
            max_y=int(r.get("max_y", 0)),
            type=str(r.get("type", "grass")),
        ))

    tile_overrides: List[TileOverride] = []
    for t in tv.get("tile_overrides", []) or []:
        if not isinstance(t, dict):
            continue
        tile_overrides.append(TileOverride(
            x=int(t.get("x", 0)),
            y=int(t.get("y", 0)),
            type=str(t.get("type", "grass")),
        ))

    static_props: List[StaticPropEntry] = []
    for p in sp or []:
        if not isinstance(p, dict):
            continue
        static_props.append(StaticPropEntry(
            key=str(p.get("key", "")).strip(),
            x=float(p.get("x", 0) or 0),
            y=float(p.get("y", 0) or 0),
            rotation_y_degrees=float(p.get("rotation_y_degrees", 0) or 0),
            scale=float(p.get("scale", 1) or 1),
            visibility_group=str(p.get("visibility_group", "base")),
        ))

    return SceneData(
        height_step=float(th.get("height_step", 0.6) or 0.6),
        height_regions=height_regions,
        visual_default=str(tv.get("default_type", "grass")),
        visual_regions=visual_regions,
        tile_overrides=tile_overrides,
        static_props=static_props,
    )


# --------------------------------------------------------------------------
# Logging
# --------------------------------------------------------------------------

def log(tag: str, msg: str) -> None:
    print(f"[{tag}] {msg}", flush=True)
