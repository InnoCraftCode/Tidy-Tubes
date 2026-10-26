<#
.SYNOPSIS
    Creates games\<Name> from templates\libgdx-2d with your package ID and display name, plus
    play-store\<Name> (draft store graphics + Word listing).
.EXAMPLE
    .\tools\scripts\new-game.ps1 -Name color-dash -PackageId com.ben.laama.colordash -DisplayName "Color Dash"
.NOTES
    The package ID becomes the Play applicationId and can never change after the first upload.
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^[a-z][a-z0-9-]{1,40}$')][string]$Name,
    [Parameter(Mandatory)][ValidatePattern('^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*){2,}$')][string]$PackageId,
    [Parameter(Mandatory)][string]$DisplayName,
    [string]$Template = 'libgdx-2d',
    [switch]$SkipTests
)

. "$PSScriptRoot\common.ps1"
$ErrorActionPreference = 'Stop'

$root = Get-WorkspaceRoot
$src = Join-Path $root "templates\$Template"
$dst = Join-Path $root "games\$Name"
if (-not (Test-Path $src)) { throw "Template not found: $src" }
if (Test-Path $dst) { throw "games\$Name already exists. Pick another name or delete it yourself." }
if ($PackageId -like 'com.example.*') { throw 'com.example.* is rejected by Google Play. Use your own reverse domain.' }

$oldPackage = 'com.example.gametemplate'
$oldPath = 'com\example\gametemplate'
$newPath = $PackageId.Replace('.', '\')
$projectName = $Name.Replace('-', '')

# 1. Copy, skipping build outputs and machine-specific files.
$exclude = '\\(build|bin|\.gradle|\.idea|\.settings|libs)(\\|$)|\.project$|\.classpath$|local\.properties$|keystore\.properties$|admob\.properties$'
Get-ChildItem $src -Recurse -Force -File | Where-Object { $_.FullName.Substring($src.Length) -notmatch $exclude } | ForEach-Object {
    $target = Join-Path $dst $_.FullName.Substring($src.Length + 1)
    New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
    Copy-Item $_.FullName $target
}

# 2. Move Java sources to the new package path.
foreach ($srcSet in Get-ChildItem $dst -Recurse -Directory | Where-Object { $_.FullName -match '\\src\\(main|test)\\java$' }) {
    $from = Join-Path $srcSet.FullName $oldPath
    if (-not (Test-Path $from)) { continue }
    $to = Join-Path $srcSet.FullName $newPath
    New-Item -ItemType Directory -Force $to | Out-Null
    Get-ChildItem $from -Force | Move-Item -Destination $to
    # Remove the now-empty com\example\gametemplate chain.
    $leftover = Join-Path $srcSet.FullName 'com\example'
    if ((Test-Path $leftover) -and -not (Get-ChildItem $leftover -Recurse -File)) { Remove-Item $leftover -Recurse -Force }
}

# 3. Rewrite identifiers in text files (UTF-8 without BOM, which Gradle and javac expect).
$utf8 = New-Object System.Text.UTF8Encoding($false)
Get-ChildItem $dst -Recurse -File -Include *.java, *.kts, *.xml, *.properties, *.md, *.pro, *.example, *.toml | ForEach-Object {
    $text = [IO.File]::ReadAllText($_.FullName)
    $new = $text.Replace($oldPackage, $PackageId).Replace('gametemplate', $projectName).Replace('Game Template', $DisplayName)
    if ($new -ne $text) { [IO.File]::WriteAllText($_.FullName, $new, $utf8) }
}

# 4. Machine-local SDK pointer for Gradle (git-ignored).
$sdk = Find-AndroidSdk
if ($sdk) { [IO.File]::WriteAllText((Join-Path $dst 'local.properties'), "sdk.dir=$($sdk.Replace('\', '\\').Replace(':', '\:'))`n", $utf8) }

Write-Status OK "Created games\$Name ($PackageId)"

# 5. Matching Google Play folder: play-store\<Name> with draft graphics and the Word listing.
& (Join-Path $root 'tools\store\new-store-folder.ps1') -Game $Name -AppName $DisplayName -PackageId $PackageId

if (-not $SkipTests) {
    $code = Invoke-Gradle -ProjectDir $dst -Arguments @(':core:test')
    if ($code -ne 0) { Write-Status FAIL 'Template tests failed in the new project'; exit $code }
    Write-Status OK 'Unit tests pass'
}

Write-Host ''
Write-Host 'Next:' -ForegroundColor White
Write-Host "  .\tools\android-build\build-android.ps1 -Project $Name -Task Desktop"
Write-Host "  .\tools\android-build\build-android.ps1 -Project $Name -Task Debug"
Write-Host "  Then edit games\$Name\core - game rules in logic\, drawing in screens\."
exit 0
