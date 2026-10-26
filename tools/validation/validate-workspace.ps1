<#
.SYNOPSIS
    Validates the workspace: structure, Claude Code agents/skills/commands, settings JSON,
    cross-references, secret leaks, and per-game build configuration. Read-only.
.PARAMETER RunTests
    Also runs :core:test for every game project (slower).
.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\validation\validate-workspace.ps1 -RunTests
#>
[CmdletBinding()]
param([switch]$RunTests)

. "$PSScriptRoot\..\scripts\common.ps1"
$root = Get-WorkspaceRoot
$script:fails = 0
$script:warns = 0
function Fail([string]$m, [string]$h) { $script:fails++; Write-Status FAIL $m $h }
function Warn([string]$m, [string]$h) { $script:warns++; Write-Status WARN $m $h }
$skipDirs = '\\(build|bin|\.gradle|\.idea|libs|out)\\'

# Returns @{ key = value; _lists = @{ key = @(items) }; _bodyLines = n } or $null if no frontmatter.
function Get-Frontmatter([string]$path) {
    $lines = Get-Content $path
    if (-not $lines -or $lines[0] -ne '---') { return $null }
    $fm = @{ _lists = @{} }
    $current = $null
    for ($i = 1; $i -lt $lines.Count; $i++) {
        $l = $lines[$i]
        if ($l -eq '---') { $fm['_bodyLines'] = $lines.Count - $i - 1; return $fm }
        if ($l -match '^([A-Za-z-]+):\s*(.*)$') {
            $current = $Matches[1]
            $fm[$current] = $Matches[2].Trim().Trim('"')
            if ($fm[$current] -eq '') { $fm._lists[$current] = @() }
        } elseif ($current -and $l -match '^\s+-\s+(.+)$') {
            $fm._lists[$current] += $Matches[1].Trim()
        }
    }
    return $null # unterminated
}

# ---------------------------------------------------------------------------- structure
Write-Host "`n== Structure ==" -ForegroundColor White
$required = @(
    'CLAUDE.md', 'README.md', '.gitignore', '.env.example', '.claude\settings.json',
    'tools\environment-check\check-environment.ps1', 'tools\validation\validate-workspace.ps1',
    'tools\android-build\build-android.ps1', 'tools\scripts\new-game.ps1', 'tools\scripts\common.ps1',
    'config\architecture\ADR-001-engine.md', 'config\release-checklists\google-play-release.md',
    'templates\libgdx-2d\settings.gradle.kts', 'templates\libgdx-2d\gradlew.bat',
    'templates\libgdx-2d\android\src\main\java\com\example\gametemplate\android\AdMobAdProvider.java',
    'games\README.md', 'docs\architecture.md', 'docs\installation.md', 'docs\troubleshooting.md',
    'play-store\README.md', 'play-store\_template\listing.json', 'tools\store\build-listing-doc.ps1',
    'tools\store\generate-store-graphics.ps1', 'tools\store\capture-screenshot.ps1', 'tools\store\new-store-folder.ps1'
)
$missing = @($required | Where-Object { -not (Test-Path (Join-Path $root $_)) })
if ($missing) { $missing | ForEach-Object { Fail "Missing: $_" } } else { Write-Status OK "$($required.Count) required paths present" }

$claudeLines = (Get-Content (Join-Path $root 'CLAUDE.md')).Count
if ($claudeLines -le 150) { Write-Status OK "CLAUDE.md is $claudeLines lines (<= 150)" } else { Warn "CLAUDE.md is $claudeLines lines" 'Move detail into skills/docs.' }

try {
    $null = Get-Content (Join-Path $root '.claude\settings.json') -Raw | ConvertFrom-Json
    Write-Status OK '.claude/settings.json is valid JSON'
} catch { Fail ".claude/settings.json invalid: $($_.Exception.Message)" }

# ---------------------------------------------------------------------------- skills
Write-Host "`n== Skills ==" -ForegroundColor White
$skillNames = @()
foreach ($dir in Get-ChildItem (Join-Path $root '.claude\skills') -Directory) {
    $file = Join-Path $dir.FullName 'SKILL.md'
    if (-not (Test-Path $file)) { Fail "Skill $($dir.Name): SKILL.md missing"; continue }
    $fm = Get-Frontmatter $file
    if (-not $fm) { Fail "Skill $($dir.Name): no/unterminated frontmatter"; continue }
    $problems = @()
    if ($fm['name'] -and $fm['name'] -ne $dir.Name) { $problems += "name '$($fm['name'])' != folder" }
    if (-not $fm['description']) { $problems += 'no description' }
    elseif ($fm['description'].Length -gt 1536) { $problems += 'description > 1536 chars (truncated in listing)' }
    $total = (Get-Content $file).Count
    if ($total -gt 500) { $problems += "$total lines (> 500)" }
    foreach ($link in [regex]::Matches((Get-Content $file -Raw), '\]\(([^)#:]+\.md)\)')) {
        if (-not (Test-Path (Join-Path $dir.FullName $link.Groups[1].Value))) { $problems += "broken link $($link.Groups[1].Value)" }
    }
    if ($problems) { Fail "Skill $($dir.Name): $($problems -join '; ')" } else { $skillNames += $dir.Name; Write-Status OK ("Skill {0} ({1} lines)" -f $dir.Name, $total) }
}

