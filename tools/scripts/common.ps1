# Shared helpers, dot-sourced by the other tools:  . "$PSScriptRoot\..\scripts\common.ps1"
# Windows PowerShell 5.1 compatible. Keep this file ASCII-only (5.1 reads BOM-less files as ANSI).

$script:WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path

function Get-WorkspaceRoot { return $script:WorkspaceRoot }

function Write-Status {
    param([ValidateSet('OK', 'WARN', 'FAIL', 'INFO')][string]$Level, [string]$Message, [string]$Hint)
    $color = @{ OK = 'Green'; WARN = 'Yellow'; FAIL = 'Red'; INFO = 'Cyan' }[$Level]
    Write-Host ("[{0,-4}] {1}" -f $Level, $Message) -ForegroundColor $color
    if ($Hint) { Write-Host ("       -> {0}" -f $Hint) -ForegroundColor DarkGray }
}

# Android SDK: ANDROID_HOME, then ANDROID_SDK_ROOT, then the Android Studio default location.
function Find-AndroidSdk {
    foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android\Sdk'))) {
        if ($candidate -and (Test-Path (Join-Path $candidate 'platform-tools'))) { return $candidate }
    }
    return $null
}

# Every Gradle game project the workspace knows about: games/*, templates/*, and any project
# sitting directly under the root (none today; kept so a stray project is still found).
function Get-GameProjects {
    $root = Get-WorkspaceRoot
    $dirs = @()
    foreach ($parent in @((Join-Path $root 'games'), (Join-Path $root 'templates'), $root)) {
        if (-not (Test-Path $parent)) { continue }
        $dirs += Get-ChildItem $parent -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName 'settings.gradle.kts') }
    }
    return $dirs | Sort-Object FullName -Unique
}

# Accepts an absolute/relative path or a bare project name (looked up in games/, templates/, root).
function Resolve-GameProject {
    param([Parameter(Mandatory)][string]$Project)
    if (Test-Path (Join-Path $Project 'settings.gradle.kts')) { return (Resolve-Path $Project).Path }
    $match = Get-GameProjects | Where-Object { $_.Name -ieq $Project } | Select-Object -First 1
    if ($match) { return $match.FullName }
    throw "Project '$Project' not found. Known projects: $((Get-GameProjects | ForEach-Object Name) -join ', ')"
}

# Runs the project's Gradle wrapper with ANDROID_HOME set for this process only.
function Invoke-Gradle {
    param([Parameter(Mandatory)][string]$ProjectDir, [Parameter(Mandatory)][string[]]$Arguments)
    $sdk = Find-AndroidSdk
    if ($sdk -and -not $env:ANDROID_HOME) { $env:ANDROID_HOME = $sdk }
    Push-Location $ProjectDir
    try {
        # Out-Host keeps Gradle's output out of the function's return value.
        & .\gradlew.bat @Arguments --console=plain | Out-Host
        return $LASTEXITCODE
    } finally {
        Pop-Location
    }
}

function Get-GradleWrapperVersion {
    param([string]$ProjectDir)
    $props = Join-Path $ProjectDir 'gradle\wrapper\gradle-wrapper.properties'
    if (-not (Test-Path $props)) { return $null }
    $line = Select-String -Path $props -Pattern 'gradle-([\d\.]+)-(bin|all)\.zip' | Select-Object -First 1
    if ($line) { return $line.Matches[0].Groups[1].Value }
    return $null
}

# Reads targetSdk from android/build.gradle.kts, following gradle.properties indirection.
function Get-TargetSdk {
    param([string]$ProjectDir)
    $build = Join-Path $ProjectDir 'android\build.gradle.kts'
    if (-not (Test-Path $build)) { return $null }
    $text = Get-Content $build -Raw
    if ($text -match 'targetSdk\s*=\s*(\d+)') { return [int]$Matches[1] }
    $props = Join-Path $ProjectDir 'gradle.properties'
    if ((Test-Path $props) -and ((Get-Content $props -Raw) -match '(?m)^android\.targetSdk=(\d+)')) { return [int]$Matches[1] }
    return $null
}
