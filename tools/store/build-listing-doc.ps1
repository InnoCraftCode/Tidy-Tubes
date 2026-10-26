<#
.SYNOPSIS
    Builds play-store\<Game>\<AppName> - Fiche Google Play.docx from listing.json, with the
    graphics, screenshot inventory and character-limit checks. Needs Microsoft Word installed.
.EXAMPLE
    .\tools\store\build-listing-doc.ps1 -Game sortpuzzle
.NOTES
    listing.json is the source; rerun after editing it. Saved as UTF-8 with BOM (French labels).
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [string]$PdfPreview   # optional: also export a PDF here (used to check the layout)
)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$dir = Join-Path (Get-WorkspaceRoot) "play-store\$Game"
$jsonPath = Join-Path $dir 'listing.json'
if (-not (Test-Path $jsonPath)) { throw "Missing $jsonPath (run tools\store\new-store-folder.ps1 first)" }
$L = [IO.File]::ReadAllText($jsonPath, [Text.Encoding]::UTF8) | ConvertFrom-Json

# Fail early (before starting Word) if the user has the document open.
$target = Join-Path $dir ("{0} - Fiche Google Play.docx" -f $L.appName)
if (Test-Path $target) {
    try { $lock = [IO.File]::Open($target, 'Open', 'ReadWrite', 'None'); $lock.Close() }
    catch { Write-Status FAIL "Le document est ouvert dans Word : $target" 'Fermez-le dans Word, puis relancez cette commande.'; exit 1 }
}

# ------------------------------------------------------------------ checks (Google Play limits)
$limits = @{ title = 30; shortDescription = 80; fullDescription = 4000; releaseNotes = 500 }
$problems = @()
foreach ($lang in $L.languages) {
    $lang | Add-Member -NotePropertyName fullText -NotePropertyValue (($lang.fullDescription) -join "`n") -Force
    foreach ($f in @('title', 'shortDescription', 'releaseNotes')) {
        if ($lang.$f.Length -gt $limits[$f]) { $problems += "$($lang.code) $f : $($lang.$f.Length)/$($limits[$f])" }
    }
    if ($lang.fullText.Length -gt 4000) { $problems += "$($lang.code) fullDescription : $($lang.fullText.Length)/4000" }
}
if ($problems) { $problems | ForEach-Object { Write-Status FAIL "Trop long : $_" }; exit 1 }

function Get-ImageInfo([string]$path) {
    if (-not (Test-Path $path)) { return $null }
    $img = [System.Drawing.Image]::FromFile($path)
    try { return @{ W = $img.Width; H = $img.Height; Bits = [System.Drawing.Image]::GetPixelFormatSize($img.PixelFormat) } } finally { $img.Dispose() }
}
$icon = Join-Path $dir 'graphics\icon-512.png'
$feature = Join-Path $dir 'graphics\feature-graphic-1024x500.png'
$iconInfo = Get-ImageInfo $icon
$featureInfo = Get-ImageInfo $feature
$shots = @(Get-ChildItem (Join-Path $dir 'screenshots\phone') -File -Include *.png, *.jpg, *.jpeg -Recurse -ErrorAction SilentlyContinue | Sort-Object Name)

# ------------------------------------------------------------------ Word helpers
$word = New-Object -ComObject Word.Application
$word.Visible = $false
$word.DisplayAlerts = 0   # wdAlertsNone: a hidden dialog would otherwise hang automation
$doc = $word.Documents.Add()
$sel = $word.Selection
$wdHeading1 = -2; $wdHeading2 = -3; $wdNormal = -1; $wdTitle = -63

function Para([string]$text, [int]$style = $wdNormal, [switch]$Bold) {
    $sel.Style = $doc.Styles.Item($style)
    $sel.Font.Bold = [int][bool]$Bold
    $sel.TypeText($text)
    $sel.TypeParagraph()
    $sel.Font.Bold = 0
}

