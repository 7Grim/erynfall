#!/usr/bin/env python3
"""Validate art/sprites PNGs against art/sprites/manifest.yaml."""

from __future__ import annotations

import argparse
from collections.abc import Iterable
import re
import struct
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = ROOT / "art" / "sprites" / "manifest.yaml"
DEFAULT_SPRITES_DIR = ROOT / "art" / "sprites"


@dataclass(frozen=True)
class ManifestEntry:
    key: str
    file: str
    category: str
    canvas_width: int
    canvas_height: int
    pivot: str
    required: bool
    animated: bool
    notes: str


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


def as_int(value: object, field_name: str, key: str) -> int:
    if isinstance(value, int):
        return value
    if isinstance(value, str) and re.fullmatch(r"-?\d+", value.strip()):
        return int(value)
    raise ValueError(f"Field '{field_name}' for key '{key}' must be an integer")


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
    if re.fullmatch(r"-?\d+", value):
        return int(value)
    return value


def parse_manifest(manifest_path: Path) -> list[ManifestEntry]:
    if not manifest_path.exists():
        raise ValueError(f"Manifest does not exist: {manifest_path}")

    lines = manifest_path.read_text(encoding="utf-8").splitlines()
    in_assets = False
    items: list[dict[str, object]] = []
    current: dict[str, object] | None = None

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
            if current is not None:
                items.append(current)
            current = {"key": parse_scalar(entry_start.group(1))}
            continue

        pair = re.match(r"^\s{4}([a-z_]+):\s*(.*)$", line)
        if pair:
            if current is None:
                raise ValueError(f"Line {index}: found field before list item: {raw_line}")
            current[pair.group(1)] = parse_scalar(pair.group(2))
            continue

        raise ValueError(f"Line {index}: unsupported manifest syntax: {raw_line}")

    if current is not None:
        items.append(current)

    if not items:
        raise ValueError("No entries found under assets:")

    required_fields = {
        "key",
        "file",
        "category",
        "canvas_width",
        "canvas_height",
        "pivot",
        "required",
        "animated",
        "notes",
    }

    result: list[ManifestEntry] = []
    seen_keys: set[str] = set()
    seen_files: set[str] = set()

    for item in items:
        missing = sorted(required_fields - set(item.keys()))
        if missing:
            raise ValueError(f"Entry for key '{item.get('key')}' is missing fields: {', '.join(missing)}")

        entry = ManifestEntry(
            key=str(item["key"]),
            file=str(item["file"]),
            category=str(item["category"]),
            canvas_width=as_int(item["canvas_width"], "canvas_width", str(item["key"])),
            canvas_height=as_int(item["canvas_height"], "canvas_height", str(item["key"])),
            pivot=str(item["pivot"]),
            required=as_bool(item["required"], "required", str(item["key"])),
            animated=as_bool(item["animated"], "animated", str(item["key"])),
            notes=str(item["notes"]),
        )

        if entry.key in seen_keys:
            raise ValueError(f"Duplicate key in manifest: {entry.key}")
        if entry.file in seen_files:
            raise ValueError(f"Duplicate file in manifest: {entry.file}")
        if not entry.file.endswith(".png"):
            raise ValueError(f"Manifest file for '{entry.key}' must end with .png: {entry.file}")
        if entry.canvas_width <= 0 or entry.canvas_height <= 0:
            raise ValueError(f"Manifest dimensions must be positive for key '{entry.key}'")

        seen_keys.add(entry.key)
        seen_files.add(entry.file)
        result.append(entry)

    return result


def png_dimensions(path: Path) -> tuple[int, int]:
    with path.open("rb") as handle:
        signature = handle.read(8)
        if signature != b"\x89PNG\r\n\x1a\n":
            raise ValueError(f"Not a valid PNG file: {path}")
        length = struct.unpack(">I", handle.read(4))[0]
        chunk_type = handle.read(4)
        if chunk_type != b"IHDR" or length < 8:
            raise ValueError(f"Missing IHDR in PNG: {path}")
        width, height = struct.unpack(">II", handle.read(8))
    return width, height


def is_frame_for_key(file_name: str, key: str) -> bool:
    simple = re.match(rf"^{re.escape(key)}_(\d+)\.png$", file_name)
    tagged = re.match(rf"^{re.escape(key)}_(.+)_(\d+)\.png$", file_name)
    return simple is not None or tagged is not None


