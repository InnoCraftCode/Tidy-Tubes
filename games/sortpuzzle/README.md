# Tidy Tubes (project `sortpuzzle`, formerly "Ball Sort")

A tube-sorting puzzle built on libGDX 1.13. Java 21 toolchain, Gradle Kotlin DSL, three modules:
`core` holds everything, `lwjgl3` runs it on a desktop for iteration, `android` is the shipping
target. There are no bitmap art assets — every shape the renderer touches is generated at
start-up from a signed distance field (`ui/Sdfs`, `ui/Tex`, `ui/Icons`); the only files are the
Poppins fonts (SIL OFL) and the synthesised sounds. Design system: `docs/design-system.md`
(workspace root). Ads: `docs/admob-integration-current-release.md`.

## Running it

You need a JDK 21 on `PATH` and, for the Android module, an Android SDK with platform 37 (or
36) and build-tools installed (`local.properties` must point at it with `sdk.dir=...`). Gradle
downloads itself. The Android build targets API 36 and compiles against 37.

```
gradlew :lwjgl3:run          # play it on the desktop
gradlew :core:test           # the unit suite, ~6 seconds
gradlew :lwjgl3:dist         # one runnable jar in lwjgl3/build/libs/
gradlew :android:assembleDebug
gradlew :android:assembleRelease
```

The desktop window opens at half the virtual resolution (360x640) and can be resized freely; the
`FitViewport` letterboxes rather than distorting, so what you see is what a phone sees.

If the build complains that `settings.gradle` and `settings.gradle.kts` both exist, an older
Android Studio scaffold is still in the folder. Delete `settings.gradle`, `build.gradle`,
`gradle/gradle-daemon-jvm.properties`, and the `app/` and `build/` directories.

## Architecture

```
core/src/main/java/com/ben/laama/sortpuzzle/
  GameConfig.java        every tuning value, in one place
  Main.java              Game subclass; owns the batch, skin, save, audio, ads; routes screens
  model/                 pure Java, zero libGDX imports
    Packed.java            a tube as four 4-bit slots in one int
    Tube.java              immutable tube
    Ball.java              interned colour token
    Move.java              from, to, count, colour
    BoardState.java        immutable board; apply(Move) returns a new one
  logic/                 pure Java, zero libGDX imports
    Difficulty.java        colours / spare tubes / scramble / move limit, from the level index
    LevelGenerator.java    reverse shuffle from the solved board
    Solver.java            DFS for "a solution", IDA* for "the shortest solution"
    HintEngine.java        the solver pointed at the current position, as advice
    Hint.java              play this move / undo / already won / don't know
    MoveValidator.java     the single source of truth for what a legal move is
    WinChecker.java        win and deadlock detection
    Level.java             a generated board plus its parameters
  screens/               everything that draws
    BaseScreen.java        720x1280 ExtendViewport (grows to 1000x1720), backdrop, ESC/back handling
    SplashScreen, HomeScreen, LevelSelectScreen, GameScreen, SettingsScreen
  ui/
    Sdfs.java, Tex.java    signed-distance primitives and the textures rasterised from them
    Icons.java             the icon set (undo, hint, settings, ...) as SDFs
    SkinFactory.java       the entire Skin, built in code; Poppins via gdx-freetype at screen density
    Palette.java           every colour in the game (design system "Nocturne")
    Ui.java                component kit: buttons, cards, chips, toggle, dialog, toast
    TubeTextures.java      U-shaped tube textures cached per size
    BrandMark.java         the logo, built from tube and ball actors
    TubeActor, BallActor, HudTable, ArcMoveAction, WinBurst, AnimatedBackground
  save/SaveData.java     progress as a JSON blob inside libGDX Preferences
  audio/SoundManager.java  effects + music, two independent switches
  ads/                   AdProvider, AdProviders, AdsConfig, RewardGate, AdPacing,
                         NoOpAdProvider (desktop stub), DisabledAdProvider (ad-free builds)
  logic/RewardOffer.java video when available, coins otherwise
```

