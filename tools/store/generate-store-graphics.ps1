<#
.SYNOPSIS
    Draws draft Google Play graphics for a game: app icon (512x512, 32-bit PNG), feature graphic
    (1024x500, 24-bit PNG, no alpha) and the promo-video backdrop (1920x1080) into
    play-store\<Game>\graphics\.
.DESCRIPTION
    Drafts, generated from the game's colours so a listing is never blocked on art. Replace them
    with designed artwork whenever you have it (same file names, same sizes).
.PARAMETER Motif
    Tubes   - glass tubes with coloured balls (sort puzzles)
    Circles - scattered coloured discs (any casual game)
.EXAMPLE
    .\tools\store\generate-store-graphics.ps1 -Game sortpuzzle -Title "Ball Sort" -Motif Tubes `
        -Background 1E2749,0B0F22 -Colors E8453C,2E86DE,2ECC71,F5C518,9B59B6,F07B24
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [Parameter(Mandatory)][string]$Title,
    [string]$Subtitle = '',
    [ValidateSet('Tubes', 'Circles')][string]$Motif = 'Circles',
    [string[]]$Background = @('1E2749', '0B0F22'),
    [string[]]$Colors = @('E8453C', '2E86DE', '2ECC71', 'F5C518', '9B59B6', 'F07B24'),
    [switch]$Force
)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# "-File script.ps1 -Colors A,B" arrives as one string; accept both forms.
$Background = @($Background | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
$Colors = @($Colors | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
$outDir = Join-Path (Get-WorkspaceRoot) "play-store\$Game\graphics"
New-Item -ItemType Directory -Force $outDir | Out-Null

function C([string]$hex, [int]$alpha = 255) {
    $v = [Convert]::ToInt32($hex.Substring(0, 6), 16)
    return [System.Drawing.Color]::FromArgb($alpha, ($v -shr 16) -band 255, ($v -shr 8) -band 255, $v -band 255)
}

function New-Canvas([int]$w, [int]$h, [System.Drawing.Imaging.PixelFormat]$format) {
    $bmp = New-Object System.Drawing.Bitmap $w, $h, $format
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $g.InterpolationMode = 'HighQualityBicubic'
    $rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
    # Brush rectangle 2 px larger than the canvas: GDI+ otherwise wraps the gradient at the edges.
    $brushRect = New-Object System.Drawing.Rectangle -1, -1, ($w + 2), ($h + 2)
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush $brushRect, (C $Background[0]), (C $Background[-1]), 90
    $grad.WrapMode = 'TileFlipXY'
    $g.FillRectangle($grad, $rect)
    $grad.Dispose()
    return @{ Bitmap = $bmp; G = $g }
}

function Draw-Ball($g, [float]$cx, [float]$cy, [float]$r, [string]$hex) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse($cx - $r, $cy - $r, 2 * $r, 2 * $r)
    $shade = New-Object System.Drawing.Drawing2D.PathGradientBrush $path
    $shade.CenterPoint = New-Object System.Drawing.PointF ($cx - $r * 0.35), ($cy - $r * 0.35)
    $base = C $hex
    $light = [System.Drawing.Color]::FromArgb(255, [Math]::Min(255, $base.R + 70), [Math]::Min(255, $base.G + 70), [Math]::Min(255, $base.B + 70))
    $shade.CenterColor = $light
    $shade.SurroundColors = @([System.Drawing.Color]::FromArgb(255, [int]($base.R * 0.75), [int]($base.G * 0.75), [int]($base.B * 0.75)))
    $g.FillPath($shade, $path)
    $shade.Dispose(); $path.Dispose()
    $hl = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(110, 255, 255, 255))
    $g.FillEllipse($hl, $cx - $r * 0.55, $cy - $r * 0.62, $r * 0.55, $r * 0.38)
    $hl.Dispose()
}

function Draw-Tube($g, [float]$x, [float]$y, [float]$w, [float]$h, [string[]]$balls) {
    $r = $w * 0.38
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddLine($x, $y, $x, $y + $h - $w / 2)
    $path.AddArc($x, $y + $h - $w, $w, $w, 180, -180)
    $path.AddLine($x + $w, $y + $h - $w / 2, $x + $w, $y)
    $glass = New-Object System.Drawing.SolidBrush (C 'FFFFFF' 28)
    $g.FillPath($glass, $path)
    $pen = New-Object System.Drawing.Pen ((C 'FFFFFF' 110)), ($w * 0.06)
    $g.DrawPath($pen, $path)
    $glass.Dispose(); $pen.Dispose(); $path.Dispose()
    $cy = $y + $h - $w / 2
    foreach ($b in $balls) {
        Draw-Ball $g ($x + $w / 2) $cy $r $b
        $cy -= 2 * $r + $w * 0.06
    }
}

function Draw-Motif($g, [float]$left, [float]$top, [float]$width, [float]$height) {
    if ($Motif -eq 'Tubes') {
        $n = 3
        $tubeW = $width / ($n * 1.7)
        $tubeH = [Math]::Min($height, $tubeW * 4.6)
        $gap = ($width - $n * $tubeW) / ($n + 1)
        $sets = @(
            @($Colors[0], $Colors[1], $Colors[0], $Colors[2]),
            @($Colors[1 % $Colors.Count], $Colors[2 % $Colors.Count], $Colors[1 % $Colors.Count]),
            @($Colors[3 % $Colors.Count], $Colors[3 % $Colors.Count], $Colors[3 % $Colors.Count], $Colors[3 % $Colors.Count])
        )
        for ($i = 0; $i -lt $n; $i++) {
            $tx = $left + $gap + $i * ($tubeW + $gap)
            Draw-Tube $g $tx ($top + ($height - $tubeH) / 2) $tubeW $tubeH $sets[$i]
        }
    } else {
        $rand = New-Object System.Random 7
        for ($i = 0; $i -lt 14; $i++) {
            $r = $height * (0.06 + $rand.NextDouble() * 0.08)
            Draw-Ball $g ($left + $rand.NextDouble() * $width) ($top + $rand.NextDouble() * $height) $r $Colors[$i % $Colors.Count]
        }
    }
}

function Save-Png($canvas, [string]$name) {
    $path = Join-Path $outDir $name
    if ((Test-Path $path) -and -not $Force) { Write-Status WARN "$name exists - kept (use -Force to overwrite)"; return }
    $canvas.Bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Status OK ("{0}  {1}x{2}  {3:N0} KB" -f $path, $canvas.Bitmap.Width, $canvas.Bitmap.Height, ((Get-Item $path).Length / 1KB))
}

# --- App icon: 512x512, 32-bit PNG. Full-bleed; Play applies the rounded mask itself. ----------
$icon = New-Canvas 512 512 ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Draw-Motif $icon.G 56 60 400 392
Save-Png $icon 'icon-512.png'
$icon.G.Dispose(); $icon.Bitmap.Dispose()

# --- Feature graphic: 1024x500, 24-bit PNG (no alpha). Keep text away from the edges. ---------
$fg = New-Canvas 1024 500 ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
Draw-Motif $fg.G 560 40 420 420
$titleFont = New-Object System.Drawing.Font 'Segoe UI', 64, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$subFont = New-Object System.Drawing.Font 'Segoe UI', 30, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$white = New-Object System.Drawing.SolidBrush (C 'EDF1FF')
$dim = New-Object System.Drawing.SolidBrush (C '8E98BC')
$fg.G.DrawString($Title, $titleFont, $white, 70, 170)
if ($Subtitle) { $fg.G.DrawString($Subtitle, $subFont, $dim, 74, 260) }
Save-Png $fg 'feature-graphic-1024x500.png'
$fg.G.Dispose(); $fg.Bitmap.Dispose()

# --- Promo-video backdrop: 1920x1080, title on the left; gameplay is overlaid on the right. ---
$vb = New-Canvas 1920 1080 ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
for ($i = 0; $i -lt [Math]::Min(5, $Colors.Count); $i++) { Draw-Ball $vb.G (190 + $i * 92) 330 36 $Colors[$i] }
$bigTitle = New-Object System.Drawing.Font 'Segoe UI', 132, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$bigSub = New-Object System.Drawing.Font 'Segoe UI', 56, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$vb.G.DrawString($Title, $bigTitle, $white, 132, 400)
if ($Subtitle) { $vb.G.DrawString($Subtitle, $bigSub, $dim, 142, 580) }
Save-Png $vb 'video-background-1920x1080.png'
$bigTitle.Dispose(); $bigSub.Dispose(); $vb.G.Dispose(); $vb.Bitmap.Dispose()
$titleFont.Dispose(); $subFont.Dispose(); $white.Dispose(); $dim.Dispose()
