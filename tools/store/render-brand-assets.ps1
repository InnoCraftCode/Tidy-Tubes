<#
.SYNOPSIS
    Renders the Tidy Tubes brand assets with GDI+ (no external tools):
      play-store\<Game>\graphics\icon-512.png                  Play Store icon, 512x512, 32-bit PNG
      play-store\<Game>\graphics\feature-graphic-1024x500.png  24-bit PNG, no alpha
      play-store\<Game>\graphics\video-background-1920x1080.png backdrop of the landscape promo video
      play-store\<Game>\graphics\icon-preview-sheet.png        the icon at launcher sizes on light/dark
      games\<Game>\android\src\main\res\mipmap-*\ic_launcher_foreground.png  adaptive foreground (108 dp canvas)
      games\<Game>\android\src\main\res\mipmap-*\ic_launcher.png / ic_launcher_round.png  legacy launcher icons
.DESCRIPTION
    This script IS the editable source of the icon and feature graphic: change the composition
    here and rerun. Colours mirror core/.../ui/Palette.java. Fonts: Poppins (SIL OFL) from the
    game's assets folder, loaded privately (no installation needed).
.EXAMPLE
    .\tools\store\render-brand-assets.ps1 -Game sortpuzzle
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [string]$Title = 'Tidy Tubes',
    [string]$Tagline = 'Sort. Breathe. Repeat.',
    [string]$Strap = "Free  $([char]0x2022)  Plays offline  $([char]0x2022)  Endless levels"
)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$root = Get-WorkspaceRoot
$gameDir = Resolve-GameProject $Game
$outDir = Join-Path $root "play-store\$Game\graphics"
$resDir = Join-Path $gameDir 'android\src\main\res'
$fontDir = Join-Path $gameDir 'android\src\main\assets\fonts'
New-Item -ItemType Directory -Force $outDir | Out-Null

# ------------------------------------------------------------------ palette (Palette.java)
$BG_TOP = '2C3775'; $BG_BOTTOM = '121736'
$AURORA_A = '3FB7C9'; $AURORA_B = '7B5CE6'
$TEXT = 'F4F6FF'; $TEXT_DIM = 'A3AACB'; $PRIMARY = '4FE3C1'; $GOLD = 'FFC857'
$BALL = @{ coral = 'F2504B'; blue = '3B82F6'; green = '34C759'; yellow = 'FFD23F' }

function C([string]$hex, [int]$alpha = 255) {
    $v = [Convert]::ToInt32($hex.Substring(0, 6), 16)
    return [System.Drawing.Color]::FromArgb($alpha, ($v -shr 16) -band 255, ($v -shr 8) -band 255, $v -band 255)
}

# Private font collection: Poppins straight from the game's assets.
$fonts = New-Object System.Drawing.Text.PrivateFontCollection
foreach ($f in @('Poppins-ExtraBold.ttf', 'Poppins-SemiBold.ttf', 'Poppins-Medium.ttf')) {
    $p = Join-Path $fontDir $f
    if (Test-Path $p) { $fonts.AddFontFile($p) }
}
function Font([string]$family, [float]$px) {
    $fam = $fonts.Families | Where-Object { $_.Name -eq $family } | Select-Object -First 1
    if (-not $fam) { $fam = [System.Drawing.FontFamily]::new('Segoe UI') }
    $style = [System.Drawing.FontStyle]::Regular
    if (-not $fam.IsStyleAvailable($style)) { $style = [System.Drawing.FontStyle]::Bold }
    return New-Object System.Drawing.Font $fam, $px, $style, ([System.Drawing.GraphicsUnit]::Pixel)
}

function New-Canvas([int]$w, [int]$h, [bool]$alpha) {
    $format = if ($alpha) { [System.Drawing.Imaging.PixelFormat]::Format32bppArgb } else { [System.Drawing.Imaging.PixelFormat]::Format24bppRgb }
    $bmp = New-Object System.Drawing.Bitmap $w, $h, $format
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAlias'
    $g.InterpolationMode = 'HighQualityBicubic'
    $g.PixelOffsetMode = 'HighQuality'
    $g.CompositingQuality = 'HighQuality'
    if ($alpha) { $g.Clear([System.Drawing.Color]::Transparent) }
    return @{ Bitmap = $bmp; G = $g }
}