The dividing line that matters: `model` and `logic` import nothing from libGDX. They are plain
Java, they are where every rule lives, and they are the only part with unit tests. Everything
that draws lives in `screens` and `ui` and holds no rules of its own — `GameScreen` asks
`MoveValidator` whether a tap is legal and animates towards whatever `BoardState` says is true.

## The rules, as implemented

A tube holds four balls. Tapping a tube lifts its top *run* — the contiguous block of one colour
at the top, not just one ball. Tapping a second tube drops them if that tube is empty, or if it
already shows the same colour and has room. If the run is longer than the space available, as
many balls as fit make the trip and the rest stay put. A level is won when every non-empty tube
holds four balls of a single colour.

Undo is unlimited within a level and gives the move back rather than costing another. It has to:
with a move limit in play, an undo that still burned a move would make the button a trap rather
than a safety net. Each level grants three free extra-tube boosters; beyond that they come out of
the permanent stock bought with coins.

## Move limit, hints, and where the ads go

A level's move limit is its par plus slack — `par + max(6, 80% of par)` — so the allowance scales
with the puzzle instead of being a flat number that is generous on level 1 and impossible on
level 200. It is deliberately loose: a greedy, far-from-optimal solve of level 120 takes 29 moves
against a limit of 43, so the wall only catches genuine flailing. Par arrives from a worker
thread a moment after the board does, and until it lands there is no limit at all — better than
guessing one and correcting it under the player's nose.

Run out and the level is lost, with one way back: a rewarded video buys five more moves and
resumes the position in place.

The hint system is the same solver pointed at the current position rather than the opening.
`HintEngine.advise` returns one of four things, and the distinction between the last two is the
point of the class: play this move, undo because the position is genuinely dead, or *I don't
know* because the search ran out of budget. Telling a player to undo when you simply failed to
look far enough is a lie that costs them the level.

Each level gives one hint free. After that a rewarded video unlocks hints for the rest of the
level — not a single hint, because a player stuck enough to watch an ad is not helped by being
asked to watch another one four moves later. While hints are unlocked, the status line keeps a
live count of moves remaining, recomputed on a worker thread after every move, and labelled
"at most" whenever the exact search had to fall back to a merely workable route.

A suggested move pulses both tubes in gold — a different colour from the blue selection
highlight, so "you picked this" and "the game is pointing here" never look alike — and flies a
translucent ball along the path three times.

On Android the rewarded video is AdMob (`android/.../AdMobAdProvider.java`, GMA Next-Gen SDK +
UMP consent); rewards settle exactly once through `RewardGate`. When no video is available —
no fill, offline, or a build without ad IDs — `RewardOffer` offers the same reward for coins
(`GameConfig.HINT_UNLOCK_PRICE`, `EXTRA_MOVES_PRICE`), and says so honestly when the player
cannot afford it. An interstitial may run when the player leaves the level-cleared card, at
most every `INTERSTITIAL_EVERY_N_LEVELS` levels and `INTERSTITIAL_MIN_INTERVAL_MS` apart
(`AdPacing`). Debug builds use Google's demo unit IDs; release builds read `admob.properties`
(git-ignored, see `admob.properties.example`) and ship **without** ads when it is missing.

On the desktop `NoOpAdProvider` **grants every reward immediately** and logs a line saying so.
That is deliberate: both the hint flow and the out-of-moves flow hang off `showRewarded`, and a
provider that always refused would leave half the game untestable where there is no ad SDK. It
is never used on a device (`AdProviders`).

## Level generation

This is the part worth reading.

Levels are built by walking **backwards** from the solved board, never by dealing randomly and
testing afterwards. Undoing the shuffle step for step is itself a valid solution, so solvability
is a property of the construction rather than something a solver has to confirm. Generation is
linear in the shuffle length and never retries.

A reverse move lifts `m` balls of colour `c` off tube `B` and drops them on tube `A`. It is only
legal when the forward move `A -> B` would put the board back exactly as it was, and there are
two distinct ways for that to hold. Either `A` shows a different colour (or is empty), so the
`m` balls become its whole top run and all of them travel back together — or `A` already shows
colour `c` **and `B` was full**, in which case the forward move carries back only as many balls
as `B` has room for, which is exactly `m`. That second family is easy to miss and roughly doubles
the generator's freedom; without it, the walk paints itself into a corner constantly.

