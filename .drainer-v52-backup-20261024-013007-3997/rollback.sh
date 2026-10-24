#!/usr/bin/env bash
set -euo pipefail
for r in 'src/main/java/com/lovenote/module/impl/SkyShader.java' 'src/main/java/com/lovenote/util/NativeMeteorSkyRenderer.java' 'src/main/resources/assets/lovenote262/shaders/native_meteor.vert' 'src/main/resources/assets/lovenote262/shaders/native_meteor.frag'; do
  if [[ -f '/c/Users/Lkirk/Desktop/new lovenote mods/.drainer-v52-backup-20261024-013007-3997/'"$r" ]]; then
    mkdir -p '/c/Users/Lkirk/Desktop/new lovenote mods/'"$(dirname "$r")"
    cp -p '/c/Users/Lkirk/Desktop/new lovenote mods/.drainer-v52-backup-20261024-013007-3997/'"$r" '/c/Users/Lkirk/Desktop/new lovenote mods/'"$r"
  fi
done
echo 'Rolled back Drainer V52.'
