#!/usr/bin/env bash
set -euo pipefail
BACK="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$BACK/../.." && pwd)"
cd "$ROOT"
cp "$BACK/ModuleManager.java" src/main/java/com/lovenote/module/ModuleManager.java
PORT=src/main/java/com/lovenote/module/impl/ported/DrainerServerMacros.java
if [[ -f "$PORT" ]]; then
  echo "The added V10 source is still present: $PORT"
  echo 'Remove it manually if you no longer want it; your original manager was restored.'
fi
echo 'Original ModuleManager restored.'
