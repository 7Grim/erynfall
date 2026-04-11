#!/usr/bin/env python3
"""Audit 3D equipment coverage against equipable items and policy."""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_ITEMS = ROOT / "server" / "src" / "main" / "resources" / "items.yaml"
DEFAULT_MANIFEST = ROOT / "art" / "models" / "manifest.yaml"
DEFAULT_POLICY = ROOT / "art" / "models" / "equipment_coverage_policy.yaml"

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
SLOT_ID_TO_NAME = {v: k for k, v in SLOT_NAME_TO_ID.items()}


@dataclass(frozen=True)
class EquipableItem:
    item_id: int
    name: str
    slot_id: int
    slot_name: str


def parse_scalar(raw: str):
    value = raw.strip()
    if value == "":
        return ""
    if value.startswith('"') and value.endswith('"') and len(value) >= 2:
        return value[1:-1]
    if value.startswith("'") and value.endswith("'") and len(value) >= 2:
        return value[1:-1]
    lower = value.lower()
    if lower == "true":
        return True
    if lower == "false":
        return False
    if re.fullmatch(r"-?\d+", value):
        return int(value)
    if re.fullmatch(r"-?\d+\.\d+", value):
        return float(value)
    return value


def parse_items_yaml(items_path: Path) -> list[dict[str, object]]:
    lines = items_path.read_text(encoding="utf-8").splitlines()
    in_items = False
    items: list[dict[str, object]] = []
    current: dict[str, object] | None = None

    for idx, raw_line in enumerate(lines, start=1):
        line = raw_line.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        stripped = line.strip()
        if stripped == "items:":
            in_items = True
            continue
        if not in_items:
            continue

        item_start = re.match(r"^\s*-\s+id:\s*(.+)$", line)
        if item_start:
            if current is not None:
                items.append(current)
            current = {"id": parse_scalar(item_start.group(1))}
            continue

        pair = re.match(r"^\s{4}([a-z_]+):\s*(.*)$", line)
        if pair:
            if current is None:
                raise ValueError(f"Line {idx}: found field before item entry")
            current[pair.group(1)] = parse_scalar(pair.group(2))
            continue

        raise ValueError(f"Line {idx}: unsupported items.yaml syntax: {raw_line}")

    if current is not None:
        items.append(current)
    return items


def parse_model_manifest(manifest_path: Path) -> set[tuple[int, int]]:
    lines = manifest_path.read_text(encoding="utf-8").splitlines()
    in_assets = False
    current: dict[str, object] | None = None
    covered: set[tuple[int, int]] = set()

    def consume(item: dict[str, object] | None) -> None:
        if item is None:
            return
        if str(item.get("category", "")).strip() != "equipment":
            return
        slot = item.get("equip_slot")
        item_id = item.get("item_id")
        if not isinstance(slot, int) or not isinstance(item_id, int):
            return
        covered.add((slot, item_id))

    for idx, raw_line in enumerate(lines, start=1):
        line = raw_line.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        stripped = line.strip()
        if stripped == "assets:":
            in_assets = True
            continue
        if not in_assets:
            continue

        start = re.match(r"^\s*-\s+key:\s*(.+)$", line)
        if start:
            consume(current)
            current = {"key": parse_scalar(start.group(1))}
            continue

        pair = re.match(r"^\s{4}([a-z_]+):\s*(.*)$", line)
        if pair:
            if current is None:
                raise ValueError(f"Line {idx}: found field before asset entry")
            current[pair.group(1)] = parse_scalar(pair.group(2))
            continue

        raise ValueError(f"Line {idx}: unsupported manifest syntax: {raw_line}")

    consume(current)
    return covered


def parse_policy(policy_path: Path) -> tuple[set[int], set[int], set[int]]:
    lines = policy_path.read_text(encoding="utf-8").splitlines()
    visible_slots: set[int] = set()
    deferred_slots: set[int] = set()
    deferred_item_ids: set[int] = set()
    section: str | None = None

    for idx, raw_line in enumerate(lines, start=1):
        line = raw_line.split("#", 1)[0].rstrip()
        if not line.strip():
            continue
        if re.match(r"^[a-z_]+:\s*$", line.strip()):
            section = line.strip()[:-1]
            continue

        inline_deferred = re.match(r"^deferred_item_ids:\s*\[(.*)]\s*$", line.strip())
        if inline_deferred:
            section = "deferred_item_ids"
            raw_values = [part.strip() for part in inline_deferred.group(1).split(",") if part.strip()]
            for raw_value in raw_values:
                value = parse_scalar(raw_value)
                if not isinstance(value, int):
                    raise ValueError(f"Line {idx}: deferred_item_ids values must be integers")
                deferred_item_ids.add(value)
            continue

        item = re.match(r"^\s*-\s*(.+)$", line)
        if not item:
            raise ValueError(f"Line {idx}: unsupported policy syntax: {raw_line}")

        value = parse_scalar(item.group(1))
        if section == "visible_slots":
            slot_name = str(value).upper()
            if slot_name not in SLOT_NAME_TO_ID:
                raise ValueError(f"Line {idx}: unknown slot '{value}' in visible_slots")
            visible_slots.add(SLOT_NAME_TO_ID[slot_name])
        elif section == "deferred_slots":
            slot_name = str(value).upper()
            if slot_name not in SLOT_NAME_TO_ID:
                raise ValueError(f"Line {idx}: unknown slot '{value}' in deferred_slots")
            deferred_slots.add(SLOT_NAME_TO_ID[slot_name])
        elif section == "deferred_item_ids":
            if not isinstance(value, int):
                raise ValueError(f"Line {idx}: deferred_item_ids values must be integers")
            deferred_item_ids.add(value)
        else:
            raise ValueError(f"Line {idx}: list item outside known section")

    return visible_slots, deferred_slots, deferred_item_ids


