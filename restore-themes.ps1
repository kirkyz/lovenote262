$ErrorActionPreference = "Stop"

$path = "src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java"
$full = Join-Path (Get-Location).Path $path
$backup = "$full.before-modern-cards"

if (-not (Test-Path $backup)) {
    throw "Backup not found. No changes made."
}

$current = [System.IO.File]::ReadAllText($full)
$original = [System.IO.File]::ReadAllText($backup)

$marker = "    private void drawThemePresets("
$endMarker = "    private String trim("

if ($current.Contains($marker)) {
    Write-Host "Themes method already exists. No changes needed."
    exit 0
}

$start = $original.IndexOf($marker)
$end = $original.IndexOf($endMarker, $start)

if ($start -lt 0 -or $end -lt 0) {
    throw "Could not locate the Themes method in the backup."
}

$method = $original.Substring($start, $end - $start)

$insertAt = $current.IndexOf($endMarker)

if ($insertAt -lt 0) {
    throw "Could not locate insertion point."
}

Copy-Item $full "$full.before-theme-restore" -Force

$current = $current.Insert($insertAt, $method)

[System.IO.File]::WriteAllText(
    $full,
    $current,
    [System.Text.UTF8Encoding]::new($false)
)

Write-Host "SUCCESS: Themes method restored."
