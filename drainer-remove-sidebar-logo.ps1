$ErrorActionPreference = "Stop"

$p = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$s = [IO.File]::ReadAllText($p)

$pattern = '(?s)\s*// Animated DRAINER branding\.\s*if \(LovenoteMenuConfig\.logo\)\s*\{\s*double logoAngle\s*=.*?com\.lovenote\.util\.Logos\.drawSmoothWide\(ctx,\s*sx\s*\+.*?logoAngle\);\s*\}'

$matches = [regex]::Matches($s, $pattern)

if ($matches.Count -ne 1) {
    throw "Expected one sidebar logo block, found $($matches.Count). Nothing changed."
}

Copy-Item $p "$p.before-sidebar-logo-removal" -Force

$s = [regex]::Replace($s, $pattern, "`n", 1)

[IO.File]::WriteAllText(
    $p, $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Sidebar logo removed."
