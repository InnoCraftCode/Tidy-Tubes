<#
.SYNOPSIS
    Reports which Android game-development prerequisites are installed. Read-only: installs nothing.
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\environment-check\check-environment.ps1
.NOTES
    Exit code 1 when a REQUIRED prerequisite is missing. Optional items only warn.
#>
[CmdletBinding()]
param([switch]$Quiet)

. "$PSScriptRoot\..\scripts\common.ps1"
$failures = 0
function Fail([string]$m, [string]$h) { $script:failures++; Write-Status FAIL $m $h }

Write-Host "`n== Required ==" -ForegroundColor White

# --- JDK -------------------------------------------------------------------------------------
$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) {
    $ver = (& cmd /c "java -version 2>&1" | Select-Object -First 1)
    if ($ver -match '"(\d+)') {
        $major = [int]$Matches[1]
        if ($major -ge 17) { Write-Status OK "JDK $major ($ver)" } else { Fail "JDK $major is too old" 'Install Temurin 21: winget install EclipseAdoptium.Temurin.21.JDK' }
    }
    if (-not $env:JAVA_HOME) { Write-Status WARN 'JAVA_HOME is not set' 'Point JAVA_HOME at the JDK 21 folder (Android Studio and Gradle read it).' }
} else {
    Fail 'Java not found on PATH' 'winget install EclipseAdoptium.Temurin.21.JDK'
}

# --- Android SDK -------------------------------------------------------------------------------
$sdk = Find-AndroidSdk
if ($sdk) {
    Write-Status OK "Android SDK: $sdk"
    if (-not $env:ANDROID_HOME) { Write-Status WARN 'ANDROID_HOME not set (tools set it per run; Android Studio uses local.properties)' "Optional: setx ANDROID_HOME `"$sdk`"" }
    $platforms = @(Get-ChildItem (Join-Path $sdk 'platforms') -Directory -ErrorAction SilentlyContinue | ForEach-Object Name)
    Write-Status INFO ("Platforms: " + ($platforms -join ', '))
    if (-not ($platforms -match '^android-35')) { Fail 'Platform android-35 missing (needed by current projects)' 'Android Studio > SDK Manager > Android 15 (API 35)' }
    if (-not ($platforms -match '^android-36')) { Write-Status WARN 'Platform android-36 missing: Play requires targetSdk 36 since 2026-08-31' 'Android Studio > Settings > Languages & Frameworks > Android SDK > Android 16 (API 36)' }
    $bt = @(Get-ChildItem (Join-Path $sdk 'build-tools') -Directory -ErrorAction SilentlyContinue | ForEach-Object Name)
    if ($bt) { Write-Status OK ("Build-tools: " + ($bt -join ', ')) } else { Fail 'No build-tools installed' 'SDK Manager > SDK Tools > Android SDK Build-Tools' }
    $adb = Join-Path $sdk 'platform-tools\adb.exe'
    if (Test-Path $adb) {
        $adbVer = (& $adb version | Select-Object -First 1)
        Write-Status OK "adb: $adbVer"
        $devices = @(& $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' })
        if ($devices.Count) { Write-Status OK "$($devices.Count) device(s) connected" } else { Write-Status WARN 'No device/emulator connected' 'Enable USB debugging on a phone, or start an AVD.' }
    } else { Fail 'platform-tools (adb) missing' 'SDK Manager > SDK Tools > Android SDK Platform-Tools' }
} else {
    Fail 'Android SDK not found' 'Install Android Studio (winget install Google.AndroidStudio), then run its SDK setup.'
}

# --- Gradle wrappers ---------------------------------------------------------------------------
foreach ($p in Get-GameProjects) {
    $gv = Get-GradleWrapperVersion $p.FullName
    $agp = $null
    $toml = Join-Path $p.FullName 'gradle\libs.versions.toml'
    if ((Test-Path $toml) -and ((Get-Content $toml -Raw) -match 'agp\s*=\s*"([\d\.]+)"')) { $agp = $Matches[1] }
    $msg = "Gradle wrapper $gv / AGP $agp : $($p.Name)"
    if ($agp -and $agp.StartsWith('8.') -and $gv -and $gv.StartsWith('9.')) {
        Fail $msg 'AGP 8.x does not run on Gradle 9. Restore distributionUrl to gradle-8.14.3 (see docs/troubleshooting.md).'
    } else { Write-Status OK $msg }
}

Write-Host "`n== Recommended ==" -ForegroundColor White

$git = Get-Command git -ErrorAction SilentlyContinue
if ($git) { Write-Status OK ("Git: " + (& git --version)) } else { Write-Status WARN 'Git not installed: no version history or rollback for your games' 'winget install --id Git.Git -e' }

if ($sdk) {
    $sdkmanager = Get-ChildItem (Join-Path $sdk 'cmdline-tools') -Recurse -Filter sdkmanager.bat -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($sdkmanager) { Write-Status OK "sdkmanager: $($sdkmanager.FullName)" } else { Write-Status WARN 'Android SDK Command-line Tools missing (sdkmanager/avdmanager)' 'SDK Manager > SDK Tools > Android SDK Command-line Tools (latest)' }
    $emu = Join-Path $sdk 'emulator\emulator.exe'
    if (Test-Path $emu) {
        $avds = @(& $emu -list-avds 2>$null | Where-Object { $_ -and $_ -notmatch '^INFO' })
        if ($avds.Count) { Write-Status OK ("AVDs: " + ($avds -join ', ')) } else { Write-Status WARN 'Emulator installed but no AVD defined' 'Android Studio > Device Manager > Create device (Pixel, API 35/36 x86_64 image).' }
    } else { Write-Status WARN 'Android emulator not installed' 'SDK Manager > SDK Tools > Android Emulator' }
}

$studio = 'C:\Program Files\Android\Android Studio\bin\studio64.exe'
if (Test-Path $studio) { Write-Status OK 'Android Studio installed' } else { Write-Status WARN 'Android Studio not found (optional but simplest SDK manager / profiler)' 'winget install Google.AndroidStudio' }

$claudeCli = Get-Command claude -ErrorAction SilentlyContinue
$claudeExt = Get-ChildItem "$env:USERPROFILE\.vscode\extensions" -Directory -Filter 'anthropic.claude-code-*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
if ($claudeCli) { Write-Status OK 'Claude Code CLI on PATH' }
elseif ($claudeExt) { Write-Status OK "Claude Code VS Code extension: $($claudeExt.Name)" 'CLI not on PATH; optional: https://code.claude.com/docs/en/setup' }
else { Write-Status WARN 'Claude Code not detected' 'https://code.claude.com/docs/en/setup' }

Write-Host "`n== Optional engines (not required by the chosen libGDX stack) ==" -ForegroundColor White
$godot = Get-Command godot*, Godot* -ErrorAction SilentlyContinue | Select-Object -First 1
if ($godot) { Write-Status INFO "Godot: $($godot.Source)" } else { Write-Status INFO 'Godot not installed (optional; see docs/architecture.md)' }
if (Test-Path 'C:\Program Files\Unity Hub') { Write-Status INFO 'Unity Hub installed' } else { Write-Status INFO 'Unity not installed (optional; see docs/architecture.md)' }

Write-Host ""
if ($failures) { Write-Status FAIL "$failures required prerequisite(s) missing"; exit 1 }
Write-Status OK 'All required prerequisites present'
exit 0