function Table([object[][]]$rows, [double[]]$widthsCm) {
    $sel.Style = $doc.Styles.Item($wdNormal)
    $t = $doc.Tables.Add($sel.Range, $rows.Count, $rows[0].Count)
    $t.Borders.Enable = 1
    for ($r = 0; $r -lt $rows.Count; $r++) {
        for ($c = 0; $c -lt $rows[$r].Count; $c++) { $t.Cell($r + 1, $c + 1).Range.Text = [string]$rows[$r][$c] }
    }
    for ($c = 0; $c -lt $widthsCm.Count; $c++) { $t.Columns.Item($c + 1).Width = $word.CentimetersToPoints($widthsCm[$c]) }
    $t.Rows.Item(1).Range.Font.Bold = 1
    $t.Rows.Item(1).Shading.BackgroundPatternColor = 15652797   # light blue (BGR)
    $sel.EndKey(6) | Out-Null   # wdStory: continue after the table
    $sel.TypeParagraph()
}

function Picture([string]$path, [double]$widthCm) {
    $shape = $sel.InlineShapes.AddPicture($path)
    $ratio = $shape.Height / $shape.Width
    $shape.Width = $word.CentimetersToPoints($widthCm)
    $shape.Height = $shape.Width * $ratio
    $sel.TypeParagraph()
}

function Status($ok) { if ($ok) { 'OK' } else { 'À FAIRE' } }

