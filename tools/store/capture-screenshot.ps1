<#
.SYNOPSIS
    Captures the screen of the connected phone/emulator into play-store\<Game>\screenshots\phone\.
.DESCRIPTION
    Put the game on the screen you want (e.g. the hint pulsing), then run the script. Use the
    numbering of listing.json "screenshotCaptions" so the Word listing matches them up.
.EXAMPLE
    .\tools\store\capture-screenshot.ps1 -Game sortpuzzle -Name "02-niveau-en-cours"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [Parameter(Mandatory)][ValidatePattern('^[0-9]{2}[-_a-zA-Z0-9]*$')][string]$Name
)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'

$sdk = Find-AndroidSdk
if (-not $sdk) { Write-Status FAIL 'Android SDK not found'; exit 1 }
$adb = Join-Path $sdk 'platform-tools\adb.exe'
$devices = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' })
if (-not $devices.Count) { Write-Status FAIL 'No device or emulator connected' 'USB debugging on, or start an AVD (Android Studio > Device Manager).'; exit 1 }

$outDir = Join-Path (Get-WorkspaceRoot) "play-store\$Game\screenshots\phone"
New-Item -ItemType Directory -Force $outDir | Out-Null
$target = Join-Path $outDir "$Name.png"

# screencap to the device, then pull: piping binary PNG through PowerShell 5.1 corrupts it.
& $adb shell screencap -p /sdcard/store-capture.png
& $adb pull /sdcard/store-capture.png "$target" | Out-Null
& $adb shell rm /sdcard/store-capture.png

Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Image]::FromFile($target)
try { $w = $img.Width; $h = $img.Height } finally { $img.Dispose() }
Write-Status OK "$target  ${w}x${h}"
$long = [Math]::Max($w, $h); $short = [Math]::Min($w, $h)
if ($short -lt 320 -or $long -gt 3840) { Write-Status WARN 'Play requires 320-3840 px per side.' }
if ([Math]::Abs($long / $short - 16 / 9) -gt 0.02) {
    Write-Status WARN "Aspect $([Math]::Round($long / $short, 2)):1 is not 16:9. Play accepts it up to 2:1, but 1080x1920 looks best; use a 16:9 emulator (e.g. Pixel 2/3 profile) for store shots."
}
