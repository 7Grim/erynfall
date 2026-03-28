#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$DIR/client/target/osrs-client-0.1.0-SNAPSHOT.jar"
if [[ "$(uname)" == "Darwin" ]]; then
  exec java -XstartOnFirstThread -jar "$JAR" "$@"
else
  exec java -jar "$JAR" "$@"
fi
