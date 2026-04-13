#!/usr/bin/env python3
"""Plan or execute Blender -> GLB exports for art models."""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = ROOT / "art" / "models" / "manifest.yaml"
DEFAULT_BLENDER_DIR = ROOT / "art" / "blender"
DEFAULT_MODELS_DIR = ROOT / "art" / "models"


@dataclass(frozen=True)
class ManifestExportEntry:
    key: str
    file: str
    fmt: str
    source_blend: str | None


@dataclass(frozen=True)
class ExportPlanItem:
    key: str
    blend_path: Path
    output_path: Path
    source_mode: str


def parse_scalar(raw: str):
    value = raw.strip()
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
    if re.fullmatch(r"-?\d+\.\d+", value):
        return float(value)
    return value


def parse_manifest_entries(manifest_path: Path) -> list[ManifestExportEntry]:
    if not manifest_path.exists():
        raise ValueError(f"Manifest does not exist: {manifest_path}")

    lines = manifest_path.read_text(encoding="utf-8").splitlines()
    in_assets = False
    items: list[dict[str, object]] = []
    current: dict[str, object] | None = None
    active_list_field: str | None = None

    for idx, raw_line in enumerate(lines, start=1):
        line = raw_line.split("#", 1)[0].rstrip()
        if not line.strip():
            continue

        if line.strip() == "assets:":
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
            continue

        active_list_field = None
        pair = re.match(r"^\s{4}([a-z_]+):\s*(.*)$", line)
        if pair:
            if current is None:
                raise ValueError(f"Line {idx}: found field before asset entry")
            field_name = pair.group(1)
            field_value = pair.group(2)
            if field_value == "":
                current[field_name] = []
                active_list_field = field_name
            else:
                current[field_name] = parse_scalar(field_value)
            continue

        raise ValueError(f"Line {idx}: unsupported manifest syntax: {raw_line}")

    if current is not None:
        items.append(current)

    entries: list[ManifestExportEntry] = []
    for item in items:
        key = str(item.get("key", "")).strip()
        file_name = str(item.get("file", "")).strip()
        fmt = str(item.get("format", "")).strip().lower()
        source_blend = str(item.get("source_blend", "")).strip() or None
        if not key or not file_name or not fmt:
            continue
        entries.append(ManifestExportEntry(key=key, file=file_name, fmt=fmt, source_blend=source_blend))
    return entries


def build_export_plan(entries: list[ManifestExportEntry], blender_dir: Path, models_dir: Path) -> tuple[list[ExportPlanItem], list[str], list[str]]:
    errors: list[str] = []
    warnings: list[str] = []
    plan: list[ExportPlanItem] = []

    if not blender_dir.exists():
        errors.append(f"Blender source directory does not exist: {blender_dir}")
        return plan, warnings, errors

    blends = sorted(path for path in blender_dir.rglob("*.blend") if path.is_file())
    if not blends:
        warnings.append(f"No .blend files found in {blender_dir}")

    blend_by_stem: dict[str, list[Path]] = {}
    for blend in blends:
        blend_by_stem.setdefault(blend.stem.lower(), []).append(blend)

    glb_entries = [entry for entry in entries if entry.fmt == "glb"]
    if not glb_entries:
        warnings.append("Manifest has no GLB entries to export")

    for entry in glb_entries:
        output_path = models_dir / entry.file
        blend_path: Path | None = None
        source_mode = ""

        if entry.source_blend:
            candidate = (blender_dir / entry.source_blend).resolve()
            try:
                candidate.relative_to(blender_dir.resolve())
            except ValueError:
                errors.append(
                    f"Entry '{entry.key}' has source_blend outside art/blender/: {entry.source_blend}"
                )
                continue
            if not candidate.exists():
                errors.append(
                    f"Entry '{entry.key}' points to missing source_blend file: {entry.source_blend}"
                )
                continue
            blend_path = candidate
            source_mode = "source_blend"
        else:
            matches = blend_by_stem.get(Path(entry.file).stem.lower(), [])
            if len(matches) == 1:
                blend_path = matches[0]
                source_mode = "stem-match"
            elif len(matches) > 1:
                errors.append(
                    f"Entry '{entry.key}' matched multiple .blend files by stem '{Path(entry.file).stem}'; add source_blend"
                )
                continue
            else:
                errors.append(
                    f"Entry '{entry.key}' has no source_blend and no .blend stem match for '{Path(entry.file).stem}.blend'"
                )
                continue

        plan.append(ExportPlanItem(key=entry.key, blend_path=blend_path, output_path=output_path, source_mode=source_mode))

    declared_sources: set[Path] = set()
    for entry in glb_entries:
        if entry.source_blend:
            declared_sources.add((blender_dir / entry.source_blend).resolve())
    if declared_sources:
        undeclared = [path for path in blends if path.resolve() not in declared_sources]
        if undeclared:
            warnings.append(
                "Some .blend files are not mapped by source_blend fields (stem fallback may still map them): "
                + ", ".join(str(path.relative_to(blender_dir)) for path in undeclared[:8])
                + (" ..." if len(undeclared) > 8 else "")
            )

    return plan, warnings, errors


