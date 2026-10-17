#!/usr/bin/env bash
cd /workspaces/lovenote262
while true; do
  ./quick.sh > /tmp/quick.log 2>&1
  while pgrep -f KnotClient >/dev/null; do
    sleep 12
    ./shot.sh > /dev/null 2>&1
    echo "$(date +%T) alive shot taken"
  done
  echo "$(date +%T) died -- relaunching"
  sleep 2
done