function Fill-Backdrop($g, [int]$w, [int]$h) {
    $brushRect = New-Object System.Drawing.Rectangle -1, -1, ($w + 2), ($h + 2)
    $grad = New-Object System.Drawing.Drawing2D.LinearGradientBrush $brushRect, (C $BG_TOP), (C $BG_BOTTOM), 90
    $grad.WrapMode = 'TileFlipXY'
    $g.FillRectangle($grad, 0, 0, $w, $h)
    $grad.Dispose()
    Draw-Glow $g ($w * 0.12) ($h * 0.95) ($w * 0.9) $AURORA_A 80
    Draw-Glow $g ($w * 0.95) ($h * 0.05) ($w * 0.8) $AURORA_B 70
}

function Draw-Glow($g, [float]$cx, [float]$cy, [float]$r, [string]$hex, [int]$alpha) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse($cx - $r, $cy - $r, 2 * $r, 2 * $r)
    $b = New-Object System.Drawing.Drawing2D.PathGradientBrush $path
    $b.CenterColor = C $hex $alpha
    $b.SurroundColors = @((C $hex 0))
    $g.FillPath($b, $path)
    $b.Dispose(); $path.Dispose()
}

function Draw-Ball($g, [float]$cx, [float]$cy, [float]$r, [string]$hex) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse($cx - $r, $cy - $r, 2 * $r, 2 * $r)
    $shade = New-Object System.Drawing.Drawing2D.PathGradientBrush $path
    $shade.CenterPoint = New-Object System.Drawing.PointF ($cx - $r * 0.32), ($cy - $r * 0.38)
    $base = C $hex
    $light = [System.Drawing.Color]::FromArgb(255, [Math]::Min(255, $base.R + 75), [Math]::Min(255, $base.G + 75), [Math]::Min(255, $base.B + 75))
    $shade.CenterColor = $light
    $shade.SurroundColors = @([System.Drawing.Color]::FromArgb(255, [int]($base.R * 0.62), [int]($base.G * 0.62), [int]($base.B * 0.62)))
    $shade.FocusScales = New-Object System.Drawing.PointF 0.25, 0.25
    $g.FillPath($shade, $path)
    $shade.Dispose(); $path.Dispose()
    $hl = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(120, 255, 255, 255))
    $g.FillEllipse($hl, $cx - $r * 0.58, $cy - $r * 0.66, $r * 0.5, $r * 0.34)
    $hl.Dispose()
}

# A glass tube: x,y = top-left of the bounding box, w = width, h = height (bottom is a semicircle).
function Draw-Tube($g, [float]$x, [float]$y, [float]$w, [float]$h, [string[]]$balls, [float]$rim = 0) {
    if ($rim -le 0) { $rim = [Math]::Max(1.5, $w * 0.055) }
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddLine($x, $y, $x, $y + $h - $w / 2)
    $path.AddArc($x, $y + $h - $w, $w, $w, 180, -180)
    $path.AddLine($x + $w, $y + $h - $w / 2, $x + $w, $y)
    # Contact shadow under the base.
    Draw-Glow $g ($x + $w / 2) ($y + $h) ($w * 0.9) '000000' 90
    $glass = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(30, 255, 255, 255))
    $g.FillPath($glass, $path)
    # Left-wall highlight.
    $state = $g.Save()
    $g.SetClip($path)
    $hlRect = New-Object System.Drawing.RectangleF ($x + $w * 0.08), $y, ($w * 0.2), $h
    $hlBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush $hlRect, ([System.Drawing.Color]::FromArgb(0, 255, 255, 255)), ([System.Drawing.Color]::FromArgb(70, 255, 255, 255)), 0
    $g.FillRectangle($hlBrush, $hlRect)
    $hlBrush.Dispose()
    $g.Restore($state)
    # Balls, bottom first.
    $r = ($w - 2 * $w * 0.13) / 2
    $cy = $y + $h - $w / 2
    foreach ($b in $balls) {
        Draw-Ball $g ($x + $w / 2) $cy $r $b
        $cy -= 2 * $r
    }
    $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(165, 255, 255, 255)), $rim
    $pen.LineJoin = 'Round'; $pen.StartCap = 'Round'; $pen.EndCap = 'Round'
    $g.DrawPath($pen, $path)
    $glass.Dispose(); $pen.Dispose(); $path.Dispose()
    return $r
}

# The icon composition in a box of 100 units centred on (cx, cy): one tube holding yellow and
# blue, a coral ball lifted above it. `scale` = pixels per unit.
function Draw-IconMark($g, [float]$cx, [float]$cy, [float]$scale) {
    $w = 36 * $scale; $h = 62 * $scale
    $x = $cx - $w / 2; $y = $cy - 18 * $scale
    $r = Draw-Tube $g $x $y $w $h @($BALL.yellow, $BALL.blue) ($scale * 2.2)
    # Lifted ball with a faint mint halo so it reads as "in motion".
    $lx = $cx; $ly = $y - $r - 7 * $scale
    Draw-Glow $g $lx $ly ($r * 2.3) $PRIMARY 70
    Draw-Ball $g $lx $ly $r $BALL.coral
}

