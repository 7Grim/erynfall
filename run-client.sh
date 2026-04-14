#!/bin/bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$DIR/client/target/osrs-client-0.1.0-SNAPSHOT.jar"

usage() {
  cat <<'EOF'
Usage: ./run-client.sh [jvm args] [-- client args]

Environment:
  GAME_SERVER_HOST   Override the game server host (defaults to localhost)

Examples:
  ./run-client.sh
  GAME_SERVER_HOST=localhost ./run-client.sh
  GAME_SERVER_HOST=165.22.37.200 ./run-client.sh
  ./run-client.sh -Xmx2g

Notes:
  - This launcher uses the packaged client jar instead of Maven exec:exec.
  - On macOS it automatically adds -XstartOnFirstThread.
  - If the client jar is missing, build it with:
      mvn -pl shared,client -am -DskipTests package
EOF
}

declare -a JVM_ARGS
declare -a APP_ARGS

while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    -D*|-X*|-XX:*)
      JVM_ARGS+=("$1")
      shift
      ;;
    --)
      shift
      while [[ $# -gt 0 ]]; do
        APP_ARGS+=("$1")
        shift
      done
      ;;
    *)
      APP_ARGS+=("$1")
      shift
      ;;
  esac
done

if [[ ! -f "$JAR_PATH" ]]; then
  echo "[run-client] Missing client jar: $JAR_PATH" >&2
  echo "[run-client] Build it with: mvn -pl shared,client -am -DskipTests package" >&2
  exit 1
fi

SERVER_HOST="${GAME_SERVER_HOST:-localhost}"

declare -a CMD
CMD=(java)
if [[ "$(uname)" == "Darwin" ]]; then
  CMD+=(-XstartOnFirstThread)
fi
if [[ ${#JVM_ARGS[@]} -gt 0 ]]; then
  CMD+=("${JVM_ARGS[@]}")
fi
CMD+=(-DGAME_SERVER_HOST="$SERVER_HOST" -jar "$JAR_PATH")
if [[ ${#APP_ARGS[@]} -gt 0 ]]; then
  CMD+=("${APP_ARGS[@]}")
fi

echo "[run-client] Host: $SERVER_HOST"
echo "[run-client] Jar:  $JAR_PATH"

exec "${CMD[@]}"
