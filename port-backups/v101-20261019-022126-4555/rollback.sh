#!/usr/bin/env bash
set -euo pipefail
BACK="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$BACK/../.." && pwd)"
cd "$ROOT"
while IFS= read -r f; do
  if [[ -f "$BACK/originals/$f" ]]; then cp -p "$BACK/originals/$f" "$f"; fi
done < "$BACK/files.txt"
echo 'V10.1 source files restored. Rebuild your client to use the rollback.'
