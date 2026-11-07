<#
.SYNOPSIS
    Cuts a marketing trailer from the scenes filmed by StoreCapture in trailer mode.
.DESCRIPTION
    Inputs:
      games\<Game>\lwjgl3\build\capture\trailer\<NN-scene>\00000.png ...  (30 fps, 1080x1920)
        produced by: gradlew :lwjgl3:storeCapture -PcaptureMode=trailer
      play-store\<Game>\graphics\icon-512.png                             (intro / outro)
      games\<Game>\android\src\main\assets\fonts\Poppins-*.ttf            (titles)
      -Music <wav/ogg>: the soundtrack (an original MusicSynth track, see game-music-composition)
    Each segment (intro, every scene with its animated title, outro) is rendered on an animated
    gradient in the game's palette, then the segments are chained with xfade transitions.
    Outputs in play-store\<Game>\video\:
      trailer-<lang>-portrait-1080x1920.mp4    Shorts / TikTok / Reels
      trailer-<lang>-landscape-1920x1080.mp4   Play listing (upload to YouTube, paste the link)
.EXAMPLE
    .\tools\store\make-trailer.ps1 -Game sortpuzzle -Music path\to\trailer.wav -Lang en
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Game,
    [Parameter(Mandatory)][string]$Music,
    [ValidateSet('en', 'fr')][string]$Lang = 'en',
    [ValidateSet('both', 'portrait', 'landscape')][string]$Format = 'both'
)

. "$PSScriptRoot\..\scripts\common.ps1"
$ErrorActionPreference = 'Stop'
$inv = [Globalization.CultureInfo]::InvariantCulture
function N([double]$v) { return $v.ToString('0.###', $inv) }

$ffmpeg = (Get-Command ffmpeg -ErrorAction SilentlyContinue).Source
if (-not $ffmpeg) {
    $hit = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter ffmpeg.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($hit) { $ffmpeg = $hit.FullName }
}
if (-not $ffmpeg) { Write-Status FAIL 'FFmpeg not found' 'winget install --id Gyan.FFmpeg -e'; exit 1 }

$root = Get-WorkspaceRoot
# Scenes filmed in the trailer's language when available (-PcaptureLang=fr -PcaptureOut=...trailer-fr).
$scenesDir = Join-Path $root "games\$Game\lwjgl3\build\capture\trailer-$Lang"
if (-not (Test-Path $scenesDir)) { $scenesDir = Join-Path $root "games\$Game\lwjgl3\build\capture\trailer" }
if (-not (Test-Path $scenesDir)) { Write-Status FAIL "No scenes in $scenesDir" "gradlew :lwjgl3:storeCapture -PcaptureMode=trailer"; exit 1 }
Write-Status INFO "Scenes: $scenesDir"
$Music = (Resolve-Path $Music).Path
$outDir = Join-Path $root "play-store\$Game\video"
New-Item -ItemType Directory -Force $outDir | Out-Null

# ---- Words. Title + one line per scene; scenes without a caption are skipped. ---------------
$copy = @{
    en = @{
        slogan = 'Sort. Breathe. Repeat.'
        cta1 = 'Free to play'; cta2 = 'DOWNLOAD NOW'
        '01-home' = @('Your daily calm', 'Daily puzzle, rewards and a world to explore')
        '02-solve' = @('Sort the colours', 'One colour per tube. Easy to learn.')
        '03-ten-colours' = @('Hundreds of levels', 'Up to 10 colours, hints when you need them')
        '04-shop' = @('400 styles to unlock', 'Balls, finishes, tubes and themes')
        '05-goals' = @('Goals and star road', 'Every star unlocks exclusive skins')
        '06-music' = @('Relaxing music', '10 calm loops to choose from')
        '07-languages' = @('Play in 8 languages', 'Arabic, French, Spanish, German and more')
    }
    fr = @{
        slogan = 'Trie. Respire. Recommence.'
        cta1 = 'Gratuit'; cta2 = 'TÉLÉCHARGE-LE'
        '01-home' = @('Ta pause zen du jour', 'Puzzle du jour, récompenses, un monde à explorer')
        '02-solve' = @('Trie les couleurs', 'Une couleur par tube. Simple à apprendre.')
        '03-ten-colours' = @('Des centaines de niveaux', "Jusqu'à 10 couleurs, des indices au besoin")
        '04-shop' = @('400 styles à débloquer', 'Billes, finitions, tubes et thèmes')
        '05-goals' = @('Objectifs et route des étoiles', 'Chaque étoile débloque des styles exclusifs')
        '06-music' = @('Musique relaxante', '10 boucles apaisantes au choix')
        '07-languages' = @('Joue en 8 langues', 'Arabe, anglais, espagnol, allemand et plus')
    }
}[$Lang]

