Sound effects and music, picked up automatically by SoundManager (.ogg preferred, else .wav).

Shipped (synthesised, original, no licence needed - regenerate from the workspace root with
    java tools\audio\SfxSynth.java games\sortpuzzle\android\src\main\assets\sfx ):

    pick      a ball is lifted out of a tube
    drop      a ball lands (played higher for each ball stacked)
    invalid   an illegal move was attempted
    button    any UI button
    coin      coins awarded or spent
    tube      a tube was completed with one colour
    hint      a hint is shown
    start     the board deals in
    win       the level is finished
    music     looping background music (paused while sound is off)

Optional voice lines - not shipped. Record them (or use a pack whose licence allows use in a
commercial app) and drop them here with exactly these names; they play automatically:

    voice_nice  voice_great  voice_awesome  voice_perfect  voice_level_complete

Keep effects short and mono. Replacing any file with your own .ogg of the same name overrides it.
