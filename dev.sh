#!/usr/bin/env bash
cd /workspaces/lovenote262
export DISPLAY=:99
export LIBGL_ALWAYS_SOFTWARE=1
export GALLIUM_DRIVER=llvmpipe
export MESA_GL_VERSION_OVERRIDE=4.6
export ALSOFT_DRIVERS=pulse

pgrep -x Xvfb >/dev/null || (setsid Xvfb :99 -screen 0 1280x720x24 -nolisten tcp >/tmp/xvfb.log 2>&1 &)
sleep 1
pgrep -x openbox >/dev/null || (setsid openbox >/tmp/openbox.log 2>&1 &)
pulseaudio --check 2>/dev/null || pulseaudio --start --exit-idle-time=-1
pactl list short sinks | grep -q lovenote || pactl load-module module-null-sink sink_name=lovenote
pactl set-default-sink lovenote

pkill -9 -f KnotClient 2>/dev/null; sleep 2
setsid nohup bash ./run-direct.sh < /dev/null > /tmp/run.log 2>&1 &
echo "launched - tail -f /tmp/run.log"
