#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -jar "$DIR/server/target/osrs-server-0.1.0-SNAPSHOT.jar" "$@"
