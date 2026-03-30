#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/client/target/osrs-client-0.1.0-SNAPSHOT.jar"
# Server host — defaults to localhost. Set GAME_SERVER_HOST env var to connect to Azure.
# Example: GAME_SERVER_HOST=20.x.x.x ./run-client.sh
HOST_FLAG=""
if [[ -n "$GAME_SERVER_HOST" ]]; then
  HOST_FLAG="-DGAME_SERVER_HOST=$GAME_SERVER_HOST"
fi

if [[ "$(uname)" == "Darwin" ]]; then
  exec java -XstartOnFirstThread $HOST_FLAG -jar "$JAR" "$@"
else
  exec java $HOST_FLAG -jar "$JAR" "$@"
fi