# ---------------------------------------------------------------------------- agents
Write-Host "`n== Agents ==" -ForegroundColor White
$agentNames = @()
foreach ($file in Get-ChildItem (Join-Path $root '.claude\agents') -Filter *.md) {
    $fm = Get-Frontmatter $file.FullName
    if (-not $fm) { Fail "Agent $($file.Name): no/unterminated frontmatter"; continue }
    $problems = @()
    if (-not $fm['name']) { $problems += 'no name' } elseif ($fm['name'] -match ':' -or $fm['name'].StartsWith('-')) { $problems += 'invalid name' }
    if (-not $fm['description']) { $problems += 'no description' }
    if ($fm['name'] -and $fm['name'] -ne $file.BaseName) { $problems += "name '$($fm['name'])' != file name" }
    foreach ($s in @($fm._lists['skills'])) { if ($s -and -not (Test-Path (Join-Path $root ".claude\skills\$s\SKILL.md"))) { $problems += "unknown skill '$s'" } }
    if ($fm['_bodyLines'] -lt 5) { $problems += 'body too short to be actionable' }
    if ($problems) { Fail "Agent $($file.Name): $($problems -join '; ')" } else { $agentNames += $fm['name']; Write-Status OK "Agent $($fm['name'])" }
}
if (($agentNames | Sort-Object -Unique).Count -ne $agentNames.Count) { Fail 'Duplicate agent names' }

# ---------------------------------------------------------------------------- commands
Write-Host "`n== Commands ==" -ForegroundColor White
foreach ($file in Get-ChildItem (Join-Path $root '.claude\commands') -Filter *.md) {
    $fm = Get-Frontmatter $file.FullName
    if (-not $fm -or -not $fm['description']) { Fail "Command /$($file.BaseName): missing description frontmatter" }
    elseif (Test-Path (Join-Path $root ".claude\skills\$($file.BaseName)")) { Warn "Command /$($file.BaseName) is shadowed by a skill of the same name" }
    else { Write-Status OK "Command /$($file.BaseName)" }
}

# ---------------------------------------------------------------------------- cross-references
Write-Host "`n== Cross-references ==" -ForegroundColor White
$routing = Get-Content (Join-Path $root 'CLAUDE.md') | Where-Object { $_ -match '^\|[^|]+\|\s*[a-z-]+\s*\|' -and $_ -notmatch '^\|\s*(Task|Path|---)' }
$refProblems = 0
foreach ($row in $routing) {
    $cells = $row.Trim('|').Split('|') | ForEach-Object { $_.Trim() }
    if ($cells.Count -lt 3) { continue }
    if ($cells[1] -match '^[a-z-]+$' -and $agentNames -notcontains $cells[1]) { Fail "CLAUDE.md routes to unknown agent '$($cells[1])'"; $refProblems++ }
    foreach ($s in $cells[2].Split(',') | ForEach-Object { $_.Trim() }) {
        if ($s -match '^[a-z-]+$' -and $skillNames -notcontains $s) { Fail "CLAUDE.md references unknown skill '$s'"; $refProblems++ }
    }
}
if (-not $refProblems) { Write-Status OK "CLAUDE.md routing: all agents and skills exist ($($routing.Count) rows)" }

# ---------------------------------------------------------------------------- secrets
Write-Host "`n== Secrets ==" -ForegroundColor White
$gitignore = Get-Content (Join-Path $root '.gitignore') -Raw
foreach ($pattern in @('keystore.properties', '*.jks', '*.keystore', '.env', 'local.properties', 'admob.properties')) {
    if ($gitignore -notmatch [regex]::Escape($pattern)) { Fail "Root .gitignore does not cover $pattern" }
}
$secretFiles = Get-ChildItem $root -Recurse -Force -File -Include *.jks, *.keystore, *.p12, keystore.properties, .env -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch $skipDirs }
foreach ($f in $secretFiles) { Warn "Secret-bearing file present (git-ignored, keep it out of shares/zips): $($f.FullName.Substring($root.Length + 1))" }