try {
    # -------------------------------------------------------------- page 1: identity
    Para "$($L.appName) - Fiche Google Play" $wdTitle
    Para ("Généré le {0} depuis play-store\{1}\listing.json. Copiez chaque champ tel quel dans la Play Console." -f (Get-Date -Format 'dd/MM/yyyy'), $Game)

    Para 'Informations générales' $wdHeading1
    Table @(
        , @('Champ', 'Valeur')
        , @('Nom de l''application', $L.appName)
        , @('Package (applicationId)', $L.packageId)
        , @('Version', "$($L.versionName) (versionCode $($L.versionCode))")
        , @('Catégorie', $L.category)
        , @('Tags', ($L.tags -join ', '))
        , @('E-mail de contact', $L.contactEmail)
        , @('Site web', $L.website)
        , @('Politique de confidentialité', $L.privacyPolicyUrl)
        , @('Contient des annonces', $L.containsAds)
        , @('Classification du contenu', $L.contentRating)
        , @('Public cible', $L.targetAudience)
    ) @(4.8, 10.8)

    # -------------------------------------------------------------- graphics
    Para 'Éléments graphiques' $wdHeading1
    $iconOk = $iconInfo -and $iconInfo.W -eq 512 -and $iconInfo.H -eq 512
    $featureOk = $featureInfo -and $featureInfo.W -eq 1024 -and $featureInfo.H -eq 500 -and $featureInfo.Bits -le 24
    $shotsOk = $shots.Count -ge 2
    Table @(
        , @('Élément', 'Exigence Google Play', 'Fichier', 'État')
        , @('Icône', '512 x 512 px, PNG 32 bits, 1 Mo max', 'graphics\icon-512.png', (Status $iconOk))
        , @('Image de présentation', '1024 x 500 px, PNG 24 bits ou JPEG, sans transparence', 'graphics\feature-graphic-1024x500.png', (Status $featureOk))
        , @('Captures téléphone', '2 à 8 images, 16:9 ou 9:16, 320 à 3840 px par côté (recommandé : 4+ en 1080 x 1920)', 'screenshots\phone\', ("{0} ({1} trouvée(s))" -f (Status $shotsOk), $shots.Count))
        , @('Vidéo promo', 'Lien YouTube (public ou non répertorié, sans pub). Paysage 16:9 recommandé.', 'video\promo-landscape-1920x1080.mp4', $(if (Test-Path (Join-Path $dir 'video\promo-landscape-1920x1080.mp4')) { 'OK - à mettre sur YouTube' } else { 'Facultatif' }))
    ) @(3, 5.4, 4.6, 2.6)

    if ($iconInfo) { Para 'Icône' $wdHeading2; Picture $icon 4 }
    if ($featureInfo) { Para 'Image de présentation' $wdHeading2; Picture $feature 15 }
    Para 'Les visuels générés sont des brouillons aux couleurs du jeu : remplacez-les par des créations définitives quand vous les avez (mêmes noms, mêmes tailles).'

    Para 'Captures d''écran à réaliser' $wdHeading2
    Para 'Générez-les sans téléphone avec tools\store\capture-desktop.ps1 (jeu joué automatiquement, 1080 x 1920), ou depuis un téléphone/émulateur avec tools\store\capture-screenshot.ps1. Ordre attendu :'
    $captionRows = @(, @('Ordre / nom', 'Fichier présent ?'))
    foreach ($cap in $L.screenshotCaptions) {
        $prefix = ($cap -split ' ')[0]
        $match = $shots | Where-Object { $_.Name -like "$prefix*" } | Select-Object -First 1
        $captionRows += , @($cap, $(if ($match) { $match.Name } else { '-' }))
    }
    Table $captionRows @(10, 6)

    # -------------------------------------------------------------- one section per language
    foreach ($lang in $L.languages) {
        $sel.InsertBreak(7)   # wdPageBreak
        Para "Fiche du Store - $($lang.name) ($($lang.code))" $wdHeading1
        Table @(
            , @('Champ', 'Texte', 'Caractères')
            , @('Titre', $lang.title, "$($lang.title.Length) / 30")
            , @('Description courte', $lang.shortDescription, "$($lang.shortDescription.Length) / 80")
        ) @(3.3, 9.5, 2.8)
        Para ("Description complète ({0} / 4000 caractères)" -f $lang.fullText.Length) $wdHeading2
        foreach ($line in $lang.fullDescription) { Para $line }
        Para ("Notes de version ({0} / 500 caractères)" -f $lang.releaseNotes.Length) $wdHeading2
        Para $lang.releaseNotes
    }

    # -------------------------------------------------------------- checklist
    $sel.InsertBreak(7)
    Para 'Avant la publication' $wdHeading1
    foreach ($item in @(
        'AAB signé avec la clé d''upload (build-android.ps1 -Task Bundle)',
        'targetSdk 36 (exigé par Google Play depuis le 31/08/2026)',
        'Politique de confidentialité en ligne et URL renseignée',
        'Formulaire « Sécurité des données » (voir shared\privacy\data-safety-worksheet.md)',
        'Déclaration « Contient des annonces » et identifiant publicitaire',
        'Questionnaire de classification du contenu (IARC)',
        'Public cible et contenu',
        'Test interne, puis test fermé (12 testeurs pendant 14 jours pour un nouveau compte personnel)',
        'Checklist complète : config\release-checklists\google-play-release.md'
    )) { Para ("[  ] " + $item) }

    # [string] matters: Join-Path returns a PSObject-wrapped string, and passing that to Word's
    # COM methods hung SaveAs2 indefinitely.
    [string]$out = Join-Path $dir ("{0} - Fiche Google Play.docx" -f $L.appName)
    $doc.SaveAs2($out, 16)   # wdFormatXMLDocument
    Write-Status OK $out
    if ($PdfPreview) { $doc.ExportAsFixedFormat([string]$PdfPreview, 17); Write-Status INFO "PDF preview: $PdfPreview" }
    if (-not $iconOk -or -not $featureOk) { Write-Status WARN 'Graphics missing or wrong size: run tools\store\generate-store-graphics.ps1' }
    if (-not $shotsOk) { Write-Status WARN "Only $($shots.Count) phone screenshot(s); Play needs at least 2 (tools\store\capture-screenshot.ps1)" }
} finally {
    $doc.Close(0)   # wdDoNotSaveChanges (already saved above)
    $word.Quit()
    [void][Runtime.InteropServices.Marshal]::ReleaseComObject($word)
}
