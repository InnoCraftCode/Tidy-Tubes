# Licensing, encoding, budgets and release checks for game audio

## Licensing: what may ship

| Source | Allowed | Proof to keep |
|---|---|---|
| Synthesised here (MusicSynth, SfxSynth) | yes, unconditionally | the "regenerate with" line in `assets/sfx/README.txt` |
| Composed by the user or a contractor | yes, with a written assignment or licence covering commercial apps | `play-store/<game>/licences.md` |
| CC0 / public domain | yes | URL + date + a copy of the licence text |
| CC-BY | only if the credit is shown in the game (Settings > Credits) and the store listing | the credit text, where it appears |
| CC-BY-SA, CC-NC, "free for personal use" | **no** (share-alike and non-commercial are incompatible with an ad-funded app) | - |
| Royalty-free packs (itch.io, Humble, asset stores) | yes if the licence names "commercial projects" and does not cap downloads/revenue | invoice + licence PDF |
| AI-generated music services | only if their terms grant commercial rights and no exclusivity conflicts; prefer regenerable local synthesis | export of the terms at the date of generation |
| Anything "sounds like" a known song, cover, remix, sample of a recording | **no**: melodies and recordings are protected independently | - |
| Recorded voice lines | yes with the speaker's written consent, and no identifiable child voice | consent note |

Google Play consequences: a copyright complaint suspends the app and counts against the
developer account; three strikes close it. The whole reason this skill defaults to synthesis is
that nothing it produces can be claimed by anyone.

## Encoding with FFmpeg

`scripts/encode-music.ps1` wraps this; the manual equivalent is:

```powershell
ffmpeg -y -i music_01.wav -ac 1 -ar 22050 -c:a libvorbis -q:a 3 music_01.ogg
ffmpeg -i music_01.ogg -af volumedetect -f null NUL      # prints mean_volume / max_volume
```

| Setting | Value | Why |
|---|---|---|
| Codec | Vorbis (`libvorbis`) | libGDX decodes it on every backend; Android MediaPlayer plays it; no patent or licence |
| Quality | `-q:a 3` (~96 kb/s mono) | Transparent for synthesised beds; q 2 for drones, q 4 for bright chiptune |
| Channels | mono (`-ac 1`) | Half the size, identical on phone speakers; stereo only for a bed that uses pan |
| Sample rate | 22 050 Hz beds, 44 100 Hz effects | Beds have nothing above 8 kHz after the low-pass |
| Loop tail | none | Vorbis adds no priming samples that libGDX cannot handle, but a WAV with a reverb tail past the loop end will click: wrap it in the generator or trim it |
| MP3 | never | Encoder/decoder delay (gapless is unreliable), bigger, no advantage |

Expected output: a 28 s synthesised ambient bed at q 3 is 40-50 KB (Tidy Tubes' ten loops are
40-46 KB each), a chiptune or upbeat loop 60-140 KB, a recorded or DAW-produced bed 250-350 KB;
a 2 s stinger at 44.1 kHz is 20-30 KB.

**Remove the WAV sources from `assets/`** after encoding (`encode-music.ps1 -RemoveWav`, or
by hand): `SoundManager` prefers `.ogg`, so a WAV left beside it is dead weight that still ships
in the APK. A 28 s loop is 1.2 MB as WAV, 25 times its Ogg size. Regeneration is one command,
so the WAV has no reason to exist in the repository or the build.

## Budgets

| Item | Limit | Rationale |
|---|---|---|
| One music loop | <= 400 KB | synthesised loops are 40-140 KB; the limit exists for DAW-produced beds, where ten tracks would otherwise cost 3-4 MB |
| All audio in the APK | <= 3 MB | install size drives store conversion; Tidy Tubes' shipped audio (10 loops + 9 cues as .ogg/.wav) is under 1 MB once WAV sources are removed |
| Peak level (WAV) | music 0.7 (-3 dBFS), effects 0.5 (-6 dBFS) | leaves headroom for Android's own resampling and keeps cues above the bed |
| Playback | `MUSIC_VOLUME = 0.32f`, `EFFECTS_GAIN = 0.7f` | measured against the user's "soft" preference; do not raise without asking |
| Simultaneous `Music` instances | 1 (2 during a crossfade, 4 with stems) | MediaPlayer limits on low-end devices |
| `Sound` files | each <= 1 s decoded in RAM (~90 KB per second at 44.1 kHz mono) | `Sound` is fully decoded; a 10 s "sound" is really a `Music` |

## Release checks (Play policies that touch audio)

- **Families / Teacher Approved** (if the app targets children): no sudden loud sounds, no
  scary content, music must not contain lyrics with mature themes (synthesised beds pass).
- **Ads**: no game audio over an ad; pause the bed on `AdProvider` show, resume on dismiss.
- **Data safety**: audio playback is not data collection; recording via microphone would be,
  and this workspace's games never request `RECORD_AUDIO`.
- **Accessibility**: music and effects separately switchable (already in SaveData); nothing
  depends on hearing a sound (every cue has a visual twin).
- **Hardware**: test with the phone on silent (Android still plays media volume), with
  Bluetooth headphones (expect 100-250 ms latency: cues must not be the only feedback), and
  during a phone call (bed paused by `Game.pause()`, resumed by `resume()`).
- **Credits**: list any CC-BY work in Settings > About and in the store listing's full
  description (`play-store/<game>/listing.json`), then regenerate the Word listing.

## Task summary template (what the user expects at the end)

```
Goal: <what was composed/changed>
Files: assets/sfx/music_01..05.ogg (new), SoundManager.java, GameConfig.RELAXING_MUSIC_TRACK_COUNT=5, README.txt
Validation: encode-music.ps1 report (5 loops, 24-29 s, 41-48 KB, peaks -3.1..-2.6 dBFS); WAV sources removed; :core:test green; desktop run listened
Size delta: APK +0.23 MB
Open issues: <seam on track 3 slightly audible at 68 BPM, regenerate with --seed 12?>
Next: device test on the Redmi, then listing credits (none needed, all synthesised)
```
