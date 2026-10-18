#!/usr/bin/env bash
set -euo pipefail
backup=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
project=$(cd -- "$backup/../.." && pwd)
dest="$project/src/main/java/com/lovenote/module/impl"
while read -r expected name; do
    file="$dest/$name"
    if [[ ! -f "$file" ]]; then printf 'SKIP %s: file missing\n' "$name"; continue; fi
    actual=$(sha256sum "$file" | awk '{print $1}')
    if [[ "$actual" != "$expected" ]]; then
        printf 'SKIP %s: changed since installation (do not overwrite recent edits)\n' "$name"; continue
    fi
    cp -- "$backup/originals/$name" "$file"
    printf 'RESTORE %s\n' "$name"
done < "$backup/applied-checksums.txt"
