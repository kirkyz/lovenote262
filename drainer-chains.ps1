$ErrorActionPreference = "Stop"

$p = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$s = [IO.File]::ReadAllText($p)

if (!$s.Contains('private void drawDrainerAtmosphere(')) {
    throw "Atmosphere method not found. Nothing changed."
}
if ($s.Contains('// Animated Drainer edge chains')) {
    throw "Chain effect already exists. Nothing changed."
}

$pattern = '(?s)(private void drawDrainerAtmosphere\(GuiGraphicsExtractor ctx, float openAlpha\).*?)(\r?\n    \}\r?\n    private void addRipple\(int x, int y\) \{)'

$count = [regex]::Matches($s, $pattern).Count
if ($count -ne 1) {
    throw "Could not safely locate the end of the atmosphere method ($count matches). Nothing changed."
}

$chains = @'

// Animated Drainer edge chains. Kept at the screen margins.
        float swayTime = time * 1.15f;
        int linkCount = Math.max(4, Math.min(18, h / 24));
        int startY = Math.max(38, (h - linkCount * 15) / 2);

        for (int side = 0; side < 2; side++) {
            int baseX = side == 0 ? 8 : w - 17;

            for (int i = 0; i < linkCount; i++) {
                float phase = swayTime + i * 0.28f + side * 1.7f;
                int sway = Math.round((float) Math.sin(phase) * 2.0f);
                int lx = baseX + sway;
                int ly = startY + i * 15;
                int alpha = Math.max(0, Math.min(255, (int) (openAlpha * (95 + pulse * 45))));

                int metal = LovenotePalette.alpha(0xFF32234A, alpha);
                int shade = LovenotePalette.alpha(0xFF100C19, alpha);
                int shine = LovenotePalette.alpha(LovenotePalette.C3, alpha / 2);

                // Each hollow rectangular link is offset to interlock with its neighbour.
                int offset = (i & 1) == 0 ? 0 : 2;
                ctx.fill(lx + offset, ly, lx + offset + 7, ly + 11, metal);
                ctx.fill(lx + offset + 1, ly + 2, lx + offset + 6, ly + 9, shade);
                ctx.fill(lx + offset + 1, ly, lx + offset + 5, ly + 1, shine);
                ctx.fill(lx + offset, ly + 2, lx + offset + 1, ly + 8, shine);

                // Small travelling highlight.
                float glint = (float) Math.sin(time * 2.2f - i * 0.42f + side);
                if (glint > 0.82f) {
                    ctx.fill(lx + offset + 2, ly + 3, lx + offset + 5, ly + 5,
                            LovenotePalette.alpha(0xFFFFFFFF, (int) (alpha * 0.45f)));
                }
            }
        }
'@

$s = [regex]::Replace(
    $s,
    $pattern,
    [System.Text.RegularExpressions.MatchEvaluator]{
        param($m)
        return $m.Groups[1].Value + "`r`n" + $chains + $m.Groups[2].Value
    },
    1
)

Copy-Item $p "$p.before-drainer-chains" -Force

[IO.File]::WriteAllText(
    $p,
    $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Animated edge chains added to the ClickGUI."
