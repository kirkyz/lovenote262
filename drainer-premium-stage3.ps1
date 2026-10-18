# Drainer Premium Stage 3 - safe, exact-match source edits.
$ErrorActionPreference = 'Stop'
$target = Join-Path (Get-Location) 'src/main/java/meteordevelopment/meteorclient/gui/screens/LovenoteModulesScreen.java'
if (-not (Test-Path -LiteralPath $target)) { throw "Open Git Bash in the new lovenote mods folder first. File missing: $target" }
$utf8 = New-Object System.Text.UTF8Encoding($false)
$source = [System.IO.File]::ReadAllText($target)
$hadCRLF = $source.Contains("`r`n")
$source = $source.Replace("`r`n", "`n")
if ($source.Contains('DRainer_STAGE3_PREMIUM_POLISH')) { throw 'Stage 3 is already installed. No changes made.' }
if (-not $source.Contains('DRainer_STAGE2_THREE_COLUMN')) { throw 'Stage 2 layout marker missing. No changes made.' }
function FromB64([string]$s) { return [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($s)) }
function ExactReplace([string]$before, [string]$after) {
    $old = FromB64 $before
    $new = FromB64 $after
    $pos = $script:source.IndexOf($old, [StringComparison]::Ordinal)
    if ($pos -lt 0) { throw "Source doesn't match this Stage 3 patch at edit $script:editNumber. No file has been changed." }
    if ($script:source.IndexOf($old, $pos + $old.Length, [StringComparison]::Ordinal) -ge 0) { throw "Ambiguous match at edit $script:editNumber. No file has been changed." }
    $script:source = $script:source.Substring(0, $pos) + $new + $script:source.Substring($pos + $old.Length)
    $script:editNumber++
}
$editNumber = 1
ExactReplace 'cmFpbFcgPSBNdGguY2xhbXAoTG92ZW5vdGVNZW51Q29uZmlnLnJhaWxXaWR0aCwgOTIsIDE2OCk7' 'cmFpbFcgPSBNdGguY2xhbXAoTG92ZW5vdGVNZW51Q29uZmlnLnJhaWxXaWR0aCwgMTMyLCAxNjgpOw=='
ExactReplace 'aW50IGdhcCA9IDg7CiAgICAgICAgaW50IGNhcmRXID0gKGN3IC0gKG5jb2xzIC0gMSkgKiBnYXApIC8gbmNvbHM7CiAgICAgICAgaW50IGNhcmRIID0gY29tcGFjdCA/IDQ4IDogNjg7' 'aW50IGdhcCA9IDc7CiAgICAgICAgaW50IGNhcmRXID0gKGN3IC0gKG5jb2xzIC0gMSkgKiBnYXApIC8gbmNvbHM7CiAgICAgICAgaW50IGNhcmRIID0gY29tcGFjdCA/IDQyIDogNjg7'
ExactReplace 'aW50IHRpbGUgPSBjb21wYWN0ID8gMjMgOiAyODsKICAgICAgICAgICAgaW50IGl4ID0geCArIDEyOwogICAgICAgICAgICBpbnQgaXkgPSBseSArIDk7' 'aW50IHRpbGUgPSBjb21wYWN0ID8gMjAgOiAyODsKICAgICAgICAgICAgaW50IGl4ID0geCArIDEwOwogICAgICAgICAgICBpbnQgaXkgPSBseSArIDc7'
ExactReplace 'aW50IHRleHRYID0gaXggKyB0aWxlICsgOTsKICAgICAgICAgICAgaW50IHRleHRXID0gTWF0aC5tYXgoMTAsIGNhcmRXIC0gKHRleHRYIC0geCkgLSAxMik7' 'aW50IHRleHRYID0gaXggKyB0aWxlICsgOTsKICAgICAgICAgICAgaW50IHRleHRXID0gTWF0aC5tYXgoMTAsIGNhcmRXIC0gKHRleHRYIC0geCkgLSA1Mik7'
ExactReplace 'dHh0KGN0eCwgbmFtZSwgdGV4dFgsIGx5ICsgMTEs' 'dHh0KGN0eCwgbmFtZSwgdGV4dFgsIGx5ICsgOSw='
ExactReplace 'dHh0KGN0eCwgc3RhdHVzLCB4ICsgMTMsIGx5ICsgY2FyZEggLSAxNSw=' 'dHh0KGN0eCwgc3RhdHVzLCB0ZXh0WCwgbHkgKyBjYXJkSCAtIDE2LA=='
ExactReplace 'aW50IHN3aXRjaFggPSB4ICsgY2FyZFcgLSAzNTsKICAgICAgICAgICAgaW50IHN3aXRjaFkgPSBseSArIGNhcmRIIC0gMTg7' 'aW50IHN3aXRjaFggPSB4ICsgY2FyZFcgLSAzNTsKICAgICAgICAgICAgaW50IHN3aXRjaFkgPSBseSArIChjYXJkSCAtIDEwKSAvIDI7'
ExactReplace 'aW50IGF2YWlsID0gd2luSCAtIDU2IC0gNDA7CiAgICAgICAgaW50IHJvd0ggPSBNdGguY2xhbXAoYXZhaWwgLyBNYXRoLm1heCgxLCBwYWdlcygpKSwgMTYsIDI0KTs=' 'aW50IGF2YWlsID0gd2luSCAtIDU2IC0gNDA7CiAgICAgICAgaW50IHJvd0ggPSBNdGguY2xhbXAoYXZhaWwgLyBNYXRoLm1heCgxLCBwYWdlcygpKSwgMTgsIDI1KTs='
ExactReplace 'cnIoY3R4LCBzeCArIDUsIE1hdGgucm91bmQocGlsbFkpLCBzYncgLSAxMCwgcm93SCwgNiwgTG92ZW5vdGVQYWxldHRlLmFscGhhKExvdmVub3RlUGFsZXR0ZS5DMiwgKGludCkgKDcwICogc2IpKSk7' 'cnIoY3R4LCBzeCArIDUsIE1hdGgucm91bmQocGlsbFkpLCBzYncgLSAxMCwgcm93SCwgNiwgTG92ZW5vdGVQYWxldHRlLmFscGhhKExvdmVub3RlUGFsZXR0ZS5DMiwgNzApKTs='
ExactReplace 'cnIoY3R4LCBzeCArIDUsIE1hdGgucm91bmQocGlsbFkpICsgcm93SCAvIDIgLSA3LCAyLCAxNCwgMSwgTG92ZW5vdGVQYWxldHRlLmFscGhhKExvdmVub3RlUGFsZXR0ZS53YXZlKDBmKSwgKGludCkgKDI1NSAqIHNiKSkpOw==' 'cnIoY3R4LCBzeCArIDUsIE1hdGgucm91bmQocGlsbFkpICsgcm93SCAvIDIgLSA3LCAyLCAxNCwgMSwgTG92ZW5vdGVQYWxldHRlLmFscGhhKExvdmVub3RlUGFsZXR0ZS53YXZlKDBmKSwgMjMwKSk7'
ExactReplace 'aWYgKHRydWUpIHR4dChjdHgsIHRyaW0ocGFnZUxhYmVsKGkpLCBzYncgLSA0NyksIHN4ICsgNDIgKyAoaW50KSBzbGlkZSwgcnkgKyAocm93SCAtIDgpIC8gMiwKICAgICAgICAgICAgICAgICAgICBMb3Zlbm90ZVBhbGV0dGUuYWxwaGEoTG92ZW5vdGVQYWxldHRlLmxlcnAoTG92ZW5vdGVQYWxldHRlLmRpbSgpLCBMb3Zlbm90ZVBhbGV0dGUudGV4dCgpLCBNYXRoLm1heChodiAqIDAuN2YsIGkgPT0gcGFnZSA/IDFmIDogMGYpKSwgKGludCkgKDI1NSAqIHNiKSkpOw==' 'Ly8gU3RhZ2UgMzogcGVyc2lzdGVudCBjYXRlZ29yeSBsYWJlbHMgYW5kIGxpdmUgbW9kdWxlIGNvdW50cy4KICAgICAgICAgICAgU3RyaW5nIGxhYmVsID0gcGFnZUxhYmVsKGkpOwogICAgICAgICAgICBpbnQgY291bnQgPSAwOwogICAgICAgICAgICBpZiAoaSA8IGNhdHMuc2l6ZSgpKSBmb3IgKFJvdyBtb2R1bGUgOiBhbGxNb2R1bGVzKSBpZiAobW9kdWxlLmNhdEluZGV4KCkgPT0gaSkgY291bnQrKzsKICAgICAgICAgICAgU3RyaW5nIGNvdW50VGV4dCA9IGkgPCBjYXRzLnNpemUoKSA/IEludGVnZXIudG9TdHJpbmcoY291bnQpIDogIiI7CiAgICAgICAgICAgIGludCBsYWJlbFggPSBzeCArIDM1ICsgKGludCkgc2xpZGU7CiAgICAgICAgICAgIGludCBjb3VudFggPSBzeCArIHNidyAtIDExIC0gZm9udC53aWR0aChjb3VudFRleHQpOwogICAgICAgICAgICBpbnQgbGFiZWxXaWR0aCA9IE1hdGgubWF4KDE2LCBjb3VudFggLSBsYWJlbFggLSA1KTsKICAgICAgICAgICAgaW50IGxhYmVsQ29sb3IgPSBMb3Zlbm90ZVBhbGV0dGUubGVycChMb3Zlbm90ZVBhbGV0dGUuZGltKCksIExvdmVub3RlUGFsZXR0ZS50ZXh0KCksIE1hdGgubWF4KGh2ICogMC43ZiwgaSA9PSBwYWdlID8gMWYgOiAwZikpOwogICAgICAgICAgICB0eHQoY3R4LCB0cmltKGxhYmVsLCBsYWJlbFdpZHRoKSwgbGFiZWxYLCByeSArIChyb3dIIC0gOCkgLyAyLCBsYWJlbENvbG9yKTsKICAgICAgICAgICAgaWYgKCFjb3VudFRleHQuaXNFbXB0eSgpKSB0eHQoY3R4LCBjb3VudFRleHQsIGNvdW50WCwgcnkgKyAocm93SCAtIDgpIC8gMiwKICAgICAgICAgICAgICAgICAgICBMb3Zlbm90ZVBhbGV0dGUuYWxwaGEoTG92ZW5vdGVQYWxldHRlLmZhaW50KCksIGkgPT0gcGFnZSA/IDIxNSA6IDE0MCkpOw=='
ExactReplace 'Z3JhZChjdHgsIHN4ICsgMiwgd3kgKyA4LCBzeCArIHNidyAtIDIsIHd5ICsgOSwgTG92ZW5vdGVQYWxldHRlLmFscGhhKExvdmVub3RlUGFsZXR0ZS5DMSwgOTApLCBMb3Zlbm90ZVBhbGV0dGUuYWxwaGEoTG92ZW5vdGVQYWxldHRlLkMzLCA5MCkpOw==' 'Z3JhZChjdHgsIHN4ICsgMiwgd3kgKyA4LCBzeCArIHNidyAtIDIsIHd5ICsgOSwgTG92ZW5vdGVQYWxldHRlLmFscGhhKExvdmVub3RlUGFsZXR0ZS5DMSwgOTApLCBMb3Zlbm90ZVBhbGV0dGUuYWxwaGEoTG92ZW5vdGVQYWxldHRlLkMzLCA5MCkpOwogICAgICAgIGZpbGwoY3R4LCBMb3Zlbm90ZU1lbnVDb25maWcucmFpbFJpZ2h0ID8gc3ggOiBzeCArIHNidyAtIDEsIHd5ICsgMjAsCiAgICAgICAgICAgICAgICBMb3Zlbm90ZU1lbnVDb25maWcucmFpbFJpZ2h0ID8gc3ggKyAxIDogc3ggKyBzYncsIHd5ICsgd2luSCAtIDIwLAogICAgICAgICAgICAgICAgTG92ZW5vdGVQYWxldHRlLmFscGhhKExvdmVub3RlUGFsZXR0ZS5DMiwgNTUpKTs='
$source = $source.Replace('    // ---------------------------------------------------------------- render', '    // DRainer_STAGE3_PREMIUM_POLISH: compact cards and persistent sidebar labels.
    // ---------------------------------------------------------------- render'.Replace('\n', "`r`n"))
$backup = "$target.before-stage3-$(Get-Date -Format yyyyMMdd-HHmmss)"
Copy-Item -LiteralPath $target -Destination $backup -ErrorAction Stop
[System.IO.File]::WriteAllText($target, $source, $utf8)
Write-Host "Stage 3 applied successfully ($($editNumber - 1) checked edits)." -ForegroundColor Green
Write-Host "Backup: $backup"
Write-Host 'Now run: ./gradlew compileJava' 