def to_slot_id(raw_slot: object) -> int | None:
    if isinstance(raw_slot, int):
        return raw_slot
    if isinstance(raw_slot, str):
        stripped = raw_slot.strip()
        if stripped.upper() in SLOT_NAME_TO_ID:
            return SLOT_NAME_TO_ID[stripped.upper()]
        if re.fullmatch(r"-?\d+", stripped):
            return int(stripped)
    return None


def collect_equipable_items(items: list[dict[str, object]]) -> list[EquipableItem]:
    equipable_items: list[EquipableItem] = []
    for item in items:
        if item.get("equipable") is not True:
            continue
        item_id = item.get("id")
        slot_id = to_slot_id(item.get("equip_slot"))
        if not isinstance(item_id, int) or slot_id is None:
            continue
        slot_name = SLOT_ID_TO_NAME.get(slot_id, f"SLOT_{slot_id}")
        equipable_items.append(
            EquipableItem(
                item_id=item_id,
                name=str(item.get("name", f"Item {item_id}")),
                slot_id=slot_id,
                slot_name=slot_name,
            )
        )
    return sorted(equipable_items, key=lambda i: (i.slot_id, i.item_id))


def summarize(items: list[EquipableItem],
              covered_pairs: set[tuple[int, int]],
              visible_slots: set[int],
              deferred_slots: set[int],
              deferred_item_ids: set[int]):
    covered: list[EquipableItem] = []
    deferred: list[EquipableItem] = []
    missing: list[EquipableItem] = []

    for item in items:
        pair = (item.slot_id, item.item_id)
        if item.slot_id in deferred_slots or item.item_id in deferred_item_ids:
            deferred.append(item)
        elif item.slot_id in visible_slots:
            if pair in covered_pairs:
                covered.append(item)
            else:
                missing.append(item)
        else:
            deferred.append(item)

    visible_total = sum(1 for item in items if item.slot_id in visible_slots)
    summary = {
        "equipable_total": len(items),
        "visible_total": visible_total,
        "covered": len(covered),
        "deferred": len(deferred),
        "missing": len(missing),
    }
    return covered, deferred, missing, summary


def write_json_report(path: Path,
                      covered: list[EquipableItem],
                      deferred: list[EquipableItem],
                      missing: list[EquipableItem],
                      summary: dict[str, int]) -> None:
    report = {
        "covered": [
            {"item_id": item.item_id, "name": item.name, "slot": item.slot_name}
            for item in covered
        ],
        "deferred": [
            {"item_id": item.item_id, "name": item.name, "slot": item.slot_name}
            for item in deferred
        ],
        "missing": [
            {"item_id": item.item_id, "name": item.name, "slot": item.slot_name}
            for item in missing
        ],
        "summary": summary,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")


def print_report(missing: list[EquipableItem], summary: dict[str, int]) -> None:
    print("3D EQUIPMENT COVERAGE AUDIT")
    print(f"- Equipable items total: {summary['equipable_total']}")
    print(f"- Visible-slot items total: {summary['visible_total']}")
    print(f"- Covered: {summary['covered']}")
    print(f"- Deferred: {summary['deferred']}")
    print(f"- Missing: {summary['missing']}")

    if not missing:
        print("\nMissing visible-slot coverage: none")
        return

    grouped: dict[int, list[EquipableItem]] = defaultdict(list)
    for item in missing:
        grouped[item.slot_id].append(item)

    print("\nMissing visible-slot coverage by slot:")
    for slot_id in sorted(grouped):
        print(f"- {SLOT_ID_TO_NAME.get(slot_id, f'SLOT_{slot_id}')}")
        for item in grouped[slot_id]:
            print(f"  - {item.item_id}: {item.name}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit 3D equipment coverage against gameplay items")
    parser.add_argument("--items", default=str(DEFAULT_ITEMS), help="Path to items.yaml")
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST), help="Path to model manifest.yaml")
    parser.add_argument("--policy", default=str(DEFAULT_POLICY), help="Path to equipment coverage policy yaml")
    parser.add_argument("--write-report", help="Optional JSON report output path")
    parser.add_argument("--strict", action="store_true", help="Exit non-zero when missing visible-slot coverage exists")
    args = parser.parse_args()

    try:
        items = parse_items_yaml(Path(args.items).resolve())
        covered_pairs = parse_model_manifest(Path(args.manifest).resolve())
        visible_slots, deferred_slots, deferred_item_ids = parse_policy(Path(args.policy).resolve())
        equipable_items = collect_equipable_items(items)
        covered, deferred, missing, summary = summarize(
            equipable_items,
            covered_pairs,
            visible_slots,
            deferred_slots,
            deferred_item_ids,
        )
    except ValueError as exc:
        print(f"ERROR: {exc}")
        return 2

    print_report(missing, summary)
    if args.write_report:
        write_json_report(Path(args.write_report).resolve(), covered, deferred, missing, summary)

    if args.strict and summary["missing"] > 0:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
