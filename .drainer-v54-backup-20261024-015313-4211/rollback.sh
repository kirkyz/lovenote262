#!/usr/bin/env bash
set -euo pipefail
BACKUP="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT="$(cd -- "$BACKUP/.." && pwd)"
for r in
  src/main/java/com/lovenote/module/impl/SkyShader.java
  src/main/java/com/lovenote/util/NativeMeteorSkyRenderer.java
  src/main/resources/assets/lovenote262/shaders/native_meteor.vert
  src/main/resources/assets/lovenote262/shaders/native_meteor.frag
do
 cp -p "$BACKUP/$r" "$PROJECT/$r"
done
echo 'V53 files restored.'
