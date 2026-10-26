<#
.SYNOPSIS
    Validates a game's Google Play assets against the official specs and writes a JSON report.
.DESCRIPTION
    Reads the real files (PNG/JPEG headers and pixels, not file names) and checks:
      icon            512x512, PNG, 32-bit RGBA, <= 1024 KB
      feature graphic 1024x500, PNG 24-bit or JPEG, no alpha channel
      screenshots     phone: 2-8 files, PNG 24-bit or JPEG, no alpha, 320..3840 px, long side <= 2x short,
                      featuring recommendation (>= 4 at >= 1080 px, 9:16 or 16:9) reported as WARNING,
                      alt text present in listing.json
      launcher icons  adaptive foreground + legacy PNGs at every density, monochrome vector present
    Content controls that need a human (readability, honesty, no badges) are listed as HUMAN_REVIEW,
    never auto-PASSed. Exit code 1 when any required file is missing or any check FAILs.
    Specs: https://support.google.com/googleplay/android-developer/answer/9866151 (read 2026-10-26).
.EXAMPLE
    .\tools\store\validate-store-assets.ps1 -Game sortpuzzle
    .\tools\store\validate-store-assets.ps1 -Game sortpuzzle -KitDir store-assets -ReportPath store-assets\validation-report\asset-validation.json
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [string]$KitDir,        # defaults to play-store\<Game>
    [string]$ReportPath     # defaults to <KitDir>\validation-report\asset-validation.json
)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Get-WorkspaceRoot
$gameDir = Resolve-GameProject $Game
if (-not $KitDir) { $KitDir = Join-Path $root "play-store\$Game" }
$KitDir = (Resolve-Path $KitDir).Path
if (-not $ReportPath) { $ReportPath = Join-Path $KitDir 'validation-report\asset-validation.json' }
$exported = (Split-Path $KitDir -Leaf) -eq 'store-assets'

$results = New-Object System.Collections.Generic.List[object]
$script:failed = $false

