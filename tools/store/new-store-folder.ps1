<#
.SYNOPSIS
    Creates play-store\<Game>\ (graphics, screenshots, listing.json) for a game, then draft
    graphics and the Word listing. Called by new-game.ps1; safe to rerun (never overwrites).
.EXAMPLE
    .\tools\store\new-store-folder.ps1 -Game color-dash -AppName "Color Dash" -PackageId com.ben.laama.colordash
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [Parameter(Mandatory)][string]$AppName,
    [Parameter(Mandatory)][string]$PackageId,
    [switch]$SkipDoc
)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'

$root = Get-WorkspaceRoot
$dir = Join-Path $root "play-store\$Game"
foreach ($sub in @('graphics', 'screenshots\phone')) { New-Item -ItemType Directory -Force (Join-Path $dir $sub) | Out-Null }

$listing = Join-Path $dir 'listing.json'
if (Test-Path $listing) {
    Write-Status INFO "listing.json already exists - kept"
} else {
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    $text = [IO.File]::ReadAllText((Join-Path $root 'play-store\_template\listing.json'), [Text.Encoding]::UTF8)
    [IO.File]::WriteAllText($listing, $text.Replace('__APP_NAME__', $AppName).Replace('__PACKAGE_ID__', $PackageId), $utf8)
    Write-Status OK "Created $listing (fill in the <...> fields)"
}

& "$PSScriptRoot\generate-store-graphics.ps1" -Game $Game -Title $AppName
if (-not $SkipDoc) {
    try { & "$PSScriptRoot\build-listing-doc.ps1" -Game $Game }
    catch { Write-Status WARN "Word document not generated: $($_.Exception.Message)" 'Needs Microsoft Word; rerun tools\store\build-listing-doc.ps1 later.' }
}
