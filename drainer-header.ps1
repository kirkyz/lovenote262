$ErrorActionPreference = "Stop"

$p = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$s = [IO.File]::ReadAllText($p)

$start = $s.IndexOf("    private void drawWindow(")
$end = $s.IndexOf("    private void drawFooter(", $start)

if ($start -lt 0 -or $end -lt 0) {
    throw "Cannot locate drawWindow method. No changes made."
}

$before = $s.Substring(0, $start)
$window = $s.Substring($start, $end - $start)
$after = $s.Substring($end)

function Change-Once([string]$old, [string]$new) {
    $count = ([regex]::Matches(
        $script:window,
        [regex]::Escape($old)
    )).Count

    if ($count -lt 1) {
        throw "Expected match not found for: $old. No changes saved."
    }

    $script:window = $script:window.Replace($old, $new)
}

# Increase header height and move content down.
Change-Once 'int hdr = wy + 36;' 'int hdr = wy + 107;'
Change-Once 'int titleY = wy + 46 +' 'int titleY = wy + 116 +'
Change-Once 'int top = wy + 66, bottom = ftr - 6;' 'int top = wy + 136, bottom = ftr - 6;'

# Move the entire search bar and its clickable area.
Change-Once 'rr(ctx, cx, wy + 9, sw, 18, 9,' 'rr(ctx, cx, wy + 78, sw, 18, 9,'
Change-Once 'circle(ctx, cx + 11, wy + 18, 4,' 'circle(ctx, cx + 11, wy + 87, 4,'
Change-Once 'circle(ctx, cx + 11, wy + 18, 2,' 'circle(ctx, cx + 11, wy + 87, 2,'
Change-Once 'fill(ctx, cx + 14, wy + 21, cx + 17, wy + 22,' 'fill(ctx, cx + 14, wy + 90, cx + 17, wy + 91,'
Change-Once 'addHit(cx, wy + 9, sw, 18, K_SEARCH,' 'addHit(cx, wy + 78, sw, 18, K_SEARCH,'
Change-Once 'cx + 22, wy + 14,' 'cx + 22, wy + 83,'
Change-Once 'fill(ctx, caretX, wy + 13, caretX + 1, wy + 23,' 'fill(ctx, caretX, wy + 82, caretX + 1, wy + 92,'

# Breadcrumb beside search.
Change-Once 'crumbX, wy + 14,' 'crumbX, wy + 83,'

# Add animated centred logo.
$anchor = '        int sw = Math.min(200, cw / 2);'

$logo = @'
        // DRAINER: centred animated logo, above the search bar.
        if (LovenoteMenuConfig.logo) {
            double headerLogoAngle = System.nanoTime() / 1000000000.0
                * Math.PI * 2.0 * 0.32;

            com.lovenote.util.Logos.draw(
                ctx,
                cx + (cw - 76) / 2,
                wy + 1,
                76,
                headerLogoAngle,
                0
            );
        }

'@

Change-Once $anchor ($logo + "`n" + $anchor)

# Save only after every expected replacement succeeded.
Copy-Item $p "$p.before-drainer-header" -Force

$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText(
    $p,
    $before + $window + $after,
    $utf8
)

Write-Host "SUCCESS: Drainer header layout applied."
