#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
echo 'Lovenote source inventory'
echo "Java files: $(find src/main/java -name '*.java' | wc -l)"
echo "Custom module source files: $(find src/main/java/com/lovenote/module/impl -maxdepth 1 -name '*.java' | wc -l)"
echo 'Registered custom modules:'
grep -E 'register\(new [A-Za-z0-9_]+\(' src/main/java/com/lovenote/module/ModuleManager.java || true
echo 'Checking required toolchain:'
java -version 2>&1 | head -1 || true
echo 'Building (requires Java 25 and Gradle dependencies):'
./gradlew clean build --console=plain 2>&1 | tee lovenote-build.log