# Content scan. keystore.properties is never opened (its content must not reach logs).
$leakPatterns = @{
    'private key block'          = '-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----'
    # A literal value, not a lookup such as keystoreProperties.getProperty(...) in Gradle scripts.
    'store/key password value'   = '(?m)^\s*(storePassword|keyPassword)\s*=\s*(?!CHANGE_ME|\.\.\.|\s*$|keystoreProperties|System\.getenv|providers\.|findProperty)\S+'
    'Anthropic API key'          = 'sk-ant-[A-Za-z0-9_-]{20,}'
    'Google API key'             = 'AIza[0-9A-Za-z_-]{35}'
    'production AdMob ID in tracked file' = 'ca-app-pub-(?!3940256099942544|0000000000000000)\d{16}'
}
$textFiles = Get-ChildItem $root -Recurse -File -Include *.java, *.kt, *.kts, *.xml, *.properties, *.md, *.json, *.toml, *.txt, *.ps1, *.pro -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch $skipDirs -and $_.Name -notin @('keystore.properties', 'admob.properties', 'local.properties') }
$leaks = 0
foreach ($f in $textFiles) {
    $content = Get-Content $f.FullName -Raw
    if (-not $content) { continue }
    foreach ($k in $leakPatterns.Keys) {
        if ($f.Name -eq 'validate-workspace.ps1') { continue }
        if ($content -match $leakPatterns[$k]) { Fail "Possible $k in $($f.FullName.Substring($root.Length + 1))"; $leaks++ }
    }
}
if (-not $leaks) { Write-Status OK "No secret patterns in $($textFiles.Count) text files" }

# ---------------------------------------------------------------------------- game projects
Write-Host "`n== Game projects ==" -ForegroundColor White
foreach ($p in Get-GameProjects) {
    $name = $p.FullName.Substring($root.Length + 1)
    $gv = Get-GradleWrapperVersion $p.FullName
    $toml = Join-Path $p.FullName 'gradle\libs.versions.toml'
    $agp = if ((Test-Path $toml) -and ((Get-Content $toml -Raw) -match 'agp\s*=\s*"([\d\.]+)"')) { $Matches[1] } else { '?' }
    if ($agp.StartsWith('8.') -and $gv -and $gv.StartsWith('9.')) { Fail "$name : Gradle $gv with AGP $agp will not build" 'Restore gradle-8.14.3 in gradle-wrapper.properties' }
    else { Write-Status OK "$name : Gradle $gv, AGP $agp" }

    $target = Get-TargetSdk $p.FullName
    if ($target -and $target -lt 36) { Warn "$name : targetSdk $target - Google Play requires 36 for new apps/updates" 'Install platform 36, raise compileSdk/targetSdk (docs/installation.md)' }
    elseif ($target) { Write-Status OK "$name : targetSdk $target" }

    $pgi = Join-Path $p.FullName '.gitignore'
    if (-not (Test-Path $pgi) -or (Get-Content $pgi -Raw) -notmatch 'keystore\.properties' -or (Get-Content $pgi -Raw) -notmatch '\*\.jks') {
        Fail "$name : .gitignore must cover keystore.properties and *.jks"
    }
    $appId = if ((Get-Content (Join-Path $p.FullName 'android\build.gradle.kts') -Raw) -match 'applicationId\s*=\s*"([^"]+)"') { $Matches[1] } else { '?' }
    if ($appId -like 'com.example.*' -and $p.Parent.Name -ne 'templates') { Fail "$name : applicationId $appId is rejected by Google Play" }

    if ($p.Parent.Name -eq 'games') {
        $store = Join-Path $root "play-store\$($p.Name)"
        if (-not (Test-Path (Join-Path $store 'listing.json'))) {
            Warn "$name : no play-store\$($p.Name)\ kit" "tools\store\new-store-folder.ps1 -Game $($p.Name) -AppName <Name> -PackageId $appId"
        } else {
            $shots = @(Get-ChildItem (Join-Path $store 'screenshots\phone') -File -Include *.png, *.jpg -Recurse -ErrorAction SilentlyContinue).Count
            $docs = @(Get-ChildItem $store -Filter *.docx -ErrorAction SilentlyContinue).Count
            Write-Status INFO "$name : play-store kit present ($shots screenshot(s), $docs Word listing(s); Play needs >= 2 screenshots)"
        }
    }

    if ($RunTests) {
        Write-Status INFO "$name : running :core:test"
        $code = Invoke-Gradle -ProjectDir $p.FullName -Arguments @(':core:test', '-q')
        if ($code -eq 0) { Write-Status OK "$name : unit tests pass" } else { Fail "$name : unit tests failed (exit $code)" }
    }
}

Write-Host ""
$summary = "Validation: $script:fails failure(s), $script:warns warning(s)"
if ($script:fails) { Write-Status FAIL $summary; exit 1 }
Write-Status OK $summary
exit 0
