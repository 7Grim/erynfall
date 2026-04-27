#!/usr/bin/env python3
"""
scripts/gen-art-debt.py

Aggregates all visual validation outputs into docs/ART_DEBT_BOARD.md.

Priority mapping:
  CRITICAL findings     → Must fix before next screenshot
  WARNING findings      → Must fix before public alpha
  INFO / billboard      → Nice to have
  Legacy formats (G3DJ) → Deprecated / legacy

Sources:
  scripts/audit-assets.py      --json
  scripts/validate-scene.py    --json (main_world + sandbox)
  scripts/report-entity-visuals.py  --json

Usage:
    python3 scripts/gen-art-debt.py
    python3 scripts/gen-art-debt.py --dry-run   # print to stdout, no file write
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT       = Path(__file__).resolve().parents[1]
OUTPUT     = ROOT / "docs" / "ART_DEBT_BOARD.md"
SCRIPTS    = Path(__file__).parent


# ── Run a sub-script and return parsed JSON ────────────────────────────────────

def _run_json(script: Path, extra_args: list[str] | None = None) -> list[dict]:
    cmd = [sys.executable, str(script), "--json", "--exit-zero"]
    if extra_args:
        cmd.extend(extra_args)
    result = subprocess.run(cmd, capture_output=True, text=True, cwd=str(ROOT))
    if result.returncode not in (0, 1):
        print(f"WARNING: {script.name} exited {result.returncode}: {result.stderr.strip()[:200]}",
              file=sys.stderr)
        return []
    try:
        data = json.loads(result.stdout)
        if isinstance(data, list):
            return data
        return []
    except json.JSONDecodeError:
        return []


# ── Collect findings ───────────────────────────────────────────────────────────

def collect() -> dict:
    audit_findings      = _run_json(SCRIPTS / "audit-assets.py")
    scene_main_findings = _run_json(SCRIPTS / "validate-scene.py",  ["--world-id", "main_world"])
    scene_sand_findings = _run_json(SCRIPTS / "validate-scene.py",  ["--world-id", "sandbox"])
    entity_findings     = _run_json(SCRIPTS / "report-entity-visuals.py")

    # Normalise entity findings into Finding-like dicts
    entity_dicts: list[dict] = []
    for r in entity_findings:
        status = r.get("status", "OK")
        if status == "OK":
            continue
        level = "CRITICAL" if status in {"NO_SPRITE", "MISSING_GLB"} else "INFO"
        entity_dicts.append({
            "level":   level,
            "key":     f"entity:{r.get('definition_id')}:{r.get('name')}",
            "code":    f"ENTITY_{status}",
            "message": r.get("note", ""),
            "source":  "report-entity-visuals",
        })

    # Tag audit findings with source
    for f in audit_findings:
        f.setdefault("source", "audit-assets")
    for f in scene_main_findings:
        f.setdefault("source", "validate-scene[main_world]")
    for f in scene_sand_findings:
        f.setdefault("source", "validate-scene[sandbox]")

    all_findings = audit_findings + scene_main_findings + scene_sand_findings + entity_dicts
    return {
        "findings": all_findings,
        "counts": {
            "audit_assets":    len(audit_findings),
            "scene_main":      len(scene_main_findings),
            "scene_sandbox":   len(scene_sand_findings),
            "entity_visuals":  len(entity_dicts),
        },
    }


# ── Categorise ────────────────────────────────────────────────────────────────

def _priority(finding: dict) -> int:
    code  = finding.get("code", "")
    level = finding.get("level", "INFO")
    # Legacy codes always go to their own bucket (lowest priority = 3)
    if code in {"LEGACY_G3DJ", "LEGACY_G3DB", "NO_SOURCE_BLEND", "SOURCE_BLEND_MISSING",
                "FRAGMENTED_ACTOR", "FRAGMENTED_ACTOR_NO_BASE"}:
        return 3
    if level == "CRITICAL":
        return 0
    if level == "WARNING":
        return 1
    return 2


_PRIORITY_LABELS = {
    0: "Must fix before next screenshot",
    1: "Must fix before public alpha",
    2: "Nice to have",
    3: "Deprecated / legacy",
}

_PRIORITY_EMOJI = {
    0: "🔴",
    1: "🟡",
    2: "🔵",
    3: "⚪",
}


def _group_findings(findings: list[dict]) -> dict[int, list[dict]]:
    groups: dict[int, list[dict]] = {0: [], 1: [], 2: [], 3: []}
    for f in findings:
        groups[_priority(f)].append(f)
    return groups


# ── Markdown generation ────────────────────────────────────────────────────────

def _finding_row(f: dict) -> str:
    key    = f.get("key", "")
    code   = f.get("code", "")
    msg    = f.get("message", "")
    source = f.get("source", "")
    # Truncate long messages
    if len(msg) > 140:
        msg = msg[:137] + "..."
    return f"| `{key}` | `{code}` | {msg} | {source} |"


def generate_markdown(data: dict) -> str:
    findings = data["findings"]
    counts   = data["counts"]
    groups   = _group_findings(findings)
    now      = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    total = sum(len(g) for g in groups.values())
    n_crit = len(groups[0])
    n_warn = len(groups[1])

    lines: list[str] = []
    lines.append("# Art Debt Board")
    lines.append("")
    lines.append(f"_Generated {now} by `scripts/gen-art-debt.py`._")
    lines.append("_Do not edit by hand — re-run the script to refresh._")
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    lines.append("| Source | Findings |")
    lines.append("|--------|----------|")
    lines.append(f"| audit-assets.py | {counts['audit_assets']} |")
    lines.append(f"| validate-scene.py (main_world) | {counts['scene_main']} |")
    lines.append(f"| validate-scene.py (sandbox) | {counts['scene_sandbox']} |")
    lines.append(f"| report-entity-visuals.py | {counts['entity_visuals']} |")
    lines.append(f"| **Total** | **{total}** |")
    lines.append("")

    if n_crit == 0 and n_warn == 0:
        lines.append("> ✅ No blockers. All clear for next screenshot and public alpha.")
    elif n_crit == 0:
        lines.append(f"> 🟡 {n_warn} warning(s) — fix before public alpha.")
    else:
        lines.append(f"> 🔴 {n_crit} critical issue(s) — fix before next screenshot.")
    lines.append("")
    lines.append("---")
    lines.append("")

    for priority in range(4):
        group = groups[priority]
        if not group:
            continue
        emoji = _PRIORITY_EMOJI[priority]
        label = _PRIORITY_LABELS[priority]
        lines.append(f"## {emoji} {label}  ({len(group)})")
        lines.append("")
        lines.append("| Key | Code | Detail | Source |")
        lines.append("|-----|------|--------|--------|")
        # Sort: by code then key
        for f in sorted(group, key=lambda x: (x.get("code", ""), x.get("key", ""))):
            lines.append(_finding_row(f))
        lines.append("")

    return "\n".join(lines)


# ── Main ──────────────────────────────────────────────────────────────────────

def main() -> int:
    parser = argparse.ArgumentParser(
        description="Generate docs/ART_DEBT_BOARD.md from all validation scripts.",
    )
    parser.add_argument("--dry-run", action="store_true",
                        help="Print markdown to stdout instead of writing file")
    args = parser.parse_args()

    print("Collecting findings...", file=sys.stderr)
    data     = collect()
    markdown = generate_markdown(data)

    if args.dry_run:
        print(markdown)
        return 0

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(markdown, encoding="utf-8")
    total = sum(data["counts"].values())
    print(f"Written {OUTPUT.relative_to(ROOT)}  ({total} findings)", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
