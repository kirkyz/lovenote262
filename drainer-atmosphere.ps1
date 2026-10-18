$ErrorActionPreference = "Stop"

$p = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$s = [IO.File]::ReadAllText($p)

$callAnchor = '        ctx.fill(0, 0, width, height, LovenotePalette.alpha(0x000000, (int) (130 * eo)));'
$methodAnchor = '    private void addRipple(int x, int y) {'

if (!$s.Contains($callAnchor)) {
    throw "Background insertion point not found. No changes made."
}
if (!$s.Contains($methodAnchor)) {
    throw "Helper insertion point not found. No changes made."
}
if ($s.Contains('private void drawDrainerAtmosphere(')) {
    throw "Atmosphere helper already exists. No changes made."
}

Copy-Item $p "$p.before-drainer-atmosphere" -Force

$call = @'
        drawDrainerAtmosphere(ctx, eo);
'@

$method = @'
    // Drainer ClickGUI-only ambience. Drawn behind particles and window controls.
    private void drawDrainerAtmosphere(GuiGraphicsExtractor ctx, float openAlpha) {
        float time = (System.nanoTime() % 20000000000L) / 1_000_000_000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.8f);
        int w = width;
        int h = height;

        // Restrained violet glow along the outer screen edges.
        for (int i = 0; i < 9; i++) {
            float fade = (1f - i / 9f) * (0.35f + pulse * 0.25f) * openAlpha;
            int alpha = Math.min(24, Math.max(0, (int) (fade * 30)));
            if (alpha == 0) continue;
            int col = LovenotePalette.alpha(LovenotePalette.C2, alpha);
            ctx.fill(i, 0, i + 1, h, col);
            ctx.fill(w - i - 1, 0, w - i, h, col);
            if (i < 5) {
                ctx.fill(0, i, w, i + 1, LovenotePalette.alpha(LovenotePalette.C1, alpha));
            }
        }

        // Tiny moving glints on the top perimeter, away from the logo/search area.
        for (int i = 0; i < 7; i++) {
            float phase = (time * 0.055f + i * 0.173f) % 1f;
            int x = (int) (phase * Math.max(1, w));
            int y = 5 + (i % 3) * 3;
            int alpha = (int) ((35 + 45 * pulse) * openAlpha);
            int c = LovenotePalette.alpha(LovenotePalette.C3, alpha);
            ctx.fill(x, y, x + 2, y + 3, c);
            ctx.fill(Math.max(0, x - 2), y + 1, x + 4, y + 2,
                    LovenotePalette.alpha(LovenotePalette.C2, alpha / 3));
        }
    }

'@

$s = $s.Replace($callAnchor, $callAnchor + "`r`n" + $call)
$s = $s.Replace($methodAnchor, $method + $methodAnchor)

[IO.File]::WriteAllText(
    $p,
    $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Drainer ClickGUI atmosphere added."
