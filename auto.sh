#!/usr/bin/env bash
cd /workspaces/lovenote262
export DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe MESA_GL_VERSION_OVERRIDE=4.6 ALSOFT_DRIVERS=pulse

pkill -9 -f KnotClient 2>/dev/null; sleep 1
setsid nohup bash ./run-direct.sh < /dev/null > /tmp/run.log 2>&1 &

for i in $(seq 1 60); do
  W=$(xdotool search --name Minecraft 2>/dev/null | tail -1)
  [ -n "$W" ] && break
  sleep 2
done
[ -z "$W" ] && { echo "no window"; exit 1; }
echo "window $W"

sleep 45
xdotool windowfocus $W
eval $(xdotool getwindowgeometry --shell $W)
xdotool mousemove --window $W $((WIDTH/2)) $((HEIGHT*48/100)) click 1
sleep 5
xdotool mousemove --window $W $((WIDTH*50/100)) $((HEIGHT*28/100)) click 1
sleep 2
xdotool mousemove --window $W $((WIDTH*31/100)) $((HEIGHT*81/100)) click 1
echo "clicked into world"