def collect_animation_frames(key: str, sprite_dir: Path) -> dict[str, list[tuple[int, Path]]]:
    simple_pattern = re.compile(rf"^{re.escape(key)}_(\d+)\.png$")
    tagged_pattern = re.compile(rf"^{re.escape(key)}_(.+)_(\d+)\.png$")
    by_tag: dict[str, list[tuple[int, Path]]] = {}
    for png_path in sprite_dir.glob("*.png"):
        simple_match = simple_pattern.match(png_path.name)
        if simple_match:
            tag = "default"
            frame_index = int(simple_match.group(1))
        else:
            tagged_match = tagged_pattern.match(png_path.name)
            if not tagged_match:
                continue
            tag = tagged_match.group(1)
            frame_index = int(tagged_match.group(2))
        by_tag.setdefault(tag, []).append((frame_index, png_path))
    return by_tag


def validate(entries: list[ManifestEntry], sprites_dir: Path) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []

    manifest_by_key = {entry.key: entry for entry in entries}
    manifest_files = {entry.file for entry in entries}

    for entry in entries:
        png_path = sprites_dir / entry.file
        placeholder_path = sprites_dir / f"{entry.file}.placeholder"
        frame_groups = collect_animation_frames(entry.key, sprites_dir)
        has_frames = any(frame_groups.values())

        if entry.required and not png_path.exists() and not placeholder_path.exists() and not has_frames:
            errors.append(
                f"missing required asset for key '{entry.key}': expected {entry.file} or {entry.file}.placeholder"
            )

        if png_path.exists():
            try:
                width, height = png_dimensions(png_path)
            except ValueError as exc:
                errors.append(str(exc))
                width, height = -1, -1
            if width != entry.canvas_width or height != entry.canvas_height:
                errors.append(
                    f"dimension mismatch for '{entry.key}': got {width}x{height}, expected "
                    f"{entry.canvas_width}x{entry.canvas_height} ({entry.file})"
                )

        if entry.animated:
            if not png_path.exists() and not has_frames and not placeholder_path.exists():
                errors.append(
                    f"animated key '{entry.key}' has no base PNG or frame sequence"
                )
            for tag, frames in frame_groups.items():
                indices = sorted(index for index, _ in frames)
                expected = list(range(indices[-1] + 1))
                if indices != expected:
                    errors.append(
                        f"non-contiguous frame sequence for '{entry.key}' tag '{tag}': "
                        f"found {indices}, expected {expected}"
                    )
                for _, frame_path in frames:
                    width, height = png_dimensions(frame_path)
                    if width != entry.canvas_width or height != entry.canvas_height:
                        errors.append(
                            f"dimension mismatch for frame '{frame_path.name}': got {width}x{height}, "
                            f"expected {entry.canvas_width}x{entry.canvas_height}"
                        )
        elif has_frames:
            errors.append(
                f"unexpected animation frames for non-animated key '{entry.key}'"
            )

    for png_path in sorted(sprites_dir.glob("*.png")):
        name = png_path.name
        if name in manifest_files:
            continue
        belongs_to_animated_key = any(
            manifest_by_key[key].animated and is_frame_for_key(name, key)
            for key in manifest_by_key
        )
        if belongs_to_animated_key:
            continue
        errors.append(f"unknown PNG not declared in manifest: {name}")

    if not (sprites_dir / "pack.json").exists():
        warnings.append("pack.json not found in art/sprites (TexturePacker config missing)")

    return errors, warnings


def write_runtime_key_list(entries: Iterable[ManifestEntry], output_path: Path) -> None:
    keys = sorted(entry.key for entry in entries if entry.required)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    content = "# Auto-generated by scripts/validate-art.py\n" + "\n".join(keys) + "\n"
    output_path.write_text(content, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate art assets against manifest.yaml")
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST), help="Path to manifest YAML")
    parser.add_argument("--sprites-dir", default=str(DEFAULT_SPRITES_DIR), help="Path to sprite PNG directory")
    parser.add_argument("--write-key-list", help="Optional output path for runtime expected atlas keys")
    args = parser.parse_args()

    manifest_path = Path(args.manifest).resolve()
    sprites_dir = Path(args.sprites_dir).resolve()

    if not sprites_dir.exists():
        print(f"ERROR: sprites directory does not exist: {sprites_dir}")
        return 2

    try:
        entries = parse_manifest(manifest_path)
        errors, warnings = validate(entries, sprites_dir)
    except ValueError as exc:
        print(f"ERROR: {exc}")
        return 2

    for warning in warnings:
        print(f"WARN: {warning}")

    if errors:
        print("ART VALIDATION FAILED")
        for error in errors:
            print(f" - {error}")
        return 1

    if args.write_key_list:
        write_runtime_key_list(entries, Path(args.write_key_list).resolve())

    print("ART VALIDATION OK")
    return 0


if __name__ == "__main__":
    sys.exit(main())