$scenes = Get-ChildItem $scenesDir -Directory | Sort-Object Name | Where-Object { $copy.ContainsKey($_.Name) }
if ($scenes.Count -eq 0) { Write-Status FAIL "No captioned scenes in $scenesDir"; exit 1 }

$work = Join-Path $scenesDir "..\trailer-work-$Lang"
New-Item -ItemType Directory -Force $work | Out-Null
$work = (Resolve-Path $work).Path
$fonts = Join-Path $root "games\$Game\android\src\main\assets\fonts"
Copy-Item (Join-Path $fonts 'Poppins-ExtraBold.ttf') (Join-Path $work 'title.ttf') -Force
Copy-Item (Join-Path $fonts 'Poppins-SemiBold.ttf') (Join-Path $work 'line.ttf') -Force
Copy-Item (Join-Path $root "play-store\$Game\graphics\icon-512.png") (Join-Path $work 'icon.png') -Force
$utf8 = New-Object Text.UTF8Encoding($false)
function Write-Text([string]$name, [string]$text) { [IO.File]::WriteAllText((Join-Path $work $name), $text, $utf8) }

function Invoke-Ff([string[]]$ffArgs, [string]$graph) {
    [IO.File]::WriteAllText((Join-Path $work 'graph.txt'), $graph, $utf8)
    Push-Location $work
    try {
        & $ffmpeg -hide_banner -loglevel error -y @ffArgs
        if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed: $($ffArgs -join ' ')" }
    } finally { Pop-Location }
}

