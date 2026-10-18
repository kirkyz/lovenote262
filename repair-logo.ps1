$ErrorActionPreference = "Stop"

$hud = "src/main/java/com/lovenote/module/impl/LogoWidget.java"
$logo = "src/main/java/com/lovenote/util/Logos.java"
$menu = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"

foreach ($file in @($hud, $logo, $menu)) {
    if (-not (Test-Path $file)) {
        throw "Missing file: $file"
    }
}

$h = [IO.File]::ReadAllText($hud)
$l = [IO.File]::ReadAllText($logo)
$m = [IO.File]::ReadAllText($menu)

$oldHud = 'new NumberSetting("Logo size", 36, 6, 96, 1)'
$oldCrop = 'cropTop = 0.24f'

$startMarker = '        // DRAINER branding: readable at sidebar scale, no spinning image.'
$endMarker = '        String q = searchText.trim();'

if (-not $h.Contains($oldHud)) { throw "HUD marker not found. No changes made." }
if (-not $l.Contains($oldCrop)) { throw "Crop marker not found. No changes made." }

$start = $m.IndexOf($startMarker)
$end = $m.IndexOf($endMarker, $start)

if ($start -lt 0 -or $end -lt 0) {
    throw "Sidebar markers not found. No changes made."
}

$replacement = @'
        // Animated DRAINER branding.
        if (LovenoteMenuConfig.logo) {
            double logoAngle = System.nanoTime() / 1000000000.0
                * Math.PI * 2.0 * 0.45;

            com.lovenote.util.Logos.draw(
                ctx, sx + 7, wy + 8, 36, logoAngle, 0
            );
        }

        if (sb > 0.05f) {
            txtS(ctx, "DRAINER", sx + 43, wy + 20,
                LovenotePalette.alpha(
                    LovenotePalette.text(), (int) (255 * sb)
                ));
        }
'@

foreach ($file in @($hud, $logo, $menu)) {
    Copy-Item $file "$file.before-logo-repair" -Force
}

$h = $h.Replace($oldHud, 'new NumberSetting("Logo size", 64, 6, 128, 1)')
$l = $l.Replace($oldCrop, 'cropTop = 0f')
$m = $m.Substring(0, $start) + $replacement + "`r`n" + $m.Substring($end)

$utf8 = New-Object System.Text.UTF8Encoding($false)

[IO.File]::WriteAllText($hud, $h, $utf8)
[IO.File]::WriteAllText($logo, $l, $utf8)
[IO.File]::WriteAllText($menu, $m, $utf8)

Write-Host "Logo repair applied. Backups created."
