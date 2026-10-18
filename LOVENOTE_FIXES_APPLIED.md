# Lovenote 26.2 — fixes applied (18 October 2026)

Based on the uploaded **all-modules-backbone** archive, not a checkout of the latest GitHub HEAD.

## Changes
- Removed constructor writes to `enabled = true` in `SkyShader`, `Interface`, `LogoWidget`, and `FunnyCrosshair`, so default activation can use `Module.setEnabled` and its callbacks.
- `ModuleManager` now calls `register(new SkyShader()).setEnabled(true)`; the other three defaults were already enabled via `setEnabled(true)`.
- Converted `Velocity` to an explicitly unported module while preserving its existing horizontal and vertical settings. Its toggle will not activate a misleading no-op feature. This is **not** a working knockback module.
- Marked `gradlew` executable for Unix systems.

## Deliberately not changed
- The 152 legacy placeholder modules remain inert. The Hypixel and Donut SMP placeholders still require behavior ports.
- Meteor modules and similarly named custom placeholders were not deleted or merged: feature overlap is not proof of an actual registry collision.
- Fabric API version remains `0.154.2+26.2`; changing it without checking availability would introduce a new build risk.
- Rendering and mixin implementations were not rewritten without a reproducible compiler/runtime failure.

## Verification and limits
- `./gradlew compileJava --offline --console=plain` was attempted; wrapper failed to fetch Gradle 9.6.1 due to unavailable DNS/network. This environment has Java 21, while the project targets Java 25.
- Therefore **zero compilation errors are not verified**.
- Use Java 25 and a network-enabled machine, then run `./gradlew clean build --stacktrace --console=plain`.
- Run the client in-game to verify HUD, sky, and mixin behavior.
