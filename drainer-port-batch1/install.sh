#!/bin/sh
# Installs the candidate ports WITHOUT replacing already-implemented modules.
# Usage (Git Bash): bash /path/to/drainer-port-batch1/install.sh "$HOME/Desktop/new lovenote mods"
set -eu
SELF=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT=${1:-"$PWD"}
TARGET="$PROJECT/src/main/java/com/lovenote/module/impl"
SOURCE="$SELF/src/main/java/com/lovenote/module/impl"

if [ ! -f "$PROJECT/gradlew" ] || [ ! -f "$PROJECT/src/main/java/com/lovenote/module/Module.java" ]; then
  echo "ERROR: Not a Drainer/Lovenote 26.2 source root: $PROJECT" >&2
  echo 'Usage: bash install.sh "$HOME/Desktop/new lovenote mods"' >&2
  exit 2
fi

BACKUP="$PROJECT/port-backups/batch1-$(date +%Y%m%d-%H%M%S)"
installed=0
skipped=0
mkdir -p "$BACKUP"

for candidate in "$SOURCE"/*.java; do
  name=$(basename "$candidate")
  existing="$TARGET/$name"
  if [ ! -f "$existing" ]; then
    echo "SKIP: $name (no existing registered placeholder; avoiding accidental additions)"
    skipped=$((skipped + 1))
    continue
  fi
  if ! grep -Eq 'extends[[:space:]]+UnportedModule' "$existing"; then
    echo "SKIP: $name (already implemented or edited; preserving your changes)"
    skipped=$((skipped + 1))
    continue
  fi
  cp "$existing" "$BACKUP/$name"
  cp "$candidate" "$existing"
  echo "STAGED: $name (previous source backed up)"
  installed=$((installed + 1))
done
printf '\nStaged %s, skipped %s. Backups: %s\n' "$installed" "$skipped" "$BACKUP"
echo 'Compilation and in-game testing are STILL REQUIRED.'
echo 'Run: ./gradlew compileJava'
