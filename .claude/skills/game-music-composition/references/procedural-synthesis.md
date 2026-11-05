# Procedural music: generating original loops without a DAW

Two generators exist in the workspace; both are single-file Java programs run with
`java <file>.java` (Java 21, no dependencies, no build step):

| Generator | Where | Scope |
|---|---|---|
| `SfxSynth.java` | `tools/audio/` | Tidy Tubes' 9 effects + its 10 ambient loops, hard-wired. Reference for the synthesis kit. |
| `MusicSynth.java` | `scripts/` of this skill | Generic: any game, four styles, any tempo/key/mode/length, seeded, optional stems. |

Copy `MusicSynth.java` into `tools/audio/` of the workspace (or a game's `tools/`) only if a
game needs a style the parameters cannot express; otherwise run it in place.

## MusicSynth usage

```powershell
java .claude\skills\game-music-composition\scripts\MusicSynth.java `
    --out games\<game>\android\src\main\assets\sfx `
    --name music --style ambient --tempo 68 --key D --mode major --bars 8 --seed 11 --count 5
```

| Option | Default | Meaning |
|---|---|---|
| `--out <dir>` | required | Output folder (created if missing) |
| `--name <base>` | `music` | `music.wav`, or `music_01.wav`... when `--count` > 1 |
| `--style` | `ambient` | `ambient` (pad + sparse pluck), `chiptune` (square lead, arpeggio, hats), `upbeat` (plucks, full drum kit), `tense` (drone bass, pulse kick, minor lead) |
| `--tempo <bpm>` | per style | 40-200 |
| `--key <note>` | `C` | `C C# Db D ... B`; root is placed around MIDI 48 |
| `--mode` | per style | `major` or `minor` (natural) |
| `--bars <n>` | 8 | Loop length in 4/4 bars (8 at 70 BPM = 27 s) |
| `--seed <n>` | 1 | Deterministic; each variant uses `seed + index` |
| `--count <n>` | 1 | Number of variants |
| `--stems` | off | Also writes `<name>_pad/_bass/_melody/_drums.wav` for adaptive layering |
| `--sr <hz>` | 22050 | Sample rate; 44100 only for bright 3D ambiences |

Output: 16-bit mono WAV, peak normalised to 0.7 (so `MUSIC_VOLUME = 0.32f` lands at the same
loudness as the shipped Tidy Tubes beds). The console prints duration, size and a seam check
(the absolute level difference between the last and first sample, which should be < 0.02).

## How a loop is built (so you can tune it rather than fight it)

1. **Harmony.** A 4-chord progression is picked from a diatonic pool per mode (major: I V vi IV,
   I vi IV V, vi IV I V...; minor: i VI III VII, i iv VI v...), then repeated or paired with a
   second pool entry for the back half. Diatonic only: no accidentals means any pentatonic
   melody note is consonant, which is what makes random melodies sound intentional.
2. **Pad.** Each chord's three tones, one octave up, as detuned sine pairs with a sine-shaped
   envelope that starts 0.2 s before the bar and overlaps the next. Pads wrap around the loop
   end, which is what makes the seam inaudible.
3. **Bass.** Root two octaves down, sine pluck on beat 1; beat 3 (ambient/upbeat), eighths
   alternating root/octave (chiptune), beat 1 + "and of 4" (tense).
4. **Melody.** A two-bar phrase of pentatonic notes is rolled from the seed, then arranged as
   A A' B A' over eight bars (A' re-rolls the last four slots). Repetition with variation is
   why it reads as a tune rather than noise. Density per style: ambient 22 %, tense 18 %,
   upbeat 35 %, chiptune 45 % of eighth slots; off-beat slots get a third of that.
5. **Arpeggio** (chiptune, upbeat): sixteenth-note chord tones, quiet, on bars 3-4 and 7-8 so
   the texture breathes.
6. **Drums** (not ambient): kick on 1 and 3 (every beat for tense), snare noise on 2 and 4
   (upbeat only), hats on off-beat eighths. All samples are synthesised sweeps and filtered
   noise; nothing is recorded.
7. **Mix and normalise.** Layers are summed, the sum is scaled to 0.7 peak, and the same gain
   is applied to the stems so they re-sum exactly to the mix. Chiptune is low-passed at
   7 kHz because band-limited squares at 22.05 kHz still sting on phone speakers.

## Tuning recipes

| Complaint | Change |
|---|---|
| "Too busy under gameplay" | lower density: style `ambient`, or edit `DENSITY` in the style table; drop the arpeggio |
| "Boring, nothing happens" | `--bars 16` (second progression kicks in), `--style upbeat`, or raise melody amp 0.065 -> 0.08 |
| "Seam is audible" | check the report's seam value; pads already wrap, so the culprit is a long pluck on the last beat: shorten `PLUCK_DECAY` for that style |
| "Too bright on the phone" | lower `LOWPASS_HZ` for the style (ambient already 5 kHz) |
| "All variants sound the same" | vary `--key` within +-5 semitones and alternate `--mode`; different seeds alone only change the melody |
| "Need a 3D ambience, not music" | `--style ambient --tempo 48 --bars 4` and drop the melody amp to 0 in the style table: it becomes a drone bed |

## Hand-composed alternative

If a tracker/DAW is available (LMMS, Bosca Ceoil, Ableton, FL): export WAV 44.1 kHz mono,
loop points at bar boundaries, no reverb tail past the loop end (bake it with a wrapped bounce
or trim it), then run `encode-music.ps1` on the folder. Use the same brief and levels; the
only thing that changes is the source of the WAV.

## Extending SfxSynth for stingers and cues

Effects (win, lose, coin...) stay in `SfxSynth.java` style: short `sweep`, `bell`, `sparkle`,
`noise` calls, written at 44.1 kHz and automatically softened (4 kHz low-pass, 0.5 peak) by
`write()`. When a new game needs its own cue set, copy `SfxSynth.java` into its `tools/`,
keep the kit, rewrite the `static double[] <cue>()` functions, and regenerate everything with
one command so the README's "regenerate with" line stays true.