The harder problem is where the walk *stops*. Players expect full tubes of mixed balls plus
genuinely empty spares, and a free random walk essentially never lands on such a board — measured
here at roughly one tidy board per four thousand steps, which quietly ate the empty tubes the
difficulty curve had promised. So the walk does not step one reverse move at a time. It steps in
**macro moves**: a short chain of reverse moves that starts on a tidy board and ends on a
different tidy board, found by iterative-deepening search with the move order shuffled. The
shortest such chain lifts part of one tube's top run into a spare, refills that tube from a
second, and empties the spare onto the second — a block swap between two tubes. Longer chains
rotate blocks around three or more. Every link is still a reverse move, so the invariant holds.

Two refinements keep the results honest. Some boards are dead ends — no forward move leads into
them, so no reverse move leads out — and the walk resumes from an earlier board rather than
stopping, because a dead end reached after six macro moves would otherwise ship as a level
solvable in three. And boards that already have a colour finished are skipped outright; they
give the opening away.

Which of the remaining boards ships is the difference between a curve and a plateau, and it is
easy to get wrong. Shipping the most scrambled board every time sounds right and plays terribly:
the colour count only steps up every few levels, so every level inside a band arrives at the
same ceiling and the player feels nothing change from one level to the next. Instead the
candidates are ranked by how long a solution they need, and the board at a target percentile is
the one that ships — `Difficulty.scrambleRatioFor` supplies that percentile. Every new colour
arrives on a forgiving board and tightens level by level until the next one appears, so
something moves on every single level. Past level 100 the game is at its ceiling, ten colours
and one spare tube, so the ramp cycles instead of pretending to climb further.

Determinism comes from seeding `java.util.Random` with a SplitMix64 mix of the level index, so
level 37 is the same board on every device and every install, forever.

## The solver

`Solver.findAny` is a depth-first graph search used by the tests to prove that generated levels
really are solvable, and by the game as a fallback when the optimal search runs out of budget.
`Solver.findOptimal` is IDA* and produces the par-move count shown under the level title.

Its heuristic is the number of extra tubes each colour is spread across: a colour living in three
tubes needs at least two more moves to be gathered. A single move changes that total by at most
one, which makes the estimate admissible and the answer genuinely optimal.

Both searches run on packed `int[]` boards rather than `BoardState` objects, memoise positions
under an order-independent key (tubes are interchangeable, so a board and the same board with two
tubes swapped are the same puzzle), and drop two classes of move outright: taking balls back out
of a finished tube, and tipping an already single-coloured tube into an empty one, which only
renames a tube. Neither can appear in a shortest solution.

Generation and the par search both run on a worker thread in `GameScreen`. Generation averages
11ms and peaks around 100ms on a desktop — most of that is the ranking pass, which solves a
sample of candidate boards to place the level on its difficulty ramp — and the optimal search on
a ten-colour board is longer still. Neither belongs anywhere near the render thread.

## Changing the game

**The difficulty curve** lives entirely in `Difficulty.java`. `colorsFor`, `emptyTubesFor`,
`shuffleDepthFor` and `scrambleRatioFor` are pure functions of the level index and nothing else
consults anything but them. Widening a band is a one-line edit; `DifficultyTest` asserts the
bands hold, that the colour count never goes backwards, and that the scramble ramp restarts
exactly where the colour count changes — the boundaries in `scrambleRatioFor` are written out by
hand and that test is what catches them drifting out of sync with `colorsFor`.

To make the whole game gentler or harsher without touching the bands, move `SCRAMBLE_FLOOR`:
it sets how easy the first board of each colour group is allowed to be.

**More colours** means two edits: raise `GameConfig.MAX_COLORS` and add entries to the `BALLS`
array in `Palette.java`. The packed encoding reserves a nibble per slot, so it tops out at 14
colours; past that, widen `Packed` to a `long` per tube. Pick colours that differ in lightness as
well as hue — the existing ten do.

