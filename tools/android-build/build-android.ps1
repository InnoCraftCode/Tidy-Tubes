<#
.SYNOPSIS
    Tests, runs or builds a libGDX game project with its Gradle wrapper.
.PARAMETER Project
    Folder path, or a project name found in games\, templates\ or the workspace root (e.g. Sortpuzzle).
.PARAMETER Task
    Test     core unit tests (fastest; run first)
    Desktop  play the desktop build
    Debug    debug APK
    Install  install debug APK on the connected device/emulator
    Release  release APK (R8; signed only if keystore.properties exists)
    Bundle   release AAB for Google Play (tests run first)
.PARAMETER NoAds
    Passes -Pads.enabled=false (projects that support it): no ad SDK traffic at all.
.EXAMPLE
    .\tools\android-build\build-android.ps1 -Project Sortpuzzle -Task Test
    .\tools\android-build\build-android.ps1 -Project my-game -Task Bundle
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Project,
    [ValidateSet('Test', 'Desktop', 'Debug', 'Install', 'Release', 'Bundle')][string]$Task = 'Debug',
    [switch]$NoAds
)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'

$dir = Resolve-GameProject $Project
if (-not (Find-AndroidSdk) -and $Task -notin @('Test', 'Desktop')) {
    Write-Status FAIL 'Android SDK not found' 'Run tools\environment-check\check-environment.ps1'
    exit 1
}

$gradleArgs = switch ($Task) {
    'Test'    { @(':core:test') }
    'Desktop' { @(':lwjgl3:run') }
    'Debug'   { @(':android:assembleDebug') }
    'Install' { @(':android:installDebug') }
    'Release' { @(':core:test', ':android:assembleRelease') }
    'Bundle'  { @(':core:test', ':android:bundleRelease') }
}
if ($NoAds) { $gradleArgs += '-Pads.enabled=false' }

if ($Task -in @('Release', 'Bundle')) {
    if (-not (Test-Path (Join-Path $dir 'keystore.properties'))) {
        Write-Status WARN 'No keystore.properties: the output will be UNSIGNED and cannot be uploaded to Play.' 'Copy keystore.properties.example and point it at your upload key.'
    }
    $target = Get-TargetSdk $dir
    if ($target -and $target -lt 36) {
        Write-Status WARN "targetSdk $target < 36: Google Play rejects new apps/updates below API 36 (since 2026-08-31)." 'Install platform 36, then raise compileSdk/targetSdk.'
    }
}

Write-Status INFO "$((Split-Path $dir -Leaf)): gradlew $($gradleArgs -join ' ')"
$code = Invoke-Gradle -ProjectDir $dir -Arguments $gradleArgs
if ($code -ne 0) { Write-Status FAIL "Gradle failed (exit $code)"; exit $code }

$outputs = Join-Path $dir 'android\build\outputs'
if ($Task -in @('Debug', 'Release', 'Bundle') -and (Test-Path $outputs)) {
    Get-ChildItem $outputs -Recurse -Include *.apk, *.aab |
        Sort-Object LastWriteTime -Descending | Select-Object -First 3 |
        ForEach-Object { Write-Status OK ("{0}  ({1:N1} MB)" -f $_.FullName, ($_.Length / 1MB)) }
}
Write-Status OK "$Task finished"
exit 0
