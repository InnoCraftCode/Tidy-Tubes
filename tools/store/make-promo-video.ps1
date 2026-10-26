<#
.SYNOPSIS
    Encodes captured gameplay frames into Play Store promo videos with the game's music.
.DESCRIPTION
    Inputs (produced by capture-desktop.ps1 -Video):
      games\<Game>\lwjgl3\build\capture\frames\00000.png ...   1080x1920 at 30 fps
      games\<Game>\android\src\main\assets\sfx\music.(ogg|wav) soundtrack (looped)
      play-store\<Game>\graphics\video-background-1920x1080.png landscape backdrop
                                                                (generate-store-graphics.ps1)
    Outputs in play-store\<Game>\video\:
      promo-landscape-1920x1080.mp4   for the Play listing (upload to YouTube, paste the link)
      promo-portrait-1080x1920.mp4    for YouTube Shorts / TikTok / Instagram
.EXAMPLE
    .\tools\store\make-promo-video.ps1 -Game sortpuzzle
#>
[CmdletBinding()]
param([Parameter(Mandatory)][string]$Game)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'

function Find-Ffmpeg {
    $cmd = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    # winget puts it on PATH only for new shells; look where it installs.
    $hit = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($hit) { return $hit.FullName }
    return $null
}

$root = Get-WorkspaceRoot
$ffmpeg = Find-Ffmpeg
if (-not $ffmpeg) { Write-Status FAIL 'FFmpeg not found' 'winget install --id Gyan.FFmpeg -e'; exit 1 }

$frames = Join-Path $root "games\$Game\lwjgl3\build\capture\frames"
$count = @(Get-ChildItem $frames -Filter *.png -ErrorAction SilentlyContinue).Count
if ($count -lt 30) { Write-Status FAIL "No captured frames in $frames" "Run: .\tools\store\capture-desktop.ps1 -Game $Game -Video"; exit 1 }
$duration = [Math]::Round($count / 30.0, 3)
$inv = [Globalization.CultureInfo]::InvariantCulture
$d = $duration.ToString($inv)
$videoOut = ($duration - 0.8).ToString($inv)
$audioOut = ($duration - 1.5).ToString($inv)

$sfx = Join-Path $root "games\$Game\android\src\main\assets\sfx"
$music = @('music.ogg', 'music.wav') | ForEach-Object { Join-Path $sfx $_ } | Where-Object { Test-Path $_ } | Select-Object -First 1
# Dedicated backdrop (title, no art behind the gameplay); the feature graphic is the fallback.
$feature = Join-Path $root "play-store\$Game\graphics\video-background-1920x1080.png"
if (-not (Test-Path $feature)) { $feature = Join-Path $root "play-store\$Game\graphics\feature-graphic-1024x500.png" }
$outDir = Join-Path $root "play-store\$Game\video"
New-Item -ItemType Directory -Force $outDir | Out-Null

$framePattern = Join-Path $frames '%05d.png'
$encode = @('-c:v', 'libx264', '-preset', 'slow', '-crf', '18', '-pix_fmt', 'yuv420p', '-r', '30', '-movflags', '+faststart')
$audioArgs = @()
$audioFilter = ''
$audioMap = @()
if ($music) {
    $audioArgs = @('-stream_loop', '-1', '-i', $music)
    $audioFilter = ";[A]afade=t=in:st=0:d=0.5,afade=t=out:st=${audioOut}:d=1.5[a]"
    $audioMap = @('-map', '[a]', '-c:a', 'aac', '-b:a', '192k')
} else {
    Write-Status WARN 'No music file: videos will be silent'
}

function Invoke-Ffmpeg([string[]]$ffArgs, [string]$out) {
    & $ffmpeg -hide_banner -loglevel error -y @ffArgs $out
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $out" }
    Write-Status OK ("{0}  ({1:N1} MB, {2} s)" -f $out, ((Get-Item $out).Length / 1MB), $d)
}

# --- Portrait: the raw gameplay. -------------------------------------------------------------
$portraitInputs = @('-framerate', '30', '-i', $framePattern) + $audioArgs
$filter = "[0:v]fade=t=in:st=0:d=0.4,fade=t=out:st=${videoOut}:d=0.8,format=yuv420p[v]"
if ($music) { $filter += $audioFilter.Replace('[A]', '[1:a]') }
Invoke-Ffmpeg ($portraitInputs + @('-filter_complex', $filter, '-map', '[v]') + $audioMap + $encode + @('-t', $d)) (Join-Path $outDir 'promo-portrait-1080x1920.mp4')

# --- Landscape: gameplay framed on the feature graphic (Play shows promo videos 16:9). ------
if (-not (Test-Path $feature)) { Write-Status WARN "No feature graphic - landscape video skipped ($feature)"; exit 0 }
$landInputs = @('-loop', '1', '-framerate', '30', '-i', $feature, '-framerate', '30', '-i', $framePattern) + $audioArgs
$filter = "[0:v]scale=-2:1080,crop=1920:1080,setsar=1[bg];" +
          "[1:v]scale=-2:1000,pad=iw+12:ih+12:6:6:color=0x3A4470[g];" +
          "[bg][g]overlay=x=W-w-150:y=(H-h)/2:shortest=1," +
          "fade=t=in:st=0:d=0.4,fade=t=out:st=${videoOut}:d=0.8,format=yuv420p[v]"
if ($music) { $filter += $audioFilter.Replace('[A]', '[2:a]') }
Invoke-Ffmpeg ($landInputs + @('-filter_complex', $filter, '-map', '[v]') + $audioMap + $encode + @('-t', $d)) (Join-Path $outDir 'promo-landscape-1920x1080.mp4')
