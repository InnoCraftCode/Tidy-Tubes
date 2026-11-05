param(
    [string]$Template = (Join-Path $PSScriptRoot '..\..\play-store\sortpuzzle\data-safety-template.csv'),
    [string]$OutDir   = (Join-Path $PSScriptRoot '..\..\play-store\sortpuzzle')
)
$ErrorActionPreference = 'Stop'

# ---- Tidy Tubes 1.0.8+ answers (source: play-store\sortpuzzle\data-safety-answers.md) ----
# Keys are "QuestionId" (value rows) or "QuestionId|ResponseId" (choice rows). Anything absent stays blank.
$top = @{
    'PSL_DATA_COLLECTION_COLLECTS_PERSONAL_DATA'            = 'TRUE'
    'PSL_DATA_COLLECTION_ENCRYPTED_IN_TRANSIT'              = 'TRUE'
    'PSL_SUPPORTED_ACCOUNT_CREATION_METHODS|PSL_ACM_NONE'   = 'TRUE'   # no in-app account; Play Games uses the Google account
    'PSL_SUPPORT_DATA_DELETION_BY_USER|DATA_DELETION_YES'   = 'TRUE'   # Reset progress + Google account + policy page
    'PSL_DATA_DELETION_URL'                                 = 'https://innocraftcode.github.io/Tidy-Tubes/privacy-policy/'
    'PSL_INDEPENDENTLY_VALIDATED'                           = 'FALSE'  # no MASA review
    # PSL_ACM_SPECIFY, PSL_ACCOUNT_DELETION_URL: only for in-app accounts -> blank
    # PSL_DATA_COLLECTION_COMPLIES_FAMILY_POLICY: only for Families apps -> blank
    # PSL_UPI_BADGE_OPT_IN: India UPI badge -> blank
}
# type id -> @{ required = $true/$false ; purposes = @(...) }  (every type is collected AND shared, not ephemeral)
$types = [ordered]@{
    'PSL_APPROX_LOCATION'        = @{ required = $true;  purposes = @('PSL_ADVERTISING', 'PSL_ANALYTICS') }
    'PSL_CRASH_LOGS'             = @{ required = $true;  purposes = @('PSL_ANALYTICS') }
    'PSL_PERFORMANCE_DIAGNOSTICS'= @{ required = $true;  purposes = @('PSL_ANALYTICS', 'PSL_ADVERTISING') }
    'PSL_USER_INTERACTION'       = @{ required = $true;  purposes = @('PSL_ANALYTICS') }
    'PSL_USER_GENERATED_CONTENT' = @{ required = $false; purposes = @('PSL_APP_FUNCTIONALITY') }
    'PSL_DEVICE_ID'              = @{ required = $true;  purposes = @('PSL_ADVERTISING', 'PSL_ANALYTICS', 'PSL_APP_FUNCTIONALITY') }
    'PSL_USER_ACCOUNT'           = @{ required = $false; purposes = @('PSL_APP_FUNCTIONALITY') }
}

$rows = Import-Csv $Template -Encoding UTF8
$cols = 'Question ID (machine readable)', 'Response ID (machine readable)', 'Response value', 'Answer requirement', 'Human-friendly question label'
$touched = @{}

foreach ($r in $rows) {
    $q = $r.'Question ID (machine readable)'; $rid = $r.'Response ID (machine readable)'
    $r.'Response value' = ''
    if ($q -notlike 'PSL_DATA_TYPES_*' -and $q -notlike 'PSL_DATA_USAGE_RESPONSES:*') {
        $key = if ($rid) { "$q|$rid" } else { $q }
        if ($top.ContainsKey($key)) { $r.'Response value' = $top[$key] }
        continue
    }
    if ($q -like 'PSL_DATA_TYPES_*') {
        if ($types.Contains($rid)) { $r.'Response value' = 'TRUE'; $touched[$rid] = $true }
        continue
    }
    if ($q -like 'PSL_DATA_USAGE_RESPONSES:*') {
        $parts = $q -split ':'; $type = $parts[1]; $sub = $parts[2]
        if (-not $types.Contains($type)) { continue }
        $t = $types[$type]
        switch ($sub) {
            'PSL_DATA_USAGE_COLLECTION_AND_SHARING' { $r.'Response value' = 'TRUE' }   # collected + shared
            'PSL_DATA_USAGE_EPHEMERAL'              { $r.'Response value' = 'FALSE' }
            'DATA_USAGE_USER_CONTROL' {
                $want = if ($t.required) { 'PSL_DATA_USAGE_USER_CONTROL_REQUIRED' } else { 'PSL_DATA_USAGE_USER_CONTROL_OPTIONAL' }
                if ($rid -eq $want) { $r.'Response value' = 'TRUE' }
            }
            'DATA_USAGE_COLLECTION_PURPOSE' { if ($t.purposes -contains $rid) { $r.'Response value' = 'TRUE' } }
            'DATA_USAGE_SHARING_PURPOSE'    { if ($t.purposes -contains $rid) { $r.'Response value' = 'TRUE' } }
        }
    }
}
$missing = $types.Keys | Where-Object { -not $touched[$_] }
if ($missing) { throw "Data type ids not found in template: $($missing -join ', ')" }

