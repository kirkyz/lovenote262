#!/usr/bin/env bash
set -euo pipefail
BACK="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$BACK/../.." && pwd)"
cd "$ROOT"
if ! sha256sum -c "$BACK/installed.sha256"; then
  echo 'STOP: Files were edited since V11 installation. Refusing unsafe rollback.' >&2; exit 2
fi
while IFS= read -r f; do
  case "$f" in
    src/main/java/com/lovenote/module/ModuleManager.java|src/main/java/com/lovenote/module/impl/ported/DrainerServerMacros.java)
      cp -p "$BACK/originals/$f" "$f" ;;
    *) rm -f "$f" ;;
  esac
done < "$BACK/changed-files.txt"
echo 'V11 reverted. Build again to restore the previous JAR.'
