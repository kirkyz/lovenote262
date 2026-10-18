$path = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$s = [System.IO.File]::ReadAllText((Join-Path (Get-Location).Path $path))

$old = 'else txt(ctx, pageLabel(page) + " page", cx + 4, top + 8, LovenotePalette.faint());'

if (-not $s.Contains($old)) {
    Write-Host "ERROR: Placeholder not found. No changes made."
    exit 1
}

if ($s.Contains("drawThemePresets(")) {
    Write-Host "Themes method already exists. No changes made."
    exit 1
}

$replacement = @'
else if (page == cats.size() + 1) {
            drawThemePresets(ctx, cx, top, cw, bottom);
        } else {
            txt(ctx, pageLabel(page) + " page",
                cx + 4, top + 8, LovenotePalette.faint());
        }
'@

$method = @'

    private void drawThemePresets(GuiGraphicsExtractor ctx,
                                  int x, int top, int width, int bottom) {
        txt(ctx, "APPEARANCE", x + 8, top + 8, LovenotePalette.text());
        txt(ctx, "Choose your accent colour",
            x + 8, top + 25, LovenotePalette.dim());

        int cols = 7;
        int gap = 8;
        int size = Math.min(34, Math.max(18,
            (width - 16 - gap * (cols - 1)) / cols));

        int startX = x + 8;
        int startY = top + 50;

        ctx.enableScissor(x, top, x + width, bottom);

        for (int i = 0; i < LovenotePalette.PRESETS.length; i++) {
            int bx = startX + (i % cols) * (size + gap);
            int by = startY + (i / cols) * (size + gap);

            if (by + size > bottom) break;

            int color = 0xFF000000 | LovenotePalette.PRESETS[i][0];

            if (LovenoteMenuConfig.accentIndex == i) {
                rr(ctx, bx - 2, by - 2, size + 4, size + 4,
                    9, LovenotePalette.alpha(color, 140));
            }

            rr(ctx, bx, by, size, size, 8, color);
            addHit(bx, by, size, size, K_ACCENT, null, null, i);
        }

        ctx.disableScissor();
    }

'@

$anchor = "    private String trim("

if (-not $s.Contains($anchor)) {
    Write-Host "ERROR: Method insertion point not found."
    exit 1
}

Copy-Item $path "$path.before-themes" -Force

$s = $s.Replace($old, $replacement)
$s = $s.Replace($anchor, $method + $anchor)

[System.IO.File]::WriteAllText(
    (Join-Path (Get-Location).Path $path),
    $s,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "SUCCESS: Themes page added!"
