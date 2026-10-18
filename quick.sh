#!/usr/bin/env bash
cd /workspaces/lovenote262
pkill -9 -f KnotClient 2>/dev/null
sleep 1
setsid nohup bash ./run-direct.sh < /dev/null > /tmp/run.log 2>&1 &
for i in $(seq 1 40); do
  W=$(DISPLAY=:99 xdotool search --name Minecraft 2>/dev/null | tail -1)
  [ -n "$W" ] && break
  sleep 2
done
echo "window ${W:-none}"
