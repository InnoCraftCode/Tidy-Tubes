<#
.SYNOPSIS
    Copies a game's finished Play assets from the working kit (play-store\<Game>) into the
    deliverable folder store-assets\ and validates them there.
.DESCRIPTION
    Layout produced:
      store-assets\icon\play-store-icon.png
      store-assets\icon\launcher\mipmap-*\...            copies of the app's launcher icons
      store-assets\icon\icon-preview-sheet.png
      store-assets\feature-graphic\feature-graphic.png
      store-assets\screenshots\phone\*.png
      store-assets\screenshots\sources\                  the StoreCapture scenario + previous (pre-redesign) shots
      store-assets\promo-video\*.mp4 (+ storyboard, script, source\ frames notes)
      store-assets\play-store-listing\listing.json (+ the Word file when present)
      store-assets\validation-report\asset-validation.json
.EXAMPLE
    .\tools\store\export-store-assets.ps1 -Game sortpuzzle
#>
[CmdletBinding()]
param([Parameter(Mandatory)][string]$Game)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'

$root = Get-WorkspaceRoot
$gameDir = Resolve-GameProject $Game
$kit = Join-Path $root "play-store\$Game"
$out = Join-Path $root 'store-assets'

function Copy-Into([string]$from, [string]$toDir, [string]$newName) {
    if (-not (Test-Path $from)) { Write-Status WARN "not found: $from"; return }
    New-Item -ItemType Directory -Force $toDir | Out-Null
    $dest = if ($newName) { Join-Path $toDir $newName } else { Join-Path $toDir (Split-Path $from -Leaf) }
    Copy-Item $from $dest -Force
    Write-Status OK $dest.Replace($root + '\', '')
}

Copy-Into (Join-Path $kit 'graphics\icon-512.png') (Join-Path $out 'icon') 'play-store-icon.png'
Copy-Into (Join-Path $kit 'graphics\icon-preview-sheet.png') (Join-Path $out 'icon')
foreach ($d in @('mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi')) {
    foreach ($n in @('ic_launcher_foreground.png', 'ic_launcher.png', 'ic_launcher_round.png')) {
        Copy-Into (Join-Path $gameDir "android\src\main\res\mipmap-$d\$n") (Join-Path $out "icon\launcher\mipmap-$d")
    }
}
foreach ($n in @('ic_launcher_background.xml', 'ic_launcher_monochrome.xml')) {
    Copy-Into (Join-Path $gameDir "android\src\main\res\drawable\$n") (Join-Path $out 'icon\launcher\drawable')
}
Copy-Into (Join-Path $gameDir 'android\src\main\res\mipmap-anydpi-v26\ic_launcher.xml') (Join-Path $out 'icon\launcher\mipmap-anydpi-v26')

Copy-Into (Join-Path $kit 'graphics\feature-graphic-1024x500.png') (Join-Path $out 'feature-graphic') 'feature-graphic.png'

foreach ($device in @('phone', 'tablet-7', 'tablet-10')) {
    $src = Join-Path $kit "screenshots\$device"
    if (-not (Test-Path $src)) { continue }
    $dst = Join-Path $out "screenshots\$device"
    New-Item -ItemType Directory -Force $dst | Out-Null
    Get-ChildItem $dst -File -ErrorAction SilentlyContinue | Remove-Item -Force
    Get-ChildItem $src -File -Include *.png, *.jpg -Recurse | ForEach-Object { Copy-Into $_.FullName $dst }
}
Copy-Into (Join-Path $gameDir 'lwjgl3\src\main\java\com\ben\laama\sortpuzzle\lwjgl3\StoreCapture.java') (Join-Path $out 'screenshots\sources')

Get-ChildItem (Join-Path $kit 'video') -File -Include *.mp4 -Recurse -ErrorAction SilentlyContinue | ForEach-Object { Copy-Into $_.FullName (Join-Path $out 'promo-video') }
Copy-Into (Join-Path $kit 'graphics\video-background-1920x1080.png') (Join-Path $out 'promo-video\source')

Copy-Into (Join-Path $kit 'listing.json') (Join-Path $out 'play-store-listing')
Get-ChildItem $kit -File -Filter '*.docx' -ErrorAction SilentlyContinue | ForEach-Object { Copy-Into $_.FullName (Join-Path $out 'play-store-listing') }

& "$PSScriptRoot\validate-store-assets.ps1" -Game $Game -KitDir $out -ReportPath (Join-Path $out 'validation-report\asset-validation.json')
exit $LASTEXITCODE
