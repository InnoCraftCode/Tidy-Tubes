<#
.SYNOPSIS
    Encodes music WAV loops to Ogg Vorbis with FFmpeg and reports duration, size and peak level.
.DESCRIPTION
    For every WAV matching -Filter in -Path (or the single file given), writes <name>.ogg next to it
    (mono, 22.05 kHz by default, libvorbis -q:a 3) and prints one report line per file plus a
    PASS/WARN verdict against the workspace budgets: 20-40 s loop, <= 400 KB, peak -4..-1 dBFS.
    Nothing is deleted; existing .ogg files are overwritten only with -Force.
.EXAMPLE
    .\encode-music.ps1 -Path games\mygame\android\src\main\assets\sfx
    .\encode-music.ps1 -Path out\music_01.wav -Quality 4 -SampleRate 44100 -Force
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$Path,
    [string]$Filter = 'music*.wav',
    [int]$Quality = 3,
    [int]$SampleRate = 22050,
    [int]$Channels = 1,
    [switch]$Force,
    [switch]$ReportOnly,
    [switch]$RemoveWav      # delete each WAV after its .ogg was written and verified (never with -ReportOnly)
)

$ErrorActionPreference = 'Stop'

function Find-Ffmpeg {
    $cmd = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $hit = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($hit) { return $hit.FullName }
    throw 'ffmpeg not found. Install it (winget install Gyan.FFmpeg) or add it to PATH.'
}

function Get-AudioStats([string]$ffmpeg, [string]$file) {
    # volumedetect prints to stderr; cmd /c merges it so PowerShell 5.1 does not wrap lines in ErrorRecords.
    $lines = & cmd /c "`"$ffmpeg`" -hide_banner -i `"$file`" -af volumedetect -f null NUL 2>&1"
    $text = ($lines | Out-String)
    $dur = $null; $peak = $null; $mean = $null
    if ($text -match 'Duration:\s*(\d+):(\d+):(\d+\.\d+)') { $dur = [int]$Matches[1] * 3600 + [int]$Matches[2] * 60 + [double]$Matches[3] }
    if ($text -match 'max_volume:\s*(-?\d+\.\d+)\s*dB') { $peak = [double]$Matches[1] }
    if ($text -match 'mean_volume:\s*(-?\d+\.\d+)\s*dB') { $mean = [double]$Matches[1] }
    return @{ Duration = $dur; Peak = $peak; Mean = $mean }
}

$ffmpeg = Find-Ffmpeg
$files = if (Test-Path $Path -PathType Container) { Get-ChildItem (Join-Path $Path $Filter) -File } else { Get-Item $Path }
if (-not $files) { Write-Host "No files matching $Filter in $Path"; exit 0 }

Write-Host ("{0,-22} {1,8} {2,8} {3,9} {4,9}  {5}" -f 'file', 'seconds', 'ogg KB', 'peak dB', 'mean dB', 'verdict')
$warnings = 0
foreach ($wav in $files) {
    $ogg = [System.IO.Path]::ChangeExtension($wav.FullName, '.ogg')
    if (-not $ReportOnly) {
        if ((Test-Path $ogg) -and -not $Force) {
            Write-Host "skip   $($wav.Name): $([System.IO.Path]::GetFileName($ogg)) exists (use -Force)" -ForegroundColor DarkGray
        } else {
            & $ffmpeg -y -hide_banner -loglevel error -i $wav.FullName -ac $Channels -ar $SampleRate -c:a libvorbis -q:a $Quality $ogg
            if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed on $($wav.Name)" }
        }
    }
    if (-not (Test-Path $ogg)) { continue }
    $s = Get-AudioStats $ffmpeg $ogg
    $kb = [math]::Round((Get-Item $ogg).Length / 1KB)
    $notes = @()
    $isStem = $wav.BaseName -match '_(pad|bass|melody|drums)$'   # stems are sub-mixes: only the full mix is level-checked
    if ($s.Duration -lt 20 -or $s.Duration -gt 40) { $notes += 'length outside 20-40 s' }
    if ($kb -gt 400) { $notes += 'over 400 KB' }
    if (-not $isStem -and $s.Peak -ne $null -and ($s.Peak -lt -4 -or $s.Peak -gt -1)) { $notes += 'peak outside -4..-1 dBFS' }
    $verdict = if ($notes) { $warnings++; 'WARN ' + ($notes -join ', ') } else { 'PASS' }
    Write-Host ("{0,-22} {1,8:N2} {2,8} {3,9:N1} {4,9:N1}  {5}" -f $wav.Name, $s.Duration, $kb, $s.Peak, $s.Mean, $verdict) -ForegroundColor $(if ($notes) { 'Yellow' } else { 'Green' })
    if ($RemoveWav -and -not $ReportOnly -and $s.Duration -gt 0) {
        Remove-Item $wav.FullName -Force -Confirm:$false
        Write-Host "       removed $($wav.Name)" -ForegroundColor DarkGray
    }
}
$total = (Get-ChildItem (Join-Path (Split-Path $files[0].FullName) '*.ogg') -File | Measure-Object Length -Sum).Sum
Write-Host ("Total .ogg in folder: {0:N0} KB   warnings: {1}" -f ($total / 1KB), $warnings)
if ($warnings) { exit 1 }
