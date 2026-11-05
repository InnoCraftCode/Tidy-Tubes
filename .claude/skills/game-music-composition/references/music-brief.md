# Music brief: deciding what to compose before producing a note

A brief is one table. Fill it with the designer (or the game's README and GameConfig when
working alone), save it as `games/<game>/docs/audio-brief.md`, and every later choice
(generator style, tempo, number of loops) is read from it instead of being re-argued.

## Template

```markdown
# Audio brief: <Game>

Genre: <2D puzzle | 2D action | 3D ...>   Audience: <all ages / 13+>   Session: <2-5 min>
Reference feel (words, not songs): <calm, woody, cosy> / <bright, bouncy, arcade> / ...

| State        | Mood          | Tempo   | Key / mode     | Instruments (2-3)            | Loop   | Source      |
|--------------|---------------|---------|----------------|------------------------------|--------|-------------|
| Main menu    | welcoming     | 64-72   | C major        | soft pad, sine pluck, bass   | 32 s   | MusicSynth ambient |
| Play         | focused, calm | 60-70   | same family    | pad, sparse pluck            | 24-32 s| MusicSynth ambient x N |
| Tension/boss | urgent        | 110-130 | relative minor | pulse bass, drums, lead      | 16 s   | MusicSynth tense |
| Win          | earned, warm  | n/a     | major          | stinger 1.5-2.5 s            | one-shot | SfxSynth `win` |
| Lose/fail    | soft, no shame| n/a     | minor          | stinger <= 1.5 s             | one-shot | SfxSynth |
| Shop/settings| light         | menu    | menu           | menu loop at -3 dB           | reuse  | - |
```

Mood words are what the generator's `--style` and the synth parameters map to; song names are
not (they invite imitation, and imitation is the licensing risk).

## Choosing tempo, key and mode

| Feeling wanted | Tempo (BPM) | Mode | Notes |
|---|---|---|---|
| Calm, meditative (sorting, matching, idle) | 58-76 | major or Dorian | Long pad, melody density <= 25 %. Below 58 the loop feels stalled. |
| Cosy, friendly (menus, farming, decorating) | 72-90 | major | Plucked chords, bass on beats 1 and 3. |
| Playful, arcade (runner, 2D platformer) | 100-130 | major, mixolydian | Square/triangle leads (chiptune style), hats on off-beats. |
| Tense, danger (timer, boss, stealth) | 100-140 | minor, phrygian | Drone bass, sparse lead, kick every beat, no resolution at the loop end. |
| Mysterious, exploration (3D worlds) | 60-90 | minor, lydian | Wide detuned pads, occasional bell, long reverb tails simulated by overlapping pads. |
| Victory stinger | n/a | major, ends on I | 1.5-2.5 s, ascending, finish on the tonic so it "closes". |

Rules of thumb that keep variants coherent:

- Keep all loops of one game in the **same key family** (same tonic or relative major/minor).
  Players switch screens every few seconds; a key change on every screen feels like channel
  surfing. Tidy Tubes' ten tracks are deliberately within +-5 semitones of C.
- **Loop length**: 8 bars at 60-80 BPM is 24-32 s, long enough that the ear forgets the seam.
  At 120+ BPM use 16 bars. Under 15 s a loop becomes a jingle and annoys within a minute.
- **Density**: a bed under gameplay must leave space for the effects. Melody notes on at most
  a quarter of the eighth-note slots; no note above MIDI 84 (C6) unless it is a short bell.
- **Stingers are not loops**: a win jingle is a Sound (`Sound.play`), not a `Music`, so it can
  overlap the bed and never needs stopping.

## Instrument vocabulary (what MusicSynth can produce)

| Role | Wave | Typical amp | Decay | Use |
|---|---|---|---|---|
| Pad | detuned sine pair | 0.06-0.10 | whole bar | Chord bed, every style |
| Bass | sine | 0.11-0.18 | 0.9-1.2 s | Root on beat 1; add beat 3 for motion |
| Pluck lead | PLUCK (sine + decaying 2nd/3rd harmonic) | 0.06-0.09 | 0.5-0.8 s | Melody in ambient/cosy |
| Chip lead | SQUARE (band-limited) / TRIANGLE | 0.05-0.07 | 0.15-0.3 s | Arcade styles only |
| Bell | inharmonic partials | 0.04-0.06 | 1-2 s | Sparse accents, mystery |
| Kick | 120 -> 45 Hz sine sweep | 0.25 | 0.18 s | Upbeat/tense |
| Hat | short low-passed noise | 0.05 | 0.03 s | Off-beats |
| Snare | noise + 180 Hz sine | 0.12 | 0.12 s | Upbeat beats 2 and 4 |

## Questions to settle with the user before generating

1. Can the player choose a track (then make 5-10 variants and a picker) or is music fixed?
2. Is there a tension state at all? Casual puzzles usually should not have one.
3. Any audience limit (Families policy: no startling sounds, nothing above moderate loudness)?
4. Is the game 3D or does it have a camera that moves relative to sound sources?
5. Target install size: how many KB are available for audio (default budget 3 MB total)?