# Rounded icon, once (Play's 512 icon is full-bleed square).
Invoke-Ff @('-i', 'icon.png', '-/filter_complex', 'graph.txt', '-map', '[o]', '-frames:v', '1', 'icon-round.png') `
    "[0:v]format=rgba,geq=r='r(X,Y)':g='g(X,Y)':b='b(X,Y)':a='255*lte(hypot(max(0,abs(X-256)-146),max(0,abs(Y-256)-146)),110)'[o]"

$fade = 0.45
$transitions = @('smoothup', 'slideleft', 'circleopen', 'wipeleft', 'smoothleft', 'radial', 'slideup', 'fadeblack', 'fade')
$encode = @('-c:v', 'libx264', '-preset', 'medium', '-crf', '15', '-pix_fmt', 'yuv420p', '-r', '30')

function Get-Background([int]$w, [int]$h, [double]$d) {
    return "gradients=s=${w}x${h}:r=30:d=$(N $d):c0=0x2B3472:c1=0x0B0E24:nb_colors=2:x0=$([int]($w*0.3)):y0=0:x1=$([int]($w*0.7)):y1=${h}:speed=0.004"
}

# Pop: 0 -> 112 % -> 100 %, then a slow float.
$popScale = "if(lt(t,0.15),0.02,if(lt(t,0.5),0.02+1.1*(t-0.15)/0.35,if(lt(t,0.75),1.12-0.12*(t-0.5)/0.25,1)))"
$slide = "pow(max(0,1-t/0.4),3)"   # 1 -> 0 ease-out over 0.4 s

# Shrinks a line that would not fit: Poppins averages ~0.57 em per character, 0.6 keeps a margin.
function Fit([string]$text, [int]$size, [int]$avail) {
    return [int][Math]::Min($size, [Math]::Floor($avail / (0.6 * $text.Length)))
}

function New-Segments([string]$fmt) {
    $landscape = $fmt -eq 'landscape'
    if ($landscape) { $W = 1920; $H = 1080 } else { $W = 1080; $H = 1920 }
    $files = New-Object System.Collections.Generic.List[string]
    $durations = New-Object System.Collections.Generic.List[double]

    # Layout: portrait = caption band on top, gameplay below; landscape = text left, phone right.
    if ($landscape) {
        $gh = 980; $gw = [int][Math]::Round(1080 * $gh / 1920 / 2) * 2
        $gx = $W - $gw - 190; $gy = [int](($H - $gh) / 2) - 4
        $tx = "($([int]($gx / 2 + 20))-text_w/2)"; $titleY = 410; $lineY = 530; $titleSize = 92; $lineSize = 42
        $avail = $gx - 100
    } else {
        $gh = 1490; $gw = [int][Math]::Round(1080 * $gh / 1920 / 2) * 2
        $gx = [int](($W - $gw) / 2) - 4; $gy = $H - $gh - 56
        $tx = '(w-text_w)/2'; $titleY = 118; $lineY = 232; $titleSize = 86; $lineSize = 40
        $avail = $W - 100
    }

    # ---- Intro / outro: icon pop, title, slogan or call to action. --------------------------
    foreach ($kind in @('intro', 'outro')) {
        $d = if ($kind -eq 'intro') { 3.2 } else { 4.0 }
        $iconSize = if ($landscape) { 340 } else { 440 }
        $box = $iconSize + 60
        $iconY = if ($landscape) { 150 } else { 520 }
        $nameY = $iconY + $box + 10
        $nameSize = if ($landscape) { 104 } else { 124 }
        Write-Text "$kind-1.txt" 'Tidy Tubes'
        Write-Text "$kind-2.txt" $(if ($kind -eq 'intro') { $copy.slogan } else { $copy.cta1 })
        $g = "[1:v]scale=w='max(2,$iconSize*($popScale))':h=-1:eval=frame," +
             "pad=w=${box}:h=${box}:x='(ow-iw)/2':y='(oh-ih)/2':color=0x00000000:eval=frame[i];" +
             "[0:v][i]overlay=x=(W-w)/2:y='$iconY+if(gt(t,0.75),10*sin((t-0.75)*2.2),0)':shortest=1," +
             "drawtext=fontfile=title.ttf:textfile=$kind-1.txt:fontsize=${nameSize}:fontcolor=white:x=(w-text_w)/2:" +
             "y='$nameY+40*pow(max(0,1-(t-0.55)/0.4),3)':alpha='clip((t-0.55)/0.35,0,1)':shadowcolor=black@0.45:shadowy=6," +
             "drawtext=fontfile=line.ttf:textfile=$kind-2.txt:fontsize=$([int]($nameSize*0.42)):fontcolor=0x4FE3C1:x=(w-text_w)/2:" +
             "y=$($nameY + [int]($nameSize*1.35)):alpha='clip((t-0.9)/0.35,0,1)'"
        if ($kind -eq 'outro') {
            Write-Text 'outro-3.txt' $copy.cta2
            $g += ",drawtext=fontfile=title.ttf:textfile=outro-3.txt:fontsize=$([int]($nameSize*0.4)):fontcolor=0x05221B:" +
                  "box=1:boxcolor=0x4FE3C1:boxborderw=26|46:x=(w-text_w)/2:y=$($nameY + [int]($nameSize*2.25)):" +
                  "alpha='clip((t-1.3)/0.3,0,1)'"
        }
        $g += ",format=yuv420p[v]"
        $out = "seg-$fmt-$kind.mp4"
        Invoke-Ff (@('-f', 'lavfi', '-i', (Get-Background $W $H $d), '-loop', '1', '-framerate', '30', '-t', (N $d), '-i', 'icon-round.png',
            '-/filter_complex', 'graph.txt', '-map', '[v]', '-t', (N $d)) + $encode + @($out)) $g
        if ($kind -eq 'intro') { $files.Add($out); $durations.Add($d) } else { $outro = $out; $outroD = $d }
    }

    # ---- Scenes: gameplay slides up, title and line fade in. ---------------------------------
    $i = 0
    foreach ($scene in $scenes) {
        $i++
        $count = @(Get-ChildItem $scene.FullName -Filter *.png).Count
        $d = $count / 30.0
        Write-Text "s$i-1.txt" $copy[$scene.Name][0]
        Write-Text "s$i-2.txt" $copy[$scene.Name][1]
        $pattern = (Join-Path $scene.FullName '%05d.png')
        $ts = Fit $copy[$scene.Name][0] $titleSize $avail
        $ls = Fit $copy[$scene.Name][1] $lineSize $avail
        $g = "[1:v]scale=${gw}:${gh},pad=iw+8:ih+8:4:4:color=0x5A64A0[g];" +
             "[0:v][g]overlay=x=${gx}:y='$gy+160*$slide':shortest=1," +
             "drawtext=fontfile=title.ttf:textfile=s$i-1.txt:fontsize=${ts}:fontcolor=white:x=${tx}:" +
             "y='$titleY+36*$slide':alpha='min(1,t/0.35)':shadowcolor=black@0.45:shadowy=5," +
             "drawtext=fontfile=line.ttf:textfile=s$i-2.txt:fontsize=${ls}:fontcolor=0x4FE3C1:x=${tx}:" +
             "y=${lineY}:alpha='clip((t-0.2)/0.35,0,1)',format=yuv420p[v]"
        $out = "seg-$fmt-s$i.mp4"
        Invoke-Ff (@('-f', 'lavfi', '-i', (Get-Background $W $H $d), '-framerate', '30', '-i', $pattern,
            '-/filter_complex', 'graph.txt', '-map', '[v]', '-t', (N $d)) + $encode + @($out)) $g
        $files.Add($out); $durations.Add($d)
    }
    $files.Add($outro); $durations.Add($outroD)
    return @{ files = $files; durations = $durations }
}

function New-Trailer([string]$fmt) {
    Write-Status INFO "Rendering $fmt segments"
    $seg = New-Segments $fmt
    $files = $seg.files; $durations = $seg.durations
    $inputs = @()
    foreach ($f in $files) { $inputs += @('-i', $f) }
    $g = ''
    $prev = '[0:v]'
    $offset = 0.0
    for ($k = 1; $k -lt $files.Count; $k++) {
        $offset += $durations[$k - 1] - $fade
        $label = if ($k -eq $files.Count - 1) { '[vx]' } else { "[x$k]" }
        $g += "$prev[${k}:v]xfade=transition=$($transitions[($k - 1) % $transitions.Count]):duration=$(N $fade):offset=$(N $offset)$label;"
        $prev = $label
    }
    $total = $offset + $durations[$files.Count - 1]
    $m = $files.Count
    $g += "[vx]fade=t=out:st=$(N ($total - 0.6)):d=0.6[v];" +
          "[${m}:a]aformat=sample_rates=48000:channel_layouts=stereo,atrim=0:$(N $total),afade=t=in:d=0.4,afade=t=out:st=$(N ($total - 2.2)):d=2.2,volume=0.9[a]"
    $out = Join-Path $outDir "trailer-$Lang-$fmt-$(if ($fmt -eq 'landscape') { '1920x1080' } else { '1080x1920' }).mp4"
    Invoke-Ff ($inputs + @('-stream_loop', '-1', '-i', $Music, '-/filter_complex', 'graph.txt', '-map', '[v]', '-map', '[a]') +
        $encode + @('-c:a', 'aac', '-b:a', '192k', '-movflags', '+faststart', '-t', (N $total), $out)) $g
    Write-Status OK ("{0}  ({1:N1} MB, {2} s)" -f $out, ((Get-Item $out).Length / 1MB), (N $total))
}

if ($Format -ne 'landscape') { New-Trailer 'portrait' }
if ($Format -ne 'portrait') { New-Trailer 'landscape' }
