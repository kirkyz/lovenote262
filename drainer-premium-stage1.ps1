$ErrorActionPreference = "Stop"

$root = "src/main/java/meteordevelopment/meteorclient/gui"
$config = "$root/themes/lovenote/LovenoteMenuConfig.java"
$screen = "$root/screens/LovenoteModulesScreen.java"

if (!(Test-Path $config) -or !(Test-Path $screen)) {
    throw "Cannot find ClickGUI source files."
}

$c = [System.IO.File]::ReadAllText((Resolve-Path $config))
$s = [System.IO.File]::ReadAllText((Resolve-Path $screen))

if ($c.Contains('public static String style = "Premium";')) {
    Write-Host "Premium Stage 1 already applied."
    exit 0
}

$changes = @(
    @('public static int menuScale = 65;', 'public static int menuScale = 75;'),
    @('public static int windowWidth = 600;', 'public static int windowWidth = 760;'),
    @('public static int windowHeight = 372;', 'public static int windowHeight = 430;'),
    @('public static int corner = 10;', 'public static int corner = 12;'),
    @('public static int railWidth = 118;', 'public static int railWidth = 132;'),
    @('public static int columns = 2;', 'public static int columns = 1;'),
    @('public static String style = "Classic";', 'public static String style = "Premium";'),
    @('public static int glow = 5;', 'public static int glow = 7;')
)

foreach ($change in $changes) {
    if (!$c.Contains($change[0])) {
        throw "Expected config line missing: $($change[0])"
    }
    $c = $c.Replace($change[0], $change[1])
}

$oldStyle = 'int styleA = LovenoteMenuConfig.style.equals("Classic") ? 0xF8 : LovenoteMenuConfig.style.equals("Liquid") ? 0xCC : 0x8C;'
$newStyle = 'int styleA = LovenoteMenuConfig.style.equals("Premium") ? 0xE8 : LovenoteMenuConfig.style.equals("Classic") ? 0xF8 : LovenoteMenuConfig.style.equals("Liquid") ? 0xCC : 0x8C;'

if (!$s.Contains($oldStyle)) {
    throw "Expected window style renderer missing."
}

$s = $s.Replace($oldStyle, $newStyle)

$anchor = 'rr(ctx, wx, wy, winW, winH, corner, LovenotePalette.alpha(LovenotePalette.bg(), alpha));'

$replacement = @'
rr(ctx, wx, wy, winW, winH, corner, LovenotePalette.alpha(LovenotePalette.bg(), alpha));

        // Premium glass: restrained inner chrome rim and violet highlights.
        if (LovenoteMenuConfig.style.equals("Premium")) {
            rr(ctx, wx + 2, wy + 2, winW - 4, winH - 4,
                Math.max(4, corner - 2),
                LovenotePalette.alpha(LovenotePalette.C2, 18));

            rr(ctx, wx + 4, wy + 4, winW - 8, winH - 8,
                Math.max(4, corner - 4),
                LovenotePalette.alpha(LovenotePalette.bg(), 220));

            fill(ctx, wx + corner + 8, wy + 4,
                wx + winW - corner - 8, wy + 5,
                LovenotePalette.alpha(LovenotePalette.C3, 75));
        }
'@

if (!$s.Contains($anchor)) {
    throw "Expected window background renderer missing."
}

$s = $s.Replace($anchor, $replacement)

Copy-Item $config "$config.before-premium-stage1" -Force
Copy-Item $screen "$screen.before-premium-stage1" -Force

[System.IO.File]::WriteAllText((Resolve-Path $config), $c)
[System.IO.File]::WriteAllText((Resolve-Path $screen), $s)

Write-Host "Premium Stage 1 applied successfully."
Write-Host "Original files backed up."
