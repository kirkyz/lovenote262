#!/usr/bin/env bash
set -euo pipefail
BACKUP="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT="$(cd "$BACKUP/../.." && pwd)"
cd "$PROJECT"
if [[ -d "$BACKUP/originals" ]];then
  (cd "$BACKUP/originals";find . -type f -print0) | while IFS= read -r -d '' p;do
     rel="${p#./}";mkdir -p "$(dirname "$rel")";cp -p "$BACKUP/originals/$rel" "$rel"
  done
fi
if [[ -f "$BACKUP/new-files.txt" ]];then while IFS= read -r f;do rm -f "$f";done < "$BACKUP/new-files.txt";fi
echo 'V12 backup restored.'
