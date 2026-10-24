#!/usr/bin/env bash
set -euo pipefail
PROJECT=/c/Users/Lkirk/Desktop/new\ lovenote\ mods
BACKUP=/c/Users/Lkirk/Desktop/new\ lovenote\ mods/.drainer-v47-world-nebula-backup-20261024-005133-3471
cp -p "$BACKUP/src/main/java/com/lovenote/module/impl/SkyShader.java" "$PROJECT/src/main/java/com/lovenote/module/impl/SkyShader.java"
rm -f "$PROJECT/src/main/java/com/lovenote/util/NativeNebulaRenderer.java"
rm -f "$PROJECT/src/main/resources/assets/lovenote262/shaders/native_nebula.vert"
rm -f "$PROJECT/src/main/resources/assets/lovenote262/shaders/native_nebula.frag"
echo "V47 rolled back successfully."
