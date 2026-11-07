<#
.SYNOPSIS
    Renders the cinematic trailers (main ~51 s and 15 s, 16:9 and 9:16) for each language.
.DESCRIPTION
    Prerequisites (see play-store\<Game>\video\trailer\README.md):
      1. Scenes:  gradlew :lwjgl3:storeCapture -PcaptureMode=trailer -PcaptureLang=<l> -PcaptureSize=1620x2880
                  -PcaptureOut=<game>\lwjgl3\build\capture\cine-<l>
      2. Voice:   tools\store\trailer\voice.py (Kokoro) -> play-store\<Game>\video\trailer\voice\<l>\
                  (no voice folder = music-and-captions version, used for Arabic)
      3. Music:   play-store\<Game>\video\trailer\music\ (MusicSynth, original)
    Output: play-store\<Game>\video\trailer\out\tidy-tubes-<cut>-<lang>-<16x9|9x16>.mp4
.EXAMPLE
    .\tools\store\trailer\make-cinematic-trailer.ps1 -Game sortpuzzle
    .\tools\store\trailer\make-cinematic-trailer.ps1 -Game sortpuzzle -Lang en -Cut main -Format land
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [string[]]$Lang = @('en', 'fr', 'ar'),
    [string[]]$Cut = @('main', 'short'),
    [string[]]$Format = @('land', 'port')
)

. "$PSScriptRoot\..\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'
$root = Get-WorkspaceRoot
$ffmpeg = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ffmpeg) {
    $hit = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($hit) { $ffmpeg = $hit.FullName }
}
if (-not $ffmpeg) { Write-Status FAIL 'FFmpeg not found' 'winget install --id Gyan.FFmpeg -e'; exit 1 }

$kit = Join-Path $root "play-store\$Game\video\trailer"
$assets = Join-Path $root "games\$Game\android\src\main\assets"
foreach ($l in $Lang) {
    $scenes = Join-Path $root "games\$Game\lwjgl3\build\capture\cine-$l"
    if (-not (Test-Path $scenes)) { Write-Status FAIL "No scenes for $l in $scenes"; exit 1 }
    $voice = Join-Path $kit "voice\$l"
    foreach ($c in $Cut) {
        foreach ($f in $Format) {
            $name = "tidy-tubes-$c-$l-$(if ($f -eq 'land') { '16x9' } else { '9x16' }).mp4"
            $argList = @("$PSScriptRoot\TrailerComposer.java", '--scenes', $scenes, '--lang', $l, '--cut', $c, '--format', $f,
                '--music', (Join-Path $kit 'music'), '--sfx', (Join-Path $assets 'sfx'), '--fonts', (Join-Path $assets 'fonts'),
                '--icon', (Join-Path $root "play-store\$Game\graphics\icon-512.png"), '--ffmpeg', $ffmpeg,
                '--out', (Join-Path $kit "out\$name"))
            if (Test-Path (Join-Path $voice 'durations.txt')) { $argList += @('--voice', $voice) }
            & java @argList
            if ($LASTEXITCODE -ne 0) { Write-Status FAIL "Render failed: $name"; exit 1 }
            Write-Status OK $name
        }
    }
}
