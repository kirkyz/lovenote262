#!/usr/bin/env bash
set -euo pipefail
BACKUP="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$BACKUP/../.." && pwd)"
tar -xzf "$BACKUP/original-files.tar.gz" -C "$ROOT"
echo "Restored the four original files. Rebuild the client to verify."
