$ErrorActionPreference = "Stop"

$p = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$s = [IO.File]::ReadAllText($p)

$pattern = 'Logos\.drawSmoothWide\(ctx,\s*cx\s*\+\s*\(cw\s*-\s*\d+\)\s*/\s*2,\s*wy\s*\+\s*\d+,\s*\d+,\s*\d+,\s*headerLogoAngle\);'

$count = [regex]::Matches($s, $pattern).Count

if ($count -ne 1) {
    throw "Expected one animated centre logo, found $count. No changes made."
}

Copy-Item $p "$p.before-static-header" -Force

$replacement = 'com.lovenote.util.Logos.drawSmoothWide(ctx, cx + (cw - 270) / 2, wy + 2, 270, 90, 0.0);'

$s = [regex]::Replace($s, $pattern, $replacement, 1)

[IO.File]::WriteAllText(
    $p, $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Larger stationary DRAINER header installed."
