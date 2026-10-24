#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT="${1:?pass project folder}"
cp "$HERE/src/main/java/com/lovenote/module/impl/SkyShader.java" "$PROJECT/src/main/java/com/lovenote/module/impl/SkyShader.java"
rm -f "$PROJECT/src/main/java/com/lovenote/util/NativeAuroraRenderer.java"
rm -f "$PROJECT/src/main/resources/assets/lovenote262/shaders/native_aurora.vert"
rm -f "$PROJECT/src/main/resources/assets/lovenote262/shaders/native_aurora.frag"
echo 'V45 source changes reverted.'
