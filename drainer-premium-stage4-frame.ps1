$ErrorActionPreference = 'Stop'
$path = 'src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java'
if (!(Test-Path -LiteralPath $path)) { throw "Run this script from your client project root. Missing: $path" }
$full = (Resolve-Path -LiteralPath $path).Path
$src = [System.IO.File]::ReadAllText($full)
$marker = 'DRAINER_STAGE4_CHROME_FRAME'
if ($src.Contains($marker)) { Write-Host 'Stage 4 frame is already installed; no changes made.'; exit 0 }
$anchor = '    private void drawWindow(GuiGraphicsExtractor ctx, int mx, int my, boolean interactive, int corner) {'
$callAnchor = '        if (LovenoteMenuConfig.headerLine) grad(ctx, wx + corner, wy, wx + winW - corner, wy + 1, LovenotePalette.alpha(LovenotePalette.C1, 150), LovenotePalette.alpha(LovenotePalette.C3, 150));'
if ($src.Split([string[]]@($anchor), [System.StringSplitOptions]::None).Count -ne 2) { throw 'Expected drawWindow method signature missing or duplicated; no changes made.' }
if ($src.Split([string[]]@($callAnchor), [System.StringSplitOptions]::None).Count -ne 2) { throw 'Expected headerLine rendering anchor missing or duplicated; no changes made.' }
$helper = @'
    // DRAINER_STAGE4_CHROME_FRAME: lightweight, time-based premium window trim.
    // This is decorative only and does not affect mouse hitboxes or layout.
    private void drawDrainerChromeFrame(GuiGraphicsExtractor ctx, int corner) {
        if (!LovenoteMenuConfig.style.equals("Premium")) return;

        final int x = wx, y = wy, w = winW, h = winH;
        if (w < 90 || h < 90) return;
        final float t = (System.nanoTime() % 30000000000L) / 1000000000f;
        final float breath = 0.5f + 0.5f * (float) Math.sin(t * 0.8f);
        final int edge = LovenotePalette.alpha(LovenotePalette.C2, 46 + (int) (16 * breath));
        final int silver = LovenotePalette.alpha(LovenotePalette.C3, 68);
        final int shadow = LovenotePalette.alpha(0x080510, 180);
        final int inset = Math.max(5, corner);

        // Double-line chrome border, inset so it doesn't overlap other screens.
        fill(ctx, x + inset, y + 2, x + w - inset, y + 3, silver);
        fill(ctx, x + inset, y + 3, x + w - inset, y + 4, shadow);
        fill(ctx, x + inset, y + h - 4, x + w - inset, y + h - 3, edge);
        fill(ctx, x + 2, y + inset, x + 3, y + h - inset, edge);
        fill(ctx, x + w - 3, y + inset, x + w - 2, y + h - inset, silver);

        // Restrained moving glint across the upper chrome line.
        int travel = Math.max(1, w - inset * 2 - 44);
        float sweep = (t * 0.055f) % 1f;
        int gx = x + inset + (int) (travel * sweep);
        for (int i = 0; i < 22; i++) {
            int fade = 7 + (int) (36 * (1f - Math.abs(i - 10.5f) / 11f));
            fill(ctx, gx + i, y + 2, gx + i + 1, y + 3,
                LovenotePalette.alpha(LovenotePalette.C3, fade));
        }

        // Small forged-metal corner accents, rather than rectangular chains.
        int len = Math.min(19, Math.min(w, h) / 8);
        int accent = LovenotePalette.alpha(LovenotePalette.C1, 70 + (int) (18 * breath));
        fill(ctx, x + inset, y + 5, x + inset + len, y + 6, accent);
        fill(ctx, x + 5, y + inset, x + 6, y + inset + len, accent);
        fill(ctx, x + w - inset - len, y + 5, x + w - inset, y + 6, accent);
        fill(ctx, x + w - 6, y + inset, x + w - 5, y + inset + len, accent);
        fill(ctx, x + inset, y + h - 6, x + inset + len, y + h - 5, accent);
        fill(ctx, x + 5, y + h - inset - len, x + 6, y + h - inset, accent);
        fill(ctx, x + w - inset - len, y + h - 6, x + w - inset, y + h - 5, accent);
        fill(ctx, x + w - 6, y + h - inset - len, x + w - 5, y + h - inset, accent);
    }

'@
$src = $src.Replace($anchor, $helper + $anchor)
$src = $src.Replace($callAnchor, '        drawDrainerChromeFrame(ctx, corner);' + "`r`n" + $callAnchor)
$backup = "$full.before-stage4-" + (Get-Date -Format 'yyyyMMdd-HHmmss')
Copy-Item -LiteralPath $full -Destination $backup
[System.IO.File]::WriteAllText($full, $src, (New-Object System.Text.UTF8Encoding($false)))
Write-Host 'Stage 4 chrome frame installed.'
Write-Host "Backup: $backup"
Write-Host 'Next: ./gradlew compileJava'
