#!/usr/bin/env bash
cd /workspaces/lovenote262
for i in $(seq 1 100); do
  W=$(DISPLAY=:99 xdotool search --name Minecraft 2>/dev/null | tail -1)
  if [ -n "$W" ] && pgrep -f KnotClient >/dev/null && grep -q 'joined the game' /tmp/run.log 2>/dev/null; then
    sleep 6
    DISPLAY=:99 xdotool windowfocus "$W"; sleep 2
    DISPLAY=:99 xdotool key t; sleep 2
    DISPLAY=:99 xdotool type --delay 40 '.lov toggle GroundFog'; sleep 1
    DISPLAY=:99 xdotool key Return; sleep 4
    python3 -c "import json;d=json.load(open('lovenote262/lovenote.json'));print('GroundFog enabled =', d['modules']['GroundFog']['enabled'])"
    exit 0
  fi
  sleep 3
done
echo 'never became ready'
