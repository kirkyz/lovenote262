#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo "== clearing stale jars =="
rm -f build/libs/*.jar

echo "== building =="
./gradlew build --console=plain

JAR=$(ls -1 build/libs/*.jar | grep -v -- '-sources' | head -1)
[ -n "$JAR" ] || { echo "no jar produced"; exit 1; }
echo "jar: $JAR"

DEST="${1:-}"
if [ -z "$DEST" ]; then
  for d in "$HOME/.minecraft/mods" "/workspaces/.minecraft/mods"; do
    [ -d "$d" ] && DEST="$d" && break
  done
fi
if [ -n "$DEST" ]; then
  cp "$JAR" "$DEST/"
  echo "copied $(basename "$JAR") -> $DEST"
else
  echo "mods folder not found here — grab $JAR and drop it in your mods folder"
fi
