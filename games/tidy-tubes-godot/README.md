# Tidy Tubes (Godot 4.7.2)

Port of `games/sortpuzzle` (libGDX), decided in `config/architecture/ADR-003-godot-migration.md`.
The libGDX game stays the shipping version until this one reaches parity.

## Layout
| Path | What |
|---|---|
| `src/model/` | Board encoding (`packed.gd`), pure data |
| `src/logic/` | Rules, solver, level generator, difficulty: no nodes, no scenes |
| `src/util/java_random.gd` | Bit-exact `java.util.Random`, so seeded content matches the Java game |
| `tests/` | Headless test runner and suites; `fixtures/` is exported from the Java classes |
| `tools/ExportFixtures.java` | Regenerates the fixtures from the libGDX core |

## Commands (PowerShell, from this folder)
```powershell
$g = "$env:LOCALAPPDATA\Godot\4.7.2\Godot_v4.7.2-stable_win64_console.exe"
& $g --headless --path . --import                                   # first time / after adding assets
& $g --headless --path . --script res://tests/run_tests.gd          # tests (levels 1-30)
& $g --headless --path . --script res://tests/run_tests.gd -- levels=1-120   # full parity
# Refresh fixtures after a change to the Java generator (build the libGDX core first):
java -cp ..\sortpuzzle\core\build\classes\java\main tools\ExportFixtures.java tests\fixtures\levels.json 1 120
```

## Parity status (2026-11-07)
- `java.util.Random` (`nextInt`, `Collections.shuffle`): identical streams.
- Level seeds (SplitMix64), difficulty curve (float32 maths emulated): identical.
- Levels 1-120: identical boards to the Java generator, tube for tube.
- Solver `find_any`: same move count and the same node count as Java.

Gotchas:
- Godot's `sort_custom` is not stable. Java's `List.sort` is, so equal keys need an explicit
  index tie-break.
- GDScript ints wrap like Java longs. Hex constants above 2^63 are written as signed decimals.
- Java `>>>` on longs is `_ushr`. Java float maths goes through `Difficulty.f32`.

## Speed (desktop, GDScript)
Generation takes ~160 ms per level on average, worst ~390 ms (level 113, 10 colours), against
~4 ms on the JVM. A phone may be 3-5x slower. Plan: generate the next level on a
`WorkerThreadPool` task while the current one is played, and compute par lazily.

## Migration phases
1. Setup + core logic parity: model, random, generator, solver. **Done.**
2. Remaining logic: rules, moves, hints, economy, shop catalogue (400 styles), star road,
   daily challenge, weekly goals, achievements, reminders planner, save data, i18n tables.
3. Screens with Godot Control nodes: theme from `Palette`, fonts, responsive containers.
   Built-in RTL replaces `RtlMirror`.
4. Juice: Tweens, GPUParticles2D, shaders.
5. Android plugin v2: GMA Next-Gen + UMP (`AdProvider`, `RewardGate`, `AdPacing`), Play Games
   v2, Firebase, notifications, migration of the libGDX save.
6. Parity QA, capture tools, release 2.0.0 (same applicationId and upload key).
