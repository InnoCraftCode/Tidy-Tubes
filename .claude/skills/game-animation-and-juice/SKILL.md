---
name: game-animation-and-juice
description: Animate libGDX scene2d games in this workspace - Actions/Interpolation vocabulary, custom actions, squash-and-stretch, pops, shakes, screen transitions, particles and win celebrations - with the workspace's motion rules (80-300 ms, never block input, no per-frame allocation) and the libGDX bindings for the imported game-feel and motion-design skills. Use for any animation, transition, "make it feel better / more alive / juicy", or when an animation stutters or blocks input.
---

# Game animation and juice (libGDX scene2d)

Entry point for every animation task. Three imported skills hold the depth; this file holds
the workspace rules, the libGDX bindings, and the reference code already in `games/sortpuzzle`.

| Need | Load | Notes |
|---|---|---|
| Which ease, how long, how to stagger, emotional intent | `motion-design` | CSS/GSAP wording; map beziers with the table below |
| Impact feedback: shake, hit-stop, pops, importance tiers, accessibility | `game-feel` | Godot/Unity snippets; bindings below |
| Exact `Actions.*` API, Actor/Group/Table, listeners | `libgdx-scene2d-ui` | reference, not rules |
| `Interpolation` names that exist (`pow2Out`, not `quadOut`) | `libgdx-math` | |
| `ParticleEffect`, pooling, blend modes | `libgdx-particles` | only if a Pixmap/SDF burst is not enough |

## Workspace rules (non-negotiable)

1. Animation lives in screens and `ui/` actors; it never changes game state. Logic decides,
   the screen animates towards the result (`libgdx-game-development`).
2. Durations: micro-feedback 80-150 ms, element moves 150-300 ms, screen transitions 200-350 ms,
   celebrations up to 1.5 s but skippable. Nothing blocks input except the deliberate deal-in.
3. Clamp `delta` to 1/30 in `render()` so a hitch never teleports an Action.
4. No allocation per frame in `act()`/`draw()`: reuse `Vector2`, `Color`, pooled actions
   (`Actions.action(Class)` is pooled), pooled particles.
5. Ease everything. Linear is only for progress bars and rotation loops.
6. Juice returns to rest: scale, colour and offset end exactly at their resting value
   (`scaleTo(1f, 1f, ...)` last in every sequence).
7. Offer "reduce motion" when adding shake or flashes (a `SaveData` flag, off by default).

## libGDX bindings for the imported skills

Easing names (motion-design / game-feel -> `com.badlogic.gdx.math.Interpolation`):

| Role | motion-design | libGDX |
|---|---|---|
| Entrance / settle (ease-out) | MD3 Decelerate, ease-out | `pow2Out`, `pow3Out`, `sineOut` |
| Exit (ease-in) | MD3 Accelerate | `pow2In` |
| On-screen move | MD3 Standard, ease-in-out | `sine`, `smooth`, `pow2` |
| Pop / overshoot | Bounce settle (back) | `swingOut`, `new Interpolation.Swing(1.5f)` |
| Elastic snap | Elastic | `elasticOut` (sparingly, reads cartoonish) |
| Bouncy land | Bounce | `bounceOut` |
| Looping ambient | sine | `sine` with `Actions.forever` |

Game-feel patterns in scene2d:

- **Squash and stretch** (reference: ball landing in `GameScreen`):
  `sequence(scaleTo(1.16f, 0.84f, 0.05f, sineOut), scaleTo(1f, 1f, 0.18f, swingOut))`.
  Set the actor's origin to its centre first or it squashes around a corner.
- **Pop a counter / badge**: `sequence(scaleTo(1.25f, 1.25f, 0.08f, pow2Out), scaleTo(1f, 1f, 0.16f, swingOut))`.
- **Invalid move (no shake of the whole stage)**: nudge the actor, not the camera:
  `sequence(moveBy(6, 0, 0.04f), moveBy(-12, 0, 0.08f), moveBy(6, 0, 0.04f))` plus a brief
  tint `color(Palette.danger, 0.05f)` then back. Pair with `Cue.INVALID`.
- **Trauma shake** (only for big moments; casual puzzles rarely need it): keep a `trauma`
  float on the screen, decay 1.2/s, offset the stage camera by `trauma*trauma * max * sin(t*k)`,
  never move actors. Reset the camera position when trauma reaches 0.
- **Hit-stop**: avoid in casual puzzles. If used, scale `delta` for the board group for 40-80 ms
  with a real-time timer; never freeze the UI stage.
- **Importance tiers**: small = sound + 1 pop (ball drop, button), medium = sound + pop +
  colour flash (tube completed), large = sound + burst + chord (level won, `WinBurst`).
- **Flash**: `sequence(color(Color.WHITE, 0.04f), color(rest, 0.12f))`; honour reduce-motion.

Custom actions: extend `TemporalAction` (`begin()`, `update(percent)`), expose a static
factory that uses `Actions.action(MyAction.class)` so it is pooled. Reference:
`games/sortpuzzle/core/src/main/java/com/ben/laama/sortpuzzle/ui/ArcMoveAction.java`.

Choreography: stagger siblings 20-40 ms (`Actions.delay(i * stagger)` per child), entrances
from the direction of origin, exits 65-75 % of the entrance duration (motion-design tables).

Screen transitions: fade a full-screen `Image` in `BaseScreen` over 200-250 ms, `pow2Out`, and
switch screens in `Actions.run(...)` at the midpoint; dispose the old screen after the switch.

Particles: prefer a small actor that draws N pooled sprites from a Pixmap/SDF texture
(`WinBurst` pattern); use `ParticleEffect` only for complex emitters, pooled via
`ParticleEffectPool` and drawn inside the same `SpriteBatch` begin/end.

## Procedure

1. Name the event and its tier (small / medium / large). Check the hook exists in the screen.
2. Pick channels: motion + sound first, colour second, particles only for medium/large.
3. Write the Action sequence with explicit durations and eases from the tables above.
4. Run `.\gradlew.bat :lwjgl3:run` from the game folder and trigger the event 10 times:
   it must return to rest, never block input, never drift (positions/scales exact after N runs).
5. Capture before/after with `tools\store\capture-desktop.ps1 -Game <name>` when the change
   is visible in a still frame; otherwise describe what was observed.
6. For anything that runs every frame, confirm no allocation (no `new` in `act`/`draw`) and
   check FPS on a low-end profile (`mobile-game-performance`).

## Pitfalls seen in this workspace

- `clearActions()` before adding a new sequence, or the old one keeps fighting the new one.
- `toFront()` the moving actor, or it passes behind siblings.
- Actions on an actor that was removed keep running on nothing: remove in `Actions.run` at
  the end of the sequence, not before.
- A `delay` inside `forever` leaks nothing, but a `forever` on a removed actor still ticks:
  `clearActions()` in `hide()`/`dispose()` paths of the screen.
- Pitch-stepping sounds per repeated animation (`SoundManager.play(cue, pitch)`) is cheap juice;
  keep effects soft (see `games/sortpuzzle/android/src/main/assets/sfx/README.txt`).
