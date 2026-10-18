$ErrorActionPreference = "Stop"

$p = "src/main/java/meteordevelopment/meteorclient/gui/themes/lovenote/LovenotePalette.java"

if (!(Test-Path $p)) {
    throw "Palette file missing."
}

$s = [IO.File]::ReadAllText($p)

# Match the palette declarations regardless of existing colours.
$changes = @(
    @{
        Pattern = '(?m)^(\s*public static int C1\s*=\s*)0x[0-9A-Fa-f]{8}(\s*,\s*C2\s*=\s*)0x[0-9A-Fa-f]{8}(\s*,\s*C3\s*=\s*)0x[0-9A-Fa-f]{8}'
        Replacement = '${1}0xFFB89AFF${2}0xFF8D65E8${3}0xFFE2D5FF'
    },
    @{
        Pattern = '(public static int bg\(\)\s*\{\s*return dark \?\s*)0x[0-9A-Fa-f]{6}'
        Replacement = '${1}0x0D0A16'
    },
    @{
        Pattern = '(public static int card\(\)\s*\{\s*return dark \?\s*)0x[0-9A-Fa-f]{8}'
        Replacement = '${1}0xFF171125'
    },
    @{
        Pattern = '(public static int cardHover\(\)\s*\{\s*return dark \?\s*)0x[0-9A-Fa-f]{8}'
        Replacement = '${1}0xFF241A38'
    },
    @{
        Pattern = '(public static int text\(\)\s*\{\s*return dark \?\s*)0x[0-9A-Fa-f]{8}'
        Replacement = '${1}0xFFF1EBFF'
    },
    @{
        Pattern = '(public static int dim\(\)\s*\{\s*return dark \?\s*)0x[0-9A-Fa-f]{8}'
        Replacement = '${1}0xFFA99BC3'
    },
    @{
        Pattern = '(public static int faint\(\)\s*\{\s*return dark \?\s*)0x[0-9A-Fa-f]{8}'
        Replacement = '${1}0xFF82749F'
    },
    @{
        Pattern = '(public static int track\(\)\s*\{\s*return dark \?\s*)0x[0-9A-Fa-f]{8}'
        Replacement = '${1}0xFF39294F'
    },
    @{
        Pattern = '(public static int chip\(\)\s*\{\s*return dark \?\s*)0x[0-9A-Fa-f]{8}'
        Replacement = '${1}0xFF2A203D'
    }
)

foreach ($change in $changes) {
    if (![regex]::IsMatch($s, $change.Pattern)) {
        throw "Palette structure differs from expected. Nothing changed."
    }
}

Copy-Item $p "$p.before-drainer-glass" -Force

foreach ($change in $changes) {
    $s = [regex]::Replace(
        $s,
        $change.Pattern,
        $change.Replacement
    )
}

$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($p, $s, $utf8)

Write-Host "SUCCESS: Drainer purple glass palette applied."