function Q([string]$s) {
    if ($s -match '[,"\r\n]') { return '"' + ($s -replace '"', '""') + '"' }
    return $s
}
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add(($cols | ForEach-Object { Q $_ }) -join ',')
foreach ($r in $rows) { $lines.Add((($cols | ForEach-Object { Q $r.$_ }) -join ',')) }
$body = ($lines -join "`r`n") + "`r`n"
$utf8 = New-Object System.Text.UTF8Encoding($false)

$notes = @'
# Tidy Tubes (com.ben.laama.sortpuzzle) - Play Console "Data safety" import, version 1.0.8 and later. Generated 2026-11-04 (template: console export with account-creation questions, 777 rows).
# RECOMMENDATIONS (read before importing)
# 1. Import the file WITHOUT these notes: data-safety-import.csv. Play Console only accepts the 5-column template; comment lines like these make the import fail.
# 2. Where: Play Console > Policy and programs > App content > Data safety > Start/Manage > "Import from CSV" (top right). Importing OVERWRITES every answer already in the form.
# 3. After the import, open every section and click through to the end: the console re-validates each answer and may ask for confirmations the CSV cannot carry.
# 4. Summary of what this file declares: collects and shares data = YES; encrypted in transit = YES; account creation = NONE (Play Games sign-in uses the Google account, no in-app account); deletion request = YES with the privacy policy URL as the "delete data" link; independent MASA review = NO. Families and UPI badge rows left blank on purpose (not applicable).
# 5. Data types declared (all collected AND shared with Google SDKs, none ephemeral): approximate location (AdMob, ads+analytics), crash logs (Crashlytics), diagnostics (Crashlytics/Analytics/AdMob), app interactions (Analytics), other user-generated content (Play Games scores, OPTIONAL), device or other identifiers (advertising ID + Firebase app instance ID), personal identifiers (Play Games player ID, OPTIONAL).
# 6. "Shared = yes" is the conservative choice: AdMob, Firebase and Play Games send the data to Google. If you later decide Firebase is a pure service provider, change the two Analytics/Crashlytics types to "collected only" and re-import.
# 7. Not in this CSV, answer them by hand in the console: privacy policy URL (https://innocraftcode.github.io/Tidy-Tubes/privacy-policy/), Families policy = NO, independent security review = NO, Ads declaration = YES contains ads, Advertising ID = YES.
# 8. Regenerate this file every time an SDK is added or removed. Source of truth: data-safety-answers.md next to this file.
# 9. Before importing, export the current form from the console once and compare the header line: it must read exactly as line 1 below. If Google changed the template, re-base the answers on that export.
'@

$clean = Join-Path $OutDir 'data-safety-import.csv'
$noted = Join-Path $OutDir 'data-safety-import-with-notes.csv'
[IO.File]::WriteAllText($clean, $body, $utf8)
[IO.File]::WriteAllText($noted, ($notes -replace "`r?`n", "`r`n") + "`r`n" + $body, $utf8)

"rows written: $($rows.Count)"
"answers set:  $(($rows | Where-Object { $_.'Response value' -ne '' }).Count)"
$rows | Where-Object { $_.'Response value' -ne '' } | ForEach-Object { "{0} | {1} | {2}" -f $_.'Question ID (machine readable)', $_.'Response ID (machine readable)', $_.'Response value' }
