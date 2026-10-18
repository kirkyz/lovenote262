$ErrorActionPreference = "Stop"

$logoPath = "src/main/java/com/lovenote/util/Logos.java"
$uiPath = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"

$logo = [IO.File]::ReadAllText($logoPath)
$ui = [IO.File]::ReadAllText($uiPath)

# Validate before changing anything.
if (!$logo.Contains("public static final int CELL = 128;")) {
    throw "Logo renderer differs from expected. Nothing changed."
}

if (!$ui.Contains("cx + (cw - 76) / 2")) {
    throw "Centre logo marker missing. Nothing changed."
}

if (!$ui.Contains("ctx, sx + 7, wy + 8, 36, logoAngle, 0")) {
    throw "Sidebar logo marker missing. Nothing changed."
}

Copy-Item $logoPath "$logoPath.before-wide-drainer"
Copy-Item $uiPath "$uiPath.before-wide-drainer"

# Increase atlas cell resolution so the logo is sharper.
$logo = $logo.Replace(
    "public static final int CELL = 128;",
    "public static final int CELL = 256;"
)

# Add a wide-image drawing overload.
$marker = "public static boolean imageAvailable()"

$method = @'
        public static void drawWide(
                GuiGraphicsExtractor g,
                int x, int y,
                int width, int height,
                double angle
        ) {
                ensure(IMAGE_STYLE, tintMode);

                int f = (int) Math.floor(
                        (angle / (Math.PI * 2)) * FRAMES
                ) % FRAMES;

                if (f < 0) f += FRAMES;

                Gfx.blit(
                        g, ID,
                        x, y,
                        f * (float) CELL, 0f,
                        width, height,
                        CELL, CELL,
                        CELL * FRAMES, CELL
                );
        }

'@

if (!$logo.Contains($marker)) {
    throw "Insertion marker missing. Nothing saved."
}

$logo = $logo.Replace($marker, $method + $marker)

# Update the centre logo.
$ui = $ui.Replace(
    "com.lovenote.util.Logos.draw(",
    "com.lovenote.util.Logos.draw("
)

# Replace the centre logo using whitespace-tolerant matching.
$centrePattern = '(?s)com\.lovenote\.util\.Logos\.draw\(\s*ctx,\s*cx\s*\+\s*\(cw\s*-\s*76\)\s*/\s*2,\s*wy\s*\+\s*1,\s*76,\s*headerLogoAngle,\s*0\s*\);'

if (![regex]::IsMatch($ui, $centrePattern)) {
    throw "Centre logo call not found. Nothing saved."
}

$ui = [regex]::Replace(
    $ui,
    $centrePattern,
    'com.lovenote.util.Logos.drawWide(ctx, cx + (cw - 220) / 2, wy + 7, 220, 65, headerLogoAngle);'
)

# Replace the sidebar logo using whitespace-tolerant matching.
$sidePattern = '(?s)com\.lovenote\.util\.Logos\.draw\(\s*ctx,\s*sx\s*\+\s*7,\s*wy\s*\+\s*8,\s*36,\s*logoAngle,\s*0\s*\);'

if (![regex]::IsMatch($ui, $sidePattern)) {
    throw "Sidebar logo call not found. Nothing saved."
}

$ui = [regex]::Replace(
    $ui,
    $sidePattern,
    'com.lovenote.util.Logos.drawWide(ctx, sx + 6, wy + 10, 90, 30, logoAngle);'
)
$utf8 = New-Object System.Text.UTF8Encoding($false)

[IO.File]::WriteAllText($logoPath, $logo, $utf8)
[IO.File]::WriteAllText($uiPath, $ui, $utf8)

Write-Host "SUCCESS: Wide DRAINER logo renderer installed."
