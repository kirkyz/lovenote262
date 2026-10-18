$ErrorActionPreference = "Stop"

$path = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$full = Join-Path (Get-Location).Path $path

$s = [System.IO.File]::ReadAllText($full)

$startMarker = "        int logoCx = sx + 23, logoCy = wy + 24;"
$endMarker = "        if (LovenoteMenuConfig.logo) {"

$start = $s.IndexOf($startMarker)
$end = $s.IndexOf($endMarker, $start)

if ($start -lt 0 -or $end -lt 0) {
    throw "Logo code not found. No changes made."
}

$lineEnd = $s.IndexOf("`n", $end)

if ($lineEnd -lt 0) {
    throw "Logo line ending not found."
}

$replacement = @'
        // DRAINER - compact Drain Chrome wordmark
        int logoCx = sx + 23;
        int logoCy = wy + 24;

        if (LovenoteMenuConfig.logo) {
            int bx = logoCx - 20;
            int by = logoCy - 11;

            rr(ctx, bx, by, 40, 22, 6, 0xFF181923);

            grad(ctx, bx + 4, by + 3, bx + 36, by + 4,
                0xFFE8E9F4, 0xFF9C9CFF);

            txt(ctx, "DRAINER",
                bx + 1, by + 8, 0xFFE4E5F2);
        }
'@

Copy-Item $full "$full.before-drainer-branding" -Force

$s = $s.Substring(0, $start) +
     $replacement +
     $s.Substring($lineEnd + 1)

[System.IO.File]::WriteAllText(
    $full,
    $s,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "SUCCESS: DRAINER branding installed."
