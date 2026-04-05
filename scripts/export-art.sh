#!/usr/bin/env bash
# export-art.sh — Exports all Aseprite source files to art/sprites/
#
# Usage:
#   ./scripts/export-art.sh              # export everything in art/aseprite/
#   ./scripts/export-art.sh player.ase   # export a single file
#
# Requires Aseprite to be installed and on your PATH, or set ASEPRITE=/path/to/aseprite.
#
# After running this script, rebuild the atlas:
#   mvn generate-resources -pl client -am
# Then start the client to see your changes, or press F5 in-game to hot-reload.

set -euo pipefail

ASEPRITE="${ASEPRITE:-aseprite}"
ASE_DIR="$(dirname "$0")/../art/aseprite"
OUT_DIR="$(dirname "$0")/../art/sprites"

if ! command -v "$ASEPRITE" &>/dev/null; then
  echo "ERROR: aseprite not found. Install Aseprite or set ASEPRITE=/path/to/aseprite"
  exit 1
fi

mkdir -p "$OUT_DIR"

export_file() {
  local ase_file="$1"
  local base
  base="$(basename "$ase_file" .aseprite)"
  base="$(basename "$base" .ase)"

  # Check whether the file has animation tags
  local tags
  tags="$("$ASEPRITE" -b --list-tags "$ase_file" 2>/dev/null || true)"

  if [ -n "$tags" ]; then
    # Animated: export each tag as numbered frames
    # Output: {base}_{tag}_{frameIndex}.png  (e.g. player_walk_n_0.png)
    "$ASEPRITE" -b \
      --split-tags \
      --filename-format "{title}_{tag}_{tagframe}.png" \
      "$ase_file" \
      --save-as "$OUT_DIR/${base}_{tag}_{tagframe}.png"
    echo "  Exported (animated): $base"
  else
    # Static: single flat PNG
    "$ASEPRITE" -b "$ase_file" --save-as "$OUT_DIR/${base}.png"
    echo "  Exported (static):   $base"
  fi
}

if [ $# -eq 0 ]; then
  # Export all .aseprite / .ase files in art/aseprite/
  found=0
  for f in "$ASE_DIR"/*.aseprite "$ASE_DIR"/*.ase; do
    [ -f "$f" ] || continue
    export_file "$f"
    found=$((found + 1))
  done
  if [ "$found" -eq 0 ]; then
    echo "No .aseprite or .ase files found in $ASE_DIR"
    exit 0
  fi
else
  # Export the specified file(s)
  for f in "$@"; do
    export_file "$f"
  done
fi

echo ""
echo "Done. Now run: mvn generate-resources -pl client -am"
echo "Then start the client (or press F5 in-game) to see your sprites."
