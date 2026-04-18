#!/usr/bin/env python3
"""Fix G3DJ animation keyframe times that were written in seconds instead of milliseconds.

G3DJ format requires keyframe times in MILLISECONDS.
The player_base.g3dj was generated with values like 0.3, 0.6, 1.2 (seconds)
instead of 300, 600, 1200 (milliseconds).

This multiplies all "keytime" values by 1000.

Usage:
    python3 scripts/fix-g3dj-keytimes.py art/models/player_base.g3dj
    python3 scripts/fix-g3dj-keytimes.py art/models/player_base.g3dj --dry-run
"""

import json
import sys
import os
import shutil
import argparse


def fix_keytimes(data, multiplier=1000.0):
    """Walk G3DJ animations → bones → keyframes and multiply all 'keytime' values.

    G3DJ structure:
      animations[].bones[].keyframes[].keytime  (primary path)

    Also handles the flat translation/rotation/scaling variant for completeness.
    """
    count = 0
    for anim in data.get("animations", []):
        for bone in anim.get("bones", []):
            # Primary G3DJ path: keyframes array
            for kf in bone.get("keyframes", []):
                if "keytime" in kf and isinstance(kf["keytime"], (int, float)):
                    kf["keytime"] = kf["keytime"] * multiplier
                    count += 1
            # Alternative path: separate translation/rotation/scaling arrays
            for kftype in ("translation", "rotation", "scaling"):
                for kf in bone.get(kftype, []):
                    if "keytime" in kf and isinstance(kf["keytime"], (int, float)):
                        kf["keytime"] = kf["keytime"] * multiplier
                        count += 1
    return count


def main():
    parser = argparse.ArgumentParser(description="Fix G3DJ keytime values (seconds → milliseconds)")
    parser.add_argument("file", help="Path to .g3dj file")
    parser.add_argument("--dry-run", action="store_true", help="Print what would change without writing")
    parser.add_argument("--multiplier", type=float, default=1000.0, help="Multiplier (default: 1000)")
    args = parser.parse_args()

    if not os.path.exists(args.file):
        print(f"ERROR: File not found: {args.file}")
        sys.exit(1)

    print(f"Reading {args.file} ...")
    with open(args.file, "r", encoding="utf-8") as f:
        data = json.load(f)

    # Print animation summary before fix
    animations = data.get("animations", [])
    print(f"\nAnimations found: {len(animations)}")
    for anim in animations:
        name = anim.get("id", "?")
        all_keytimes = []
        for bone in anim.get("bones", []):
            for kf in bone.get("keyframes", []):
                kt = kf.get("keytime")
                if kt is not None:
                    all_keytimes.append(kt)
            for kftype in ("translation", "rotation", "scaling"):
                for kf in bone.get(kftype, []):
                    kt = kf.get("keytime")
                    if kt is not None:
                        all_keytimes.append(kt)
        if all_keytimes:
            print(f"  [{name}] keytimes: {sorted(set(all_keytimes))[:8]} ... (max={max(all_keytimes):.3f})")

    if args.dry_run:
        print(f"\n[DRY RUN] Would multiply all keytime values by {args.multiplier}")
        print("Re-run without --dry-run to apply.")
        return

    # Backup original
    backup_path = args.file + ".bak"
    shutil.copy2(args.file, backup_path)
    print(f"\nBackup saved: {backup_path}")

    # Apply fix
    count = fix_keytimes(data, args.multiplier)
    print(f"Fixed {count} keytime value(s)")

    # Print animation summary after fix
    print(f"\nAnimations after fix:")
    for anim in data.get("animations", []):
        name = anim.get("id", "?")
        all_keytimes = []
        for bone in anim.get("bones", []):
            for kf in bone.get("keyframes", []):
                kt = kf.get("keytime")
                if kt is not None:
                    all_keytimes.append(kt)
            for kftype in ("translation", "rotation", "scaling"):
                for kf in bone.get(kftype, []):
                    kt = kf.get("keytime")
                    if kt is not None:
                        all_keytimes.append(kt)
        if all_keytimes:
            duration_s = max(all_keytimes) / 1000.0
            print(f"  [{name}] max keytime={max(all_keytimes):.0f}ms ({duration_s:.2f}s)")

    # Write fixed file
    with open(args.file, "w", encoding="utf-8") as f:
        json.dump(data, f, separators=(",", ":"))

    print(f"\nWrote fixed file: {args.file}")


if __name__ == "__main__":
    main()