# The brand mark used on the feature graphic and video backdrop: three tubes mid-sort.
function Draw-BrandMark($g, [float]$left, [float]$top, [float]$scale) {
    $w = 30 * $scale; $h = 56 * $scale; $gap = 12 * $scale
    $sets = @(@($BALL.blue, $BALL.coral, $BALL.blue), @($BALL.coral, $BALL.blue), @($BALL.green, $BALL.green, $BALL.green, $BALL.green))
    $y = $top + 16 * $scale
    for ($i = 0; $i -lt 3; $i++) {
        $x = $left + $i * ($w + $gap)
        if ($i -eq 2) { Draw-Glow $g ($x + $w / 2) ($y + $h / 2) ($w * 1.4) $GOLD 90 }
        $r = Draw-Tube $g $x $y $w $h $sets[$i]
        if ($i -eq 0) { Draw-Ball $g ($x + $w / 2) ($y - $r - 5 * $scale) $r $BALL.coral }
    }
}

function Save-Png($bitmap, [string]$path) {
    $dir = Split-Path $path -Parent
    New-Item -ItemType Directory -Force $dir | Out-Null
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Status OK ("{0}  {1}x{2}  {3:N0} KB" -f $path.Replace($root + '\', ''), $bitmap.Width, $bitmap.Height, ((Get-Item $path).Length / 1KB))
}

# --- Play Store icon: 512x512, 32-bit, full bleed (Play applies its own rounded mask). ---------
$icon = New-Canvas 512 512 $true
Fill-Backdrop $icon.G 512 512
Draw-IconMark $icon.G 256 264 4.4
Save-Png $icon.Bitmap (Join-Path $outDir 'icon-512.png')
$icon.G.Dispose(); $icon.Bitmap.Dispose()

# --- Adaptive foreground: 108 dp canvas, artwork inside the 66 dp safe zone. ------------------
$densities = @{ mdpi = 1.0; hdpi = 1.5; xhdpi = 2.0; xxhdpi = 3.0; xxxhdpi = 4.0 }
foreach ($d in $densities.Keys) {
    $px = [int](108 * $densities[$d])
    $fg = New-Canvas $px $px $true
    Draw-IconMark $fg.G ($px / 2) ($px / 2 + $px * 0.012) ($px * 0.0058)
    Save-Png $fg.Bitmap (Join-Path $resDir "mipmap-$d\ic_launcher_foreground.png")
    $fg.G.Dispose(); $fg.Bitmap.Dispose()

    # Legacy launcher icons (API 24-25): square with rounded corners, and round.
    $size = [int](48 * $densities[$d])
    foreach ($shape in @('ic_launcher', 'ic_launcher_round')) {
        $lg = New-Canvas $size $size $true
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        if ($shape -eq 'ic_launcher_round') {
            $path.AddEllipse(0, 0, $size - 1, $size - 1)
        } else {
            $rad = $size * 0.2
            $path.AddArc(0, 0, 2 * $rad, 2 * $rad, 180, 90)
            $path.AddArc($size - 2 * $rad - 1, 0, 2 * $rad, 2 * $rad, 270, 90)
            $path.AddArc($size - 2 * $rad - 1, $size - 2 * $rad - 1, 2 * $rad, 2 * $rad, 0, 90)
            $path.AddArc(0, $size - 2 * $rad - 1, 2 * $rad, 2 * $rad, 90, 90)
            $path.CloseFigure()
        }
        $lg.G.SetClip($path)
        Fill-Backdrop $lg.G $size $size
        Draw-IconMark $lg.G ($size / 2) ($size / 2 + $size * 0.012) ($size * 0.0082)
        $lg.G.ResetClip()
        Save-Png $lg.Bitmap (Join-Path $resDir "mipmap-$d\$shape.png")
        $path.Dispose(); $lg.G.Dispose(); $lg.Bitmap.Dispose()
    }
}

# --- Feature graphic: 1024x500, 24-bit, no alpha. Text and mark inside the central safe area. --
$fg = New-Canvas 1024 500 $false
Fill-Backdrop $fg.G 1024 500
Draw-BrandMark $fg.G 78 118 3.5
$titleFont = Font 'Poppins ExtraBold' 82
$tagFont = Font 'Poppins SemiBold' 32
$strapFont = Font 'Poppins Medium' 22
$white = New-Object System.Drawing.SolidBrush (C $TEXT)
$dim = New-Object System.Drawing.SolidBrush (C $TEXT_DIM)
$mint = New-Object System.Drawing.SolidBrush (C $PRIMARY)
$fg.G.DrawString($Title, $titleFont, $white, 508, 142)
$fg.G.DrawString($Tagline, $tagFont, $dim, 516, 250)
$fg.G.DrawString($Strap, $strapFont, $mint, 518, 306)
Save-Png $fg.Bitmap (Join-Path $outDir 'feature-graphic-1024x500.png')
$fg.G.Dispose(); $fg.Bitmap.Dispose()

# --- Promo-video backdrop: 1920x1080; gameplay is overlaid on the right by make-promo-video. ---
$vb = New-Canvas 1920 1080 $false
Fill-Backdrop $vb.G 1920 1080
Draw-BrandMark $vb.G 150 70 5.0
$bigTitle = Font 'Poppins ExtraBold' 140
$bigTag = Font 'Poppins SemiBold' 54
$bigStrap = Font 'Poppins Medium' 38
$vb.G.DrawString($Title, $bigTitle, $white, 130, 560)
$vb.G.DrawString($Tagline, $bigTag, $dim, 146, 750)
$vb.G.DrawString($Strap, $bigStrap, $mint, 150, 836)
Save-Png $vb.Bitmap (Join-Path $outDir 'video-background-1920x1080.png')
$vb.G.Dispose(); $vb.Bitmap.Dispose()

# --- Preview sheet: the icon at launcher sizes on dark and light, square and round. -----------
$sheet = New-Canvas 1200 620 $false
$sheet.G.Clear((C '1C2250'))
$lightBrush = New-Object System.Drawing.SolidBrush (C 'F2F3F7')
$sheet.G.FillRectangle($lightBrush, 600, 0, 600, 620)
$src512 = [System.Drawing.Image]::FromFile((Join-Path $outDir 'icon-512.png'))
$roundSrc = [System.Drawing.Image]::FromFile((Join-Path $resDir 'mipmap-xxxhdpi\ic_launcher_round.png'))
$labelFont = Font 'Poppins Medium' 18
foreach ($half in @(0, 600)) {
    $brush = if ($half -eq 0) { $white } else { New-Object System.Drawing.SolidBrush (C '1C2250') }
    $x = $half + 40
    foreach ($s in @(256, 192, 96, 48)) {
        # Play mask: rounded corners at 20 %.
        $p = New-Object System.Drawing.Drawing2D.GraphicsPath
        $rad = $s * 0.2
        $p.AddArc($x, 60, 2 * $rad, 2 * $rad, 180, 90); $p.AddArc($x + $s - 2 * $rad, 60, 2 * $rad, 2 * $rad, 270, 90)
        $p.AddArc($x + $s - 2 * $rad, 60 + $s - 2 * $rad, 2 * $rad, 2 * $rad, 0, 90); $p.AddArc($x, 60 + $s - 2 * $rad, 2 * $rad, 2 * $rad, 90, 90)
        $p.CloseFigure()
        $sheet.G.SetClip($p); $sheet.G.DrawImage($src512, $x, 60, $s, $s); $sheet.G.ResetClip()
        $sheet.G.DrawImage($roundSrc, $x, 360, $s, $s)
        $sheet.G.DrawString("$s px", $labelFont, $brush, $x, 60 + $s + 8)
        $x += $s + 24
        $p.Dispose()
    }
    $sheet.G.DrawString('Play Store icon (20% mask)', $labelFont, $brush, $half + 40, 24)
    $sheet.G.DrawString('Legacy round launcher icon', $labelFont, $brush, $half + 40, 330)
}
Save-Png $sheet.Bitmap (Join-Path $outDir 'icon-preview-sheet.png')
$src512.Dispose(); $roundSrc.Dispose(); $sheet.G.Dispose(); $sheet.Bitmap.Dispose()

$titleFont.Dispose(); $tagFont.Dispose(); $strapFont.Dispose(); $bigTitle.Dispose(); $bigTag.Dispose(); $bigStrap.Dispose()
$white.Dispose(); $dim.Dispose(); $mint.Dispose(); $lightBrush.Dispose(); $labelFont.Dispose(); $fonts.Dispose()
Write-Status INFO 'Remove the old mipmap-*\ic_launcher*.webp files if they still exist (duplicate resource names).'
