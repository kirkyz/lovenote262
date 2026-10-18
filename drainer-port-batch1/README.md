# Drainer / Lovenote 26.2 — isolated port batch 1

**Scope:** 6 candidate replacements for registry-only placeholders. No Minecraft GUI screens, `ModuleManager`, settings base classes, keybinds, Gradle files or config loader are changed. These are Java source ports, **not** a working JAR or a guarantee of successful 26.2 gameplay testing.

Source examined: `kirkyz/lovenote` (old 1.20.1 files) and `kirkyz/lovenote262` (26.2 source), public `main` branch as viewed on 18 October 2026. The latest changes you are making locally are not available in those public snapshots.

## Candidate ports included

| Module | Port status | Behaviour / compromises |
|---|---|---|
| PingHud | source adapted; uncompiled | Smoothed latency from multiplayer player info, compatible HUD positioning |
| ServerInfo | source adapted; uncompiled | Server address, ping, tab-listed player count; uses the existing glass UI |
| DeathCoords | source adapted; uncompiled | Capture last health-to-zero transition, save/load coordinates, show HUD |
| Compass | source adapted; uncompiled | 15-degree compass ticks and headings with the existing UI renderer |
| ArmorHud | partial source adaptation; uncompiled | Percentage durability text display; old item icon rendering still TODO |
| PotionList | source adapted; uncompiled | Active potion effects and remaining duration readout |

All six deliberately replace `UnportedModule` stubs only. They **do not need registration**: the present `ModuleManager` already lists them. If you have since upgraded one of these classes, the installer leaves it untouched.

## Later: install without clobbering your active UI work

Extract this ZIP to an ordinary folder. In Git Bash, run (change the first path to the extracted folder):

```sh
bash "$HOME/Downloads/drainer-port-batch1/install.sh" "$HOME/Desktop/new lovenote mods"
```

`install.sh` checks the project tree and only overwrites exact placeholder classes (`extends UnportedModule`). Existing non-placeholder implementations are skipped. The replaced stubs are backed up under `port-backups/`.

Then build **on the computer where you already compile the client**:

```sh
cd "$HOME/Desktop/new lovenote mods"
./gradlew compileJava
```

If compilation fails, copy the **first actual `error:` lines** rather than only `BUILD FAILED`, so the exact Minecraft 26.2 API mismatch can be corrected. After compilation, test each module in a throwaway world/server that permits your modifications.

To generate a full outstanding-port CSV from the old Java reference files already in your current repo:

```sh
bash "$HOME/Downloads/drainer-port-batch1/audit.sh" "$HOME/Desktop/new lovenote mods"
```

## Why this is not the entire port

The new repository's own audit reports 163 legacy modules, including a large set of 26.2 registry-only placeholders. These six are independent, tractable ports. Combat, render/mixin, mining and fishing modules require more than changing imports; shared event, networking and rendering interfaces need adapting before those modules can be tested. Do not bulk-copy the old 1.20.1 `.java` files into 26.2's compiling source tree.

**No anti-cheat avoidance functionality included.** Automated gameplay may violate Hypixel rules; only use permitted automation on servers/worlds that allow it.

## Limitations

- The execution environment here does not have your complete build tree, Java 25 toolchain or external Gradle dependencies; these files have NOT been compiled against your updated client.
- Some original HUDs used icon rendering; the compact replacements use the new `Ui` primitives and Drainer theme.
- If `getPlayerInfo`, `getActiveEffects` or 26.2 rendering contracts have changed since the public files examined, the build must be adjusted on your machine.
- Your existing local UI changes are intentionally not merged into this patch. Keep a Git commit or copy before applying it.
