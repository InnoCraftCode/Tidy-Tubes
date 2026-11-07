# ADR-003: Migrate Tidy Tubes to Godot 4

- Status: accepted (2026-11-07), by the owner
- Supersedes for Tidy Tubes: ADR-001 (libGDX stays the engine of record until the Godot build
  reaches feature parity and ships)

## Context
Tidy Tubes 1.2.1 (versionCode 15) is a complete libGDX game: 104 Java files (~17,700 lines),
~3,400 lines of JUnit tests, AdMob GMA Next-Gen 1.6.0 + UMP 4.0.0, Play Games v2, Firebase
Analytics and Crashlytics, 8 languages with a full Arabic right-to-left mirror.

The owner wants Godot for four reasons: richer animation and effects, responsive UI and
layout, a visual scene editor, and a better engine for future games (some 3D).

## Decision
- Godot **4.7.2 stable** (standard build, GDScript), installed in `%LOCALAPPDATA%\Godot\4.7.2`,
  with Android export templates in `%APPDATA%\Godot\export_templates\4.7.2.stable`.
- New project `games/tidy-tubes-godot/`. The libGDX project stays untouched and shippable
  until the Godot build reaches parity.
- **Same levels.** The generator, solver and Java's `Random` are ported bit for bit, and
  verified against fixtures exported from the Java code. Level 37 must remain the same board
  for existing players.
- **Same store identity.** Same applicationId `com.ben.laama.sortpuzzle`, same upload key.
  Released as 2.0.0 (versionCode >= 16), with the libGDX save migrated on first launch.
- **Ads through our own Godot Android plugin (v2)** wrapping the SDKs already in use (GMA
  Next-Gen + UMP), so the workspace ad rules (`AdProvider`, `RewardGate`, `AdPacing`) carry
  over. The community plugins wrap the classic SDK only.
- Logic stays separate from scenes (`src/logic`, `src/model`), with headless tests run by
  `godot --headless`.

## Consequences
- Several weeks of porting. No libGDX feature work in the meantime, except hotfixes.
- GDScript is far slower than the JVM for the solver and generator. Measured in the spike:
  see `games/tidy-tubes-godot/README.md`. If generation is too slow at runtime, levels are
  baked as data from the same port.
- APK size grows (Godot runtime ~25-35 MB vs ~10 MB today).
- Godot gives native text shaping and right-to-left layout (HarfBuzz), containers and anchors
  for responsive UI, Tweens, GPU particles and shaders.
