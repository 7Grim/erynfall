#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
JAR_PATH="$REPO_ROOT/client/target/osrs-client-0.1.0-SNAPSHOT.jar"

echo "[artist-launch] Repo root: $REPO_ROOT"
echo "[artist-launch] Jar path:  $JAR_PATH"
echo "[artist-launch] Mode:      artist"

if [ ! -f "$JAR_PATH" ]; then
  echo "[artist-launch] ERROR: client jar not found."
  echo "[artist-launch] Build it first:"
  echo "  mvn -pl client -am -DskipTests package"
  exit 1
fi

exec java -jar "$JAR_PATH" --artist "--repo-root=$REPO_ROOT"
