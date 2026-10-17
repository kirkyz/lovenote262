#!/usr/bin/env bash
export DISPLAY=:99
export LIBGL_ALWAYS_SOFTWARE=1
export ALSOFT_DRIVERS=pulse
cd /workspaces/lovenote262
trap "" TERM INT
exec /usr/local/sdkman/candidates/java/25.0.4+1-ms/bin/java -Dfabric.dli.config=/workspaces/lovenote262/.gradle/loom-cache/launch.cfg -Dfabric.dli.env=client -Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient @/workspaces/lovenote262/build/loom-cache/argFiles/runClient -XX:StackShadowPages=32 --sun-misc-unsafe-memory-access=allow --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -Duser.country -Duser.language=en -Duser.variant net.fabricmc.devlaunchinjector.Main --width 854 --height 480
