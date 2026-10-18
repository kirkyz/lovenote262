$ErrorActionPreference = "Stop"

$lp = "src/main/java/com/lovenote/util/Logos.java"
$up = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"

$l = [IO.File]::ReadAllText($lp)
$u = [IO.File]::ReadAllText($up)

$pattern = '(?s)public static void drawWide\(\s*GuiGraphicsExtractor g,.*?\n\s*\}\s*(?=public static boolean imageAvailable\(\))'

$replacement = @'
public static void drawWide(
                GuiGraphicsExtractor g,
                int x, int y,
                int width, int height,
                double angle
        ) {
                // Keep the original PNG colours and transparency.
                ensure(IMAGE_STYLE, 3);

                // Continuous projection, rather than 32 discrete frames.
                double face = Math.cos(angle);
                int projectedWidth = Math.max(1,
                        (int) Math.round(width * Math.abs(face)));

                int drawX = x + (width - projectedWidth) / 2;

                // Frame zero is the full, front-facing logo.
                Gfx.blit(
                        g, ID,
                        drawX, y,
                        0f, 0f,
                        projectedWidth, height,
                        IMAGE_W, IMAGE_H,
                        IMAGE_W * FRAMES, IMAGE_H
                );
        }

'@

if ([regex]::Matches($l, $pattern).Count -ne 1) {
    throw "Wide renderer not found. Nothing changed."
}

$sideOld = 'com.lovenote.util.Logos.drawWide(ctx, sx + 6, wy + 10, 90, 30, logoAngle);'
$sideNew = 'com.lovenote.util.Logos.drawWide(ctx, sx + (sbw - 100) / 2, wy + 9, 100, 38, logoAngle);'

if (!$u.Contains($sideOld)) {
    throw "Sidebar logo call not found. Nothing changed."
}

Copy-Item $lp "$lp.before-smooth-fix" -Force
Copy-Item $up "$up.before-smooth-fix" -Force

$l = [regex]::Replace($l, $pattern, [System.Text.RegularExpressions.MatchEvaluator]{
    param($m)
    return $replacement
})

# Preserve original colours and existing alpha channel.
$l = $l.Replace(
    'public static boolean keyBlack = true;',
    'public static boolean keyBlack = false;'
)
$l = $l.Replace(
    'public static int tintMode = 0;',
    'public static int tintMode = 3;'
)

$u = $u.Replace($sideOld, $sideNew)

# Centre logo: slightly wider and shorter to improve proportions.
$u = $u.Replace(
    'cx + (cw - 220) / 2, wy + 7, 220, 65, headerLogoAngle',
    'cx + (cw - 240) / 2, wy + 9, 240, 60, headerLogoAngle'
)

$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($lp, $l, $utf8)
[IO.File]::WriteAllText($up, $u, $utf8)

Write-Host "SUCCESS: Smooth Drainer logo patch applied."
