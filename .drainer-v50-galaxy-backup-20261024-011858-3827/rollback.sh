#!/usr/bin/env bash
set -euo pipefail
for REL in 'src/main/java/com/lovenote/module/impl/SkyShader.java' 'src/main/java/com/lovenote/util/NativeNebulaRenderer.java' 'src/main/resources/assets/lovenote262/shaders/native_nebula.frag'; do
  cp -p '/c/Users/Lkirk/Desktop/new lovenote mods/.drainer-v50-galaxy-backup-20261024-011858-3827/'"$REL" '/c/Users/Lkirk/Desktop/new lovenote mods/'"$REL"
done
echo 'Reverted Drainer V50 Galaxy changes.'
