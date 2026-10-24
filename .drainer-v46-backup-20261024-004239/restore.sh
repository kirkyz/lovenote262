#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT="${1:?pass project path}"
cp "$HERE/src/main/java/com/lovenote/module/impl/SkyShader.java" "$PROJECT/src/main/java/com/lovenote/module/impl/SkyShader.java"
rm -f "$PROJECT/src/main/java/com/lovenote/util/CelestialGpuRenderer.java" "$PROJECT/src/main/resources/assets/lovenote262/shaders/celestial_studio.vert" "$PROJECT/src/main/resources/assets/lovenote262/shaders/celestial_studio.frag"
echo 'V46 reverted to V45 source.'