def run_blender_export(blender_exe: str, item: ExportPlanItem) -> None:
    item.output_path.parent.mkdir(parents=True, exist_ok=True)
    script = """
import bpy
import sys

argv = sys.argv
if "--" not in argv:
    raise SystemExit("Missing '--' argument separator for export script")
output_path = argv[argv.index("--") + 1]
bpy.ops.export_scene.gltf(
    filepath=output_path,
    export_format='GLB',
    export_apply=True,
)
print(f"Exported GLB: {output_path}")
""".strip()

    with tempfile.NamedTemporaryFile(mode="w", suffix="_blender_export.py", delete=False, encoding="utf-8") as tmp:
        tmp.write(script)
        tmp_path = Path(tmp.name)

    try:
        command = [
            blender_exe,
            "--background",
            str(item.blend_path),
            "--python",
            str(tmp_path),
            "--",
            str(item.output_path),
        ]
        subprocess.run(command, check=True)
    finally:
        try:
            tmp_path.unlink(missing_ok=True)
        except OSError:
            pass


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Plan or execute canonical Blender (.blend) -> runtime GLB exports"
    )
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST), help="Path to art model manifest")
    parser.add_argument("--blender-dir", default=str(DEFAULT_BLENDER_DIR), help="Path to Blender source directory")
    parser.add_argument("--models-dir", default=str(DEFAULT_MODELS_DIR), help="Path to runtime model directory")
    parser.add_argument("--blender-exe", default="blender", help="Blender executable path (used with --run)")
    parser.add_argument("--dry-run", action="store_true", help="Print export plan without invoking Blender")
    parser.add_argument("--run", action="store_true", help="Run Blender exports for planned items")
    args = parser.parse_args()

    if args.run and args.dry_run:
        print("ERROR: choose either --run or --dry-run (not both)")
        return 2
    if not args.run and not args.dry_run:
        args.dry_run = True

    manifest_path = Path(args.manifest).resolve()
    blender_dir = Path(args.blender_dir).resolve()
    models_dir = Path(args.models_dir).resolve()

    try:
        entries = parse_manifest_entries(manifest_path)
    except ValueError as exc:
        print(f"ERROR: {exc}")
        return 2

    plan, warnings, errors = build_export_plan(entries, blender_dir, models_dir)
    for warning in warnings:
        print(f"WARN: {warning}")
    if errors:
        print("BLENDER EXPORT PLAN FAILED")
        for error in errors:
            print(f" - {error}")
        return 1

    if not plan:
        print("No export actions planned")
        return 0

    print(f"Planned exports: {len(plan)}")
    for item in plan:
        print(
            f" - {item.key}: {item.blend_path.relative_to(blender_dir)} -> "
            f"{item.output_path.relative_to(models_dir)} [{item.source_mode}]"
        )

    if args.dry_run:
        print("DRY RUN: no files exported")
        return 0

    for item in plan:
        try:
            run_blender_export(args.blender_exe, item)
        except FileNotFoundError:
            print(
                "ERROR: Blender executable not found. Set --blender-exe or install Blender in PATH."
            )
            return 2
        except subprocess.CalledProcessError as exc:
            print(f"ERROR: Blender export failed for key '{item.key}' with exit code {exc.returncode}")
            return 1

    print("BLENDER EXPORTS COMPLETE")
    return 0


if __name__ == "__main__":
    sys.exit(main())
