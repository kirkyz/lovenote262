# Lovenote 26.2 UI / legacy-widget pass

Source: prior anti-knockback ZIP and uploaded original lovenote-main ZIP. **Not synced to GitHub**.

## Applied
- Menu logo now clamps its size and coordinates to GUI bounds.
- Menu logo follows Interface logo style; fallback monogram is default because `assets/lovenote/textures/logo.png` is not present.
- LogoWidget clamps the whole widget to the GUI instead of letting the name/mark overflow.
- Ported CoordsHud, SpeedWidget and InfoHud from inert placeholders to working text-based 26.2 widgets with configurable positions and frames. They remain off by default.

## Still missing
- Most legacy widgets (InventoryWidget, KeybindsWidget, CpsHud, TargetHud, SpotifyHud, etc.) remain placeholders.
- Hypixel/Donut categories contain placeholders; AutoTip/AutoSell are functional but most old server features are not.
- 26.2 Java 25 compile and in-game GUI inspection have not been performed.
- The screenshot may show a second GUI overlay; verify whether the large mark is from LogoWidget, MenuFx, or another theme after running.

## Build
`chmod +x gradlew && ./gradlew clean build --stacktrace --console=plain` with Java 25.
