#!/bin/sh
# Generates an inventory of the old module names and current 26.2 status.
# Does not modify Java source files.
set -eu
PROJECT=${1:-"$PWD"}
TARGET="$PROJECT/src/main/java/com/lovenote/module/impl"
LEGACY="$PROJECT/legacy-1.21-reference"
if [ ! -d "$TARGET" ]; then
  echo "ERROR: Not a Drainer source tree: $PROJECT" >&2
  exit 2
fi
OUT="$PROJECT/drainer-port-audit.csv"
printf 'name,status,note\n' > "$OUT"
if [ ! -d "$LEGACY" ]; then
  echo "Legacy reference directory not found: $LEGACY" >&2
  echo 'The current repository may not include the original legacy source. No full count possible.'
  exit 3
fi
for ref in "$LEGACY"/*.java; do
  [ -f "$ref" ] || continue
  name=$(basename "$ref" .java)
  case "$name" in
    *Mixin|Module|ModuleManager|Category|*Setting|RenderUtil|Colors|Config|Fx|Gl|BlockUtil|HudWidget)
      continue ;;
  esac
  current="$TARGET/$name.java"
  if [ ! -f "$current" ]; then
    printf '%s,missing,"No 26.2 file; requires port and registration review"\n' "$name" >> "$OUT"
  elif grep -Eq 'extends[[:space:]]+UnportedModule' "$current"; then
    printf '%s,placeholder,"Visible but does nothing"\n' "$name" >> "$OUT"
  else
    printf '%s,implementation_present,"Review functionality and test; name match alone is not proof"\n' "$name" >> "$OUT"
  fi
done
printf 'Audit written: %s\n' "$OUT"
if command -v awk >/dev/null 2>&1; then
  awk -F, 'NR>1 {count[$2]++} END {for (s in count) print "  " s ": " count[s]}' "$OUT"
fi
