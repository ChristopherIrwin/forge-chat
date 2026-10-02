#!/usr/bin/env bash
# ForgeChat direct-javac build (Gradle daemon cannot run in this sandbox).
set -euo pipefail
ROOT="$HOME/workspace/forge-chat"
DEPS="$HOME/workspace/.toolchains/paper-deps"
JAVAC="$HOME/workspace/.toolchains/jdk-25.0.4.1+1/bin/javac"
JAR="$HOME/workspace/.toolchains/jdk-25.0.4.1+1/bin/jar"
CP=$(ls "$DEPS"/*.jar | tr '\n' ':')
OUT="$ROOT/build"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/stage"
find "$ROOT/src/main/java" -name '*.java' > "$OUT/sources.txt"
$JAVAC -Werror -Xlint:deprecation -parameters -d "$OUT/classes" -cp "$CP" @"$OUT/sources.txt"
cp -r "$OUT/classes"/. "$OUT/stage"/
cp "$ROOT/src/main/resources/plugin.yml" "$OUT/stage/plugin.yml"
cp "$ROOT/src/main/resources/config.yml" "$OUT/stage/config.yml"
( cd "$OUT/stage" && $JAR --create --file "$ROOT/ForgeChat-1.0.0.jar" . )
echo "built $ROOT/ForgeChat-1.0.0.jar"
