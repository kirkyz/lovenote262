$ErrorActionPreference = "Stop"

$ui = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$mod = "src/main/resources/fabric.mod.json"

if (!(Test-Path $ui) -or !(Test-Path $mod)) {
    throw "Required files missing. Nothing changed."
}

$u = [IO.File]::ReadAllText($ui)
$m = [IO.File]::ReadAllText($mod)

if (!$u.Contains('String crumb = "lovenote";')) {
    throw "Breadcrumb marker not found. Nothing changed."
}

if (!$m.Contains('"name": "lovenote"')) {
    throw "Mod name marker not found. Nothing changed."
}

Copy-Item $ui "$ui.before-drainer-brand"
Copy-Item $mod "$mod.before-drainer-brand"

$u = $u.Replace('String crumb = "lovenote";', 'String crumb = "drainer";')
$u = $u.Replace('txt(ctx, "lovenote", cx, y,', 'txt(ctx, "drainer", cx, y,')
$u = $u.Replace('font.width("lovenote")', 'font.width("drainer")')

# The image is already the branding, so remove the extra DRAINER text beside it.
$u = [regex]::Replace(
    $u,
    '(?s)\s*if \(sb > 0\.05f\) \{\s*txtS\(ctx, "DRAINER",.*?\);\s*\}',
    ''
)

$m = $m.Replace('"name": "lovenote"', '"name": "Drainer"')

$utf8 = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($ui, $u, $utf8)
[IO.File]::WriteAllText($mod, $m, $utf8)

Write-Host "Stage 1 branding applied."
