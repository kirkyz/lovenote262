#!/usr/bin/env bash
cd /workspaces/lovenote262
for i in $(seq 1 80); do
  W=$(DISPLAY=:99 xdotool search --name Minecraft 2>/dev/null | tail -1)
  if [ -n "$W" ] && grep -q 'joined the game' /tmp/run.log 2>/dev/null; then
    sleep 3
    echo "ready at ~$((i*3))s W=$W"
    DISPLAY=:99 xdotool windowfocus "$W"
    sleep 1
    DISPLAY=:99 xdotool keydown g; sleep 1; DISPLAY=:99 xdotool keyup g
    sleep 2
    python3 -c "import json;d=json.load(open('lovenote262/lovenote.json'));print('GroundFog enabled =', d['modules']['GroundFog']['enabled'])"
    exit 0
  fi
  sleep 3
done
echo "never became ready"
