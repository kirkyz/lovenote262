#!/usr/bin/env bash
set -euo pipefail
PROJECT=/c/Users/Lkirk/Desktop/new\ lovenote\ mods
BACKUP=/c/Users/Lkirk/Desktop/new\ lovenote\ mods/.drainer-v48-backup-20261024-010356-3649
cp -p "$BACKUP/src/main/java/com/lovenote/module/impl/SkyShader.java" "$PROJECT/src/main/java/com/lovenote/module/impl/SkyShader.java"
cp -p "$BACKUP/src/main/java/com/lovenote/util/NativeNebulaRenderer.java" "$PROJECT/src/main/java/com/lovenote/util/NativeNebulaRenderer.java"
cp -p "$BACKUP/src/main/resources/assets/lovenote262/shaders/native_nebula.vert" "$PROJECT/src/main/resources/assets/lovenote262/shaders/native_nebula.vert"
cp -p "$BACKUP/src/main/resources/assets/lovenote262/shaders/native_nebula.frag" "$PROJECT/src/main/resources/assets/lovenote262/shaders/native_nebula.frag"
cp -p "$BACKUP/src/main/resources/assets/lovenote262/shaders/celestial_studio.frag" "$PROJECT/src/main/resources/assets/lovenote262/shaders/celestial_studio.frag"
echo "Drainer V48 rolled back."
