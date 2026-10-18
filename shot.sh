#!/usr/bin/env bash
N=$(date +%s)
DISPLAY=:99 import -window root /tmp/s$N.png
cp /tmp/s$N.png ./s$N.png
echo "open s$N.png"
