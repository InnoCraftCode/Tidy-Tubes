<#
.SYNOPSIS
    Generates Play Store screenshots (and optionally the promo videos) from the desktop build,
    without a phone or emulator.
.DESCRIPTION
    Runs the game's ':lwjgl3:storeCapture' task (scripted, solver-played gameplay rendered at
    1080x1920), converts the screenshots to 24-bit PNG (Play rejects alpha), and with -Video
    renders 30 fps frames and encodes them with make-promo-video.ps1 (needs FFmpeg).
    The game needs a StoreCapture launcher - games\sortpuzzle has the reference one.
.EXAMPLE
    .\tools\store\capture-desktop.ps1 -Game sortpuzzle
    .\tools\store\capture-desktop.ps1 -Game sortpuzzle -Video
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [switch]$Video,
    # phone (1080x1920), tablet-7 (1200x1920) or tablet-10 (1600x2560): real Android resolutions,
    # rendered by the same scripted scenario; the game extends its viewport to the wider aspect.
    [ValidateSet('phone', 'tablet-7', 'tablet-10')][string]$Device = 'phone'
)
$sizes = @{ 'phone' = '1080x1920'; 'tablet-7' = '1200x1920'; 'tablet-10' = '1600x2560' }

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Get-WorkspaceRoot
$dir = Resolve-GameProject $Game
if ((Get-Content (Join-Path $dir 'lwjgl3\build.gradle.kts') -Raw) -notmatch 'storeCapture') {
    Write-Status FAIL "$Game has no :lwjgl3:storeCapture task" 'Copy StoreCapture.java + the task from games\sortpuzzle and adapt its script.'
    exit 1
}

# --- Screenshots ---------------------------------------------------------------------------
$code = Invoke-Gradle -ProjectDir $dir -Arguments @(':lwjgl3:storeCapture', "-PcaptureDevice=$Device", "-PcaptureSize=$($sizes[$Device])", '-q')
if ($code -ne 0) { Write-Status FAIL 'Screenshot capture failed'; exit $code }

$shotDir = Join-Path $root "play-store\$Game\screenshots\$Device"
foreach ($file in Get-ChildItem $shotDir -Filter *.png) {
    # Load through a memory stream so the file is not locked while we overwrite it.
    $bytes = [IO.File]::ReadAllBytes($file.FullName)
    $stream = New-Object IO.MemoryStream(, $bytes)
    $source = [System.Drawing.Image]::FromStream($stream)
    $flat = New-Object System.Drawing.Bitmap $source.Width, $source.Height, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($flat)
    $g.DrawImage($source, 0, 0, $source.Width, $source.Height)
    $g.Dispose(); $source.Dispose(); $stream.Dispose()
    $flat.Save($file.FullName, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Status OK ("{0}  {1}x{2} 24-bit" -f $file.Name, $flat.Width, $flat.Height)
    $flat.Dispose()
}

# --- Promo video ---------------------------------------------------------------------------
if ($Video) {
    $code = Invoke-Gradle -ProjectDir $dir -Arguments @(':lwjgl3:storeCapture', '-PcaptureMode=video', '-q')
    if ($code -ne 0) { Write-Status FAIL 'Video frame capture failed'; exit $code }
    & "$PSScriptRoot\make-promo-video.ps1" -Game $Game
}

Write-Status INFO "Regenerate the Word listing: .\tools\store\build-listing-doc.ps1 -Game $Game"
