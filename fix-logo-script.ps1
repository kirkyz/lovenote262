$ErrorActionPreference = "Stop"

$p = "drainer-logo-fix.ps1"
$s = [IO.File]::ReadAllText($p)

$start = $s.IndexOf('$oldCentre = @' + "'")
$end = $s.IndexOf('$utf8 = New-Object', $start)

if ($start -lt 0 -or $end -lt 0) {
    throw "Cannot locate replacement section. Script unchanged."
}

$replacement = @'
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

'@

Copy-Item $p "$p.before-regex-fix" -Force

$s = $s.Substring(0, $start) + $replacement + $s.Substring($end)

[IO.File]::WriteAllText(
    $p,
    $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Logo patch script repaired."
