#!/bin/bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_PATH="$DIR/server/target/osrs-server-0.1.0-SNAPSHOT.jar"
ROOT_ENV_PATH="$DIR/.env.server.local"
SERVER_ENV_PATH="$DIR/server/.env.server.local"

usage() {
  cat <<'EOF'
Usage: ./run-server.sh [--world <worldId>] [jvm args]

World options:
  sandbox          Local/dev test world (default)
  main_world       Main live-world scaffold

Examples:
  ./run-server.sh
  ./run-server.sh --world sandbox
  ./run-server.sh --world main_world
  ./run-server.sh --world sandbox -Xmx2g
  ./run-server.sh -Derynfall.worldId=main_world

Notes:
  - This script places -Derynfall.worldId before -jar correctly.
  - If present, local env is sourced before launch from one of:
      server/.env.server.local
      .env.server.local
  - For local auth against the shared Azure auth/database stack, set at minimum:
      DB_PASSWORD
      JWT_SIGNING_KEY
      JWT_ISSUER
      JWT_AUDIENCE
  - If the server jar is missing, build it with:
      mvn -pl shared,server -am -DskipTests package
EOF
}

WORLD_ID="${ERYNFALL_WORLD_ID:-sandbox}"
declare -a JVM_ARGS
declare -a APP_ARGS

while [[ $# -gt 0 ]]; do
  case "$1" in
    --help|-h)
      usage
      exit 0
      ;;
    --world)
      if [[ $# -lt 2 ]]; then
        echo "[run-server] Missing value for --world" >&2
        usage >&2
        exit 1
      fi
      WORLD_ID="$2"
      shift 2
      ;;
    --world=*)
      WORLD_ID="${1#*=}"
      shift
      ;;
    -Derynfall.worldId=*)
      WORLD_ID="${1#*=}"
      shift
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

case "$WORLD_ID" in
  sandbox|main_world)
    ;;
  *)
    echo "[run-server] Unsupported world for normal local launch: $WORLD_ID" >&2
    usage >&2
    exit 1
    ;;
esac

if [[ ! -f "$JAR_PATH" ]]; then
  echo "[run-server] Missing server jar: $JAR_PATH" >&2
  echo "[run-server] Build it with: mvn -pl shared,server -am -DskipTests package" >&2
  exit 1
fi

LOCAL_ENV_PATH=""
if [[ -f "$SERVER_ENV_PATH" ]]; then
  LOCAL_ENV_PATH="$SERVER_ENV_PATH"
elif [[ -f "$ROOT_ENV_PATH" ]]; then
  LOCAL_ENV_PATH="$ROOT_ENV_PATH"
fi

if [[ -n "$LOCAL_ENV_PATH" ]]; then
  echo "[run-server] Loading local env: $LOCAL_ENV_PATH"
  set -a
  # shellcheck disable=SC1090
  source "$LOCAL_ENV_PATH"
  set +a
fi

echo "[run-server] World: $WORLD_ID"
echo "[run-server] Jar:   $JAR_PATH"

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "[run-server] WARN: DB_PASSWORD is not set. Azure DB login will fail and the server will fall back to in-memory mode." >&2
fi

if [[ -z "${JWT_SIGNING_KEY:-}" ]]; then
  echo "[run-server] WARN: JWT_SIGNING_KEY is not set. Token-auth login from the Azure auth service will be rejected by the local game server." >&2
fi

if [[ -z "${JWT_ISSUER:-}" || -z "${JWT_AUDIENCE:-}" ]]; then
  echo "[run-server] WARN: JWT_ISSUER and/or JWT_AUDIENCE are not set. Token verification may fail even if JWT_SIGNING_KEY is present." >&2
fi

declare -a CMD
CMD=(java)
if [[ ${#JVM_ARGS[@]} -gt 0 ]]; then
  CMD+=("${JVM_ARGS[@]}")
fi
CMD+=(-Derynfall.worldId="$WORLD_ID" -jar "$JAR_PATH")
if [[ ${#APP_ARGS[@]} -gt 0 ]]; then
  CMD+=("${APP_ARGS[@]}")
fi

exec "${CMD[@]}"