**More levels** takes no edits at all. There is no level table; `LevelGenerator.generate(n)`
answers for any `n`, and the level-select grid simply shows everything unlocked plus thirty ahead.

**Economy and generosity** numbers are all in `GameConfig`: coins per win, the par bonus, booster
price, free boosters per level, free hints per level, extra moves per video, and the two
constants behind the move limit (`MOVE_LIMIT_BASE_SLACK` and `MOVE_LIMIT_SLACK_RATIO`).

## Assets and sound

No bitmap art, deliberately. `Tex.java` generates the ball, the tubes, the panels, the icons and
the background gradient as anti-aliased pixmaps at start-up (signed distance fields in
`Sdfs.java`), and `SkinFactory` assembles the whole `Skin` in code. Text is Poppins
(`android/src/main/assets/fonts/`, SIL Open Font License, licence file alongside), rasterised by
`gdx-freetype` at the screen's density so it is crisp on every phone; `SkinFactory.registerFonts`
is the only place that knows about it. Launcher icons and the Play Store graphics are rendered by
`tools/store/render-brand-assets.ps1` (workspace root).

Sound effects and a 20-second looping music bed ship as `.wav` files in
`android/src/main/assets/sfx/`. They are synthesised, not recorded - `tools/audio/SfxSynth.java`
at the workspace root generates all of them deterministically, so they are original and carry no
licence. Regenerate with `java tools\audio\SfxSynth.java games\sortpuzzle\android\src\main\assets\sfx`.
`SoundManager` prefers an `.ogg` of the same name, so replacing any cue is a file drop. Voice lines
(`voice_nice`, `voice_great`, `voice_awesome`, `voice_perfect`, `voice_level_complete`) are wired but
not shipped; `android/src/main/assets/sfx/README.txt` lists every cue.

## Store screenshots and promo video

`lwjgl3/.../StoreCapture.java` renders the game off-screen at 1080x1920 and drives it through a
scripted scenario (`GameScreen.autoPlay` plays the solver's moves through the normal tap path), so
the Play screenshots and the promo-video frames show real animations. It uses throwaway preferences
and disabled audio. From the workspace root:
`.\tools\store\capture-desktop.ps1 -Game sortpuzzle [-Video]` (outputs in `play-store/sortpuzzle/`),
then `.\tools\store\export-store-assets.ps1 -Game sortpuzzle` to validate and copy the final kit to
`store-assets/`.

## Juice

- Every screen fades in; every button squashes and springs back when pressed (`BaseScreen`).
- `AnimatedBackground`: translucent balls drift up the gradient, toned down to 35% behind the board.
- Held balls bob; landing balls squash and clink, each one a step higher than the last.
- Completing a tube: its balls hop in a wave, sparks in its colour, a chime, and a word
  (NICE! / GREAT! / AWESOME! / AMAZING!) that escalates through the level.
- Winning: firework, all balls jump in a wave, PERFECT! or COMPLETE!, then the result panel.

## Android release

`android/build.gradle.kts` reads signing details from a `keystore.properties` at the project root:

```
storeFile=ball-sort-release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Without that file the release build still succeeds and produces an unsigned APK, so a fresh clone
never blocks. The file is git-ignored along with `*.jks` — do not commit a keystore.

Release builds run R8 with `proguard-rules.pro`. The rule that matters is the one keeping
`SaveData`: libGDX `Json` walks its fields reflectively, and stripping them wipes player progress
on release builds only — a bug that never shows up in debug.

## Known trade-offs

`core` compiles to Java 17 bytecode even though the toolchain is 21, because D8 is the lowest
common denominator and `core` is consumed by the Android build. The `lwjgl3` module targets 21.

The par-move count is computed with a bounded node budget. On the hardest boards the optimal
search can exhaust it, in which case par falls back to the length of the first solution found —
an upper bound rather than the true optimum. The HUD reads "best possible", which stays honest
either way.
