#!/usr/bin/env python3
"""Validate art/models entries against art/models/manifest.yaml."""

from __future__ import annotations

import argparse
from collections.abc import Iterable
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = ROOT / "art" / "models" / "manifest.yaml"
DEFAULT_MODELS_DIR = ROOT / "art" / "models"


@dataclass(frozen=True)
class ModelEntry:
    key: str
    file: str
    category: str
    format: str
    scale: float
    origin: str
    required: bool
    notes: str


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


def parse_manifest(manifest_path: Path) -> list[ModelEntry]:
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

    required_fields = {"key", "file", "category", "format", "scale", "origin", "required", "notes"}
    entries: list[ModelEntry] = []
    seen_keys: set[str] = set()
    seen_files: set[str] = set()

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

        if fmt not in {"g3dj", "g3db"}:
            raise ValueError(f"Unsupported format for '{key}': {fmt}")
        if not file_name.lower().endswith(f".{fmt}"):
            raise ValueError(f"File extension does not match format for '{key}': {file_name} vs {fmt}")

        entry = ModelEntry(
            key=key,
            file=file_name,
            category=str(item["category"]),
            format=fmt,
            scale=as_float(item["scale"], "scale", key),
            origin=str(item["origin"]),
            required=as_bool(item["required"], "required", key),
            notes=str(item["notes"]),
        )
        if entry.scale <= 0:
            raise ValueError(f"Scale must be > 0 for key '{key}'")

        seen_keys.add(key)
        seen_files.add(file_name)
        entries.append(entry)

    return entries


def validate(entries: list[ModelEntry], models_dir: Path) -> tuple[list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []

    manifest_files = {entry.file for entry in entries}

    for entry in entries:
        model_path = models_dir / entry.file
        if not model_path.exists():
            msg = f"missing model file for key '{entry.key}': {entry.file}"
            if entry.required:
                errors.append(msg)
            else:
                warnings.append(msg)

    known_extensions = {".g3dj", ".g3db"}
    for model_path in sorted(models_dir.iterdir()):
        if model_path.is_dir():
            continue
        if model_path.name == "manifest.yaml":
            continue
        if model_path.suffix.lower() not in known_extensions:
            continue
        if model_path.name not in manifest_files:
            errors.append(f"model file present but not declared in manifest: {model_path.name}")

    return errors, warnings


def write_runtime_key_list(entries: Iterable[ModelEntry], output_path: Path) -> None:
    keys = sorted(entry.key for entry in entries)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    content = "# Auto-generated by scripts/validate-models.py\n" + "\n".join(keys) + "\n"
    output_path.write_text(content, encoding="utf-8")


def write_runtime_metadata(entries: Iterable[ModelEntry], output_path: Path) -> None:
    assets = []
    for entry in sorted(entries, key=lambda e: e.key):
        assets.append(
            {
                "key": entry.key,
                "file": entry.file,
                "category": entry.category,
                "format": entry.format,
                "scale": entry.scale,
                "origin": entry.origin,
                "required": entry.required,
            }
        )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps({"assets": assets}, sort_keys=True, separators=(",", ":")), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate static model assets against manifest.yaml")
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST), help="Path to model manifest YAML")
    parser.add_argument("--models-dir", default=str(DEFAULT_MODELS_DIR), help="Path to model asset directory")
    parser.add_argument("--write-key-list", help="Optional output path for runtime model key list")
    parser.add_argument("--write-runtime-meta", help="Optional output path for runtime model metadata JSON")
    args = parser.parse_args()

    manifest_path = Path(args.manifest).resolve()
    models_dir = Path(args.models_dir).resolve()

    if not models_dir.exists():
        print(f"ERROR: model directory does not exist: {models_dir}")
        return 2

    try:
        entries = parse_manifest(manifest_path)
        errors, warnings = validate(entries, models_dir)
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
