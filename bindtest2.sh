#!/usr/bin/env bash
cd /workspaces/lovenote262
for i in $(seq 1 100); do
  W=$(DISPLAY=:99 xdotool search --name Minecraft 2>/dev/null | tail -1)
  if [ -n "$W" ] && pgrep -f KnotClient >/dev/null && grep -q 'joined the game' /tmp/run.log 2>/dev/null; then
    echo "ready at ~$((i*3))s W=$W"
    sleep 5
    DISPLAY=:99 xdotool windowfocus "$W"
    sleep 2
    for k in 1 2; do
      DISPLAY=:99 xdotool keydown g; sleep 1; DISPLAY=:99 xdotool keyup g
      sleep 6
      python3 -c "import json;d=json.load(open('lovenote262/lovenote.json'));print('  press $k -> GroundFog enabled =', d['modules']['GroundFog']['enabled'])"
      pgrep -f KnotClient >/dev/null || { echo '  client died'; break; }
    done
    exit 0
  fi
  sleep 3
done
echo "never became ready"
