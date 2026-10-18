#!/usr/bin/env bash
set -euo pipefail
if [[ ! -f gradlew || ! -d src/main/java ]]; then
  echo "ERROR: Run this from your Minecraft client project root (folder containing gradlew)." >&2
  exit 1
fi
SOURCE="src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
ASSETS="src/main/resources/assets/lovenote/textures/gui/drainer"
if [[ ! -f "$SOURCE" ]]; then echo "ERROR: Missing $SOURCE" >&2; exit 1; fi
if ! grep -q 'DRAINER_STAGE4_CHROME_FRAME' "$SOURCE"; then
  echo "ERROR: Your local screen differs from the supplied Stage 4 version; no changes made." >&2; exit 1
fi
if grep -q 'DRAINER_STAGE5_ORNAMENTS' "$SOURCE"; then
  echo "Stage 5 already appears installed; stopping to avoid overwriting your changes." >&2; exit 1
fi
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP="stage5_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP" "$ASSETS"
cp "$SOURCE" "$BACKUP/LovenoteModulesScreen.java"
for f in chain_left.png chain_right.png crystal.png; do
  if [[ -f "$ASSETS/$f" ]]; then cp "$ASSETS/$f" "$BACKUP/$f"; fi
done
cp "$SCRIPT_DIR/$SOURCE" "$SOURCE"
cp "$SCRIPT_DIR/$ASSETS/"*.png "$ASSETS/"
echo "Backup saved: $BACKUP"
echo "Compiling Stage 5..."
if ./gradlew compileJava; then
  echo "Compile succeeded. Building mod JAR..."
  ./gradlew build
  echo "Stage 5 installed. Find your JAR under build/libs/."
else
  echo "Compile failed. Restoring original Java file (assets are harmless)." >&2
  cp "$BACKUP/LovenoteModulesScreen.java" "$SOURCE"
  exit 1
fi
