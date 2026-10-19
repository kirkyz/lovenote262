#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT="${1:-$PWD}"
for file in  "src/main/java/com/lovenote/module/ModuleManager.java"  "src/main/java/com/lovenote/module/impl/Lyrics.java"  "src/main/java/com/lovenote/spotify/SpotifyManager.java"  "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"; do
 cp -p "$HERE/originals/$file" "$PROJECT/$file"
done
echo "V8.1 originals restored from $HERE"
