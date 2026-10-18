$ErrorActionPreference = "Stop"

$p = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$s = [IO.File]::ReadAllText($p)

$changes = @(
    @(
        'com.lovenote.util.Logos.drawSmoothWide(ctx, sx + (sbw - 100) / 2, wy + 9, 100, 38, logoAngle);',
        'com.lovenote.util.Logos.drawSmoothWide(ctx, sx + (sbw - 78) / 2, wy + 11, 78, 30, logoAngle);'
    ),
    @(
        '* Math.PI * 2.0 * 0.32;',
        '* Math.PI * 2.0 * 0.18;'
    ),
    @(
        '* Math.PI * 2.0 * 0.45;',
        '* Math.PI * 2.0 * 0.18;'
    )
)

foreach ($change in $changes) {
    if (!$s.Contains($change[0])) {
        throw "Expected code not found: $($change[0]). Nothing changed."
    }
}

Copy-Item $p "$p.before-logo-polish" -Force

foreach ($change in $changes) {
    $s = $s.Replace($change[0], $change[1])
}

[IO.File]::WriteAllText(
    $p, $s,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host "SUCCESS: Logo sizing and animation speed updated."