function Rel([string]$p) { return $p.Replace($root + '\', '') }

# Reads the real image format from the file header, plus alpha usage from the pixels.
function Get-ImageFacts([string]$path) {
    $bytes = [IO.File]::ReadAllBytes($path)
    $facts = @{ format = 'unknown'; bitDepth = $null; colorType = $null; hasAlphaChannel = $false; usesTransparency = $false; width = 0; height = 0; sizeBytes = $bytes.Length }
    if ($bytes.Length -gt 24 -and $bytes[0] -eq 0x89 -and $bytes[1] -eq 0x50 -and $bytes[2] -eq 0x4E -and $bytes[3] -eq 0x47) {
        $facts.format = 'PNG'
        $facts.width = [BitConverter]::ToInt32(@($bytes[19], $bytes[18], $bytes[17], $bytes[16]), 0)
        $facts.height = [BitConverter]::ToInt32(@($bytes[23], $bytes[22], $bytes[21], $bytes[20]), 0)
        $facts.bitDepth = [int]$bytes[24]
        $ct = [int]$bytes[25]
        $facts.colorType = $ct
        $facts.hasAlphaChannel = ($ct -eq 4 -or $ct -eq 6)   # grey+alpha, RGBA
        $facts.bitsPerPixel = switch ($ct) { 0 { $facts.bitDepth } 2 { 3 * $facts.bitDepth } 3 { $facts.bitDepth } 4 { 2 * $facts.bitDepth } 6 { 4 * $facts.bitDepth } }
    } elseif ($bytes.Length -gt 3 -and $bytes[0] -eq 0xFF -and $bytes[1] -eq 0xD8) {
        $facts.format = 'JPEG'
        $facts.bitsPerPixel = 24
    }
    $img = [System.Drawing.Bitmap]::FromFile($path)
    try {
        if ($facts.width -eq 0) { $facts.width = $img.Width; $facts.height = $img.Height }
        if ($facts.hasAlphaChannel) {
            # Scan the alpha plane once (LockBits is fast even for 1080x1920).
            $rect = New-Object System.Drawing.Rectangle 0, 0, $img.Width, $img.Height
            $data = $img.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
            $buffer = New-Object byte[] ($data.Stride * $img.Height)
            [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $buffer, 0, $buffer.Length)
            $img.UnlockBits($data)
            for ($i = 3; $i -lt $buffer.Length; $i += 4) { if ($buffer[$i] -ne 255) { $facts.usesTransparency = $true; break } }
        }
    } finally { $img.Dispose() }
    return $facts
}

function Add-Result([string]$file, [string]$type, $facts, [object[]]$checks, [string[]]$humanReview) {
    $status = 'PASS'
    foreach ($c in $checks) { if ($c.status -eq 'FAIL') { $status = 'FAIL'; $script:failed = $true } elseif ($c.status -eq 'WARNING' -and $status -ne 'FAIL') { $status = 'WARNING' } }
    $entry = [ordered]@{
        file = $file; assetType = $type; status = $status
        detectedFormat = $(if ($facts) { $facts.format } else { $null })
        bitsPerPixel = $(if ($facts) { $facts.bitsPerPixel } else { $null })
        width = $(if ($facts) { $facts.width } else { $null }); height = $(if ($facts) { $facts.height } else { $null })
        aspectRatio = $(if ($facts -and $facts.height) { [Math]::Round($facts.width / $facts.height, 4) } else { $null })
        sizeKB = $(if ($facts) { [Math]::Round($facts.sizeBytes / 1KB, 1) } else { $null })
        checks = $checks
        humanReview = $humanReview
    }
    $results.Add([pscustomobject]$entry)
    $level = @{ PASS = 'OK'; WARNING = 'WARN'; FAIL = 'FAIL' }[$status]
    Write-Status $level ("{0,-7} {1}" -f $type, $file)
    foreach ($c in $checks | Where-Object { $_.status -ne 'PASS' }) { Write-Host ("        {0}: {1} - {2}" -f $c.status, $c.name, $c.reason) -ForegroundColor DarkYellow }
}

function Check([string]$name, [bool]$ok, [string]$reason, [string]$failLevel = 'FAIL') {
    return [pscustomobject]@{ name = $name; status = $(if ($ok) { 'PASS' } else { $failLevel }); reason = $(if ($ok) { 'ok' } else { $reason }) }
}

function Missing([string]$file, [string]$type) {
    $script:failed = $true
    $results.Add([pscustomobject]([ordered]@{ file = $file; assetType = $type; status = 'FAIL'; checks = @([pscustomobject]@{ name = 'exists'; status = 'FAIL'; reason = 'required file missing' }) }))
    Write-Status FAIL ("{0,-7} {1} (missing)" -f $type, $file)
}

# ------------------------------------------------------------------ icon 512
$iconPath = if ($exported) { Join-Path $KitDir 'icon\play-store-icon.png' } else { Join-Path $KitDir 'graphics\icon-512.png' }
if (Test-Path $iconPath) {
    $f = Get-ImageFacts $iconPath
    $checks = @(
        (Check 'dimensions 512x512' ($f.width -eq 512 -and $f.height -eq 512) "got $($f.width)x$($f.height)"),
        (Check 'format PNG' ($f.format -eq 'PNG') "got $($f.format)"),
        (Check '32-bit RGBA (alpha channel present)' ($f.format -eq 'PNG' -and $f.colorType -eq 6 -and $f.bitDepth -eq 8) "colour type $($f.colorType), depth $($f.bitDepth)"),
        (Check 'size <= 1024 KB' ($f.sizeBytes -le 1024 * 1024) "$([Math]::Round($f.sizeBytes/1KB)) KB")
    )
    Add-Result (Rel $iconPath) 'icon' $f $checks @('Readable at 48 px; clear silhouette; no badges, ranking, price or award claims; no third-party marks')
} else { Missing (Rel $iconPath) 'icon' }

# ------------------------------------------------------------------ feature graphic
$featDir = if ($exported) { Join-Path $KitDir 'feature-graphic' } else { Join-Path $KitDir 'graphics' }
$featPath = @(Get-ChildItem $featDir -File -Include 'feature-graphic*.png', 'feature-graphic*.jpg', 'feature-graphic*.jpeg' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1)
if ($featPath.Count -gt 0) {
    $p = $featPath[0].FullName
    $f = Get-ImageFacts $p
    $checks = @(
        (Check 'dimensions 1024x500' ($f.width -eq 1024 -and $f.height -eq 500) "got $($f.width)x$($f.height)"),
        (Check 'format PNG or JPEG' ($f.format -in @('PNG', 'JPEG')) "got $($f.format)"),
        (Check '24-bit, no alpha channel' (($f.format -eq 'JPEG') -or ($f.colorType -eq 2 -and $f.bitDepth -eq 8)) "colour type $($f.colorType), depth $($f.bitDepth)"),
        (Check 'size <= 15 MB' ($f.sizeBytes -le 15MB) "$([Math]::Round($f.sizeBytes/1KB)) KB")
    )
    Add-Result (Rel $p) 'feature' $f $checks @('Key elements inside the central safe area; no exaggerated claims, badges or third-party marks; legible at small sizes')
} else { Missing (Rel (Join-Path $featDir 'feature-graphic-1024x500.png')) 'feature' }

# ------------------------------------------------------------------ screenshots (phone)
$listing = $null
$listingPath = if ($exported) { Join-Path $KitDir 'play-store-listing\listing.json' } else { Join-Path $KitDir 'listing.json' }
if (Test-Path $listingPath) { $listing = [IO.File]::ReadAllText($listingPath, [Text.Encoding]::UTF8) | ConvertFrom-Json }

foreach ($device in @('phone', 'tablet-7', 'tablet-10')) {
    $dir = Join-Path $KitDir "screenshots\$device"
    if (-not (Test-Path $dir)) { if ($device -eq 'phone') { Missing (Rel $dir) 'shot' }; continue }
    $shots = @(Get-ChildItem $dir -File -Include *.png, *.jpg, *.jpeg -Recurse | Sort-Object Name)
    $hiRes = 0
    foreach ($s in $shots) {
        $f = Get-ImageFacts $s.FullName
        $long = [Math]::Max($f.width, $f.height); $short = [Math]::Min($f.width, $f.height)
        $ratio = $long / $short
        $is916 = [Math]::Abs($ratio - 16 / 9) -lt 0.01
        if ($short -ge 1080 -and $is916) { $hiRes++ }
        $alt = $null
        if ($listing -and $listing.screenshots) { $alt = ($listing.screenshots | Where-Object { $_.file -eq $s.Name } | Select-Object -First 1).alt }
        $checks = @(
            (Check 'format PNG or JPEG' ($f.format -in @('PNG', 'JPEG')) "got $($f.format)"),
            (Check '24-bit, no alpha channel' (($f.format -eq 'JPEG') -or ($f.colorType -eq 2 -and $f.bitDepth -eq 8)) "colour type $($f.colorType), depth $($f.bitDepth)"),
            (Check 'each side 320..3840 px' ($short -ge 320 -and $long -le 3840) "$($f.width)x$($f.height)"),
            (Check 'long side <= 2x short side' ($ratio -le 2.0) ("ratio {0:N3}" -f $ratio)),
            (Check 'featuring: >= 1080 px and 16:9 / 9:16' ($short -ge 1080 -and $is916) "$($f.width)x$($f.height)" 'WARNING'),
            (Check 'alt text in listing.json' (-not [string]::IsNullOrWhiteSpace($alt)) 'no screenshots[].alt entry for this file' 'WARNING')
        )
        Add-Result (Rel $s.FullName) 'shot' $f $checks @('Shows the real app UI; one feature per image; no misleading overlay, badge or invented number')
    }
    if ($device -eq 'phone') {
        $countChecks = @(
            (Check 'phone screenshots: at least 2' ($shots.Count -ge 2) "found $($shots.Count)"),
            (Check 'phone screenshots: at most 8' ($shots.Count -le 8) "found $($shots.Count)"),
            (Check 'featuring eligibility: >= 4 at >= 1080 px 9:16' ($hiRes -ge 4) "found $hiRes" 'WARNING')
        )
        Add-Result (Rel $dir) 'shots' $null $countChecks @()
    }
}

# ------------------------------------------------------------------ launcher icons (app resources)
$res = Join-Path $gameDir 'android\src\main\res'
$launcherChecks = @()
foreach ($d in @{ mdpi = 1; hdpi = 1.5; xhdpi = 2; xxhdpi = 3; xxxhdpi = 4 }.GetEnumerator()) {
    $fgPath = Join-Path $res "mipmap-$($d.Key)\ic_launcher_foreground.png"
    $expected = [int](108 * $d.Value)
    if (Test-Path $fgPath) { $f = Get-ImageFacts $fgPath; $launcherChecks += Check "adaptive foreground $($d.Key) ${expected}px RGBA" ($f.width -eq $expected -and $f.height -eq $expected -and $f.colorType -eq 6) "got $($f.width)x$($f.height) type $($f.colorType)" }
    else { $launcherChecks += Check "adaptive foreground $($d.Key)" $false 'missing' }
    foreach ($legacy in @('ic_launcher', 'ic_launcher_round')) {
        $lp = Join-Path $res "mipmap-$($d.Key)\$legacy.png"
        $exp = [int](48 * $d.Value)
        if (Test-Path $lp) { $f = Get-ImageFacts $lp; $launcherChecks += Check "legacy $legacy $($d.Key) ${exp}px" ($f.width -eq $exp -and $f.height -eq $exp) "got $($f.width)x$($f.height)" }
        else { $launcherChecks += Check "legacy $legacy $($d.Key)" $false 'missing' }
        $webp = Join-Path $res "mipmap-$($d.Key)\$legacy.webp"
        $launcherChecks += Check "no duplicate $legacy.webp in $($d.Key)" (-not (Test-Path $webp)) 'old webp still present (duplicate resource)'
    }
}
$launcherChecks += Check 'adaptive icon xml references foreground/background/monochrome' ((Get-Content (Join-Path $res 'mipmap-anydpi-v26\ic_launcher.xml') -Raw) -match 'monochrome') 'monochrome layer missing'
$launcherChecks += Check 'monochrome vector exists' (Test-Path (Join-Path $res 'drawable\ic_launcher_monochrome.xml')) 'missing'
Add-Result (Rel $res) 'launch' $null $launcherChecks @('Adaptive foreground artwork stays inside the 66 dp safe zone (verify on the preview sheet / a device)')

# ------------------------------------------------------------------ report
$report = [ordered]@{
    game = $Game; kit = (Rel $KitDir); generated = (Get-Date).ToString('s')
    specSource = 'https://support.google.com/googleplay/android-developer/answer/9866151 (read 2026-10-26)'
    overall = $(if ($script:failed) { 'FAIL' } elseif ($results | Where-Object { $_.status -eq 'WARNING' }) { 'WARNING' } else { 'PASS' })
    note = 'Technical validation only. Entries under humanReview need a person; they are never marked PASS by this script.'
    assets = $results
}
New-Item -ItemType Directory -Force (Split-Path $ReportPath -Parent) | Out-Null
[IO.File]::WriteAllText($ReportPath, ($report | ConvertTo-Json -Depth 6), (New-Object Text.UTF8Encoding $false))
Write-Status INFO "Report: $(Rel $ReportPath)  overall=$($report.overall)"
if ($script:failed) { exit 1 }
exit 0
