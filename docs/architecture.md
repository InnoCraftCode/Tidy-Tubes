# Workspace architecture

## Decisions
- Engine: libGDX - [ADR-001](../config/architecture/ADR-001-engine.md)
- Ads: AdMob GMA Next-Gen + UMP behind `AdProvider` - [ADR-002](../config/architecture/ADR-002-ads-sdk.md)
- Code style: [java-libgdx.md](../config/coding-standards/java-libgdx.md)

## Separation of concerns
```
workspace (reusable, versioned once)           game (one Gradle project each)
  .claude/  agents, skills, commands             core/     rules (pure Java) + screens
  tools/    PowerShell automation                android/  launcher, AdMobAdProvider, manifest
  config/   ADRs, standards, checklists          lwjgl3/   desktop launcher for iteration
  shared/   design/privacy/testing templates     README    concept, architecture, ads, commands
  templates/libgdx-2d  -> copied by new-game.ps1 ->  games/<name>/
```
Games never depend on each other or on `shared/` at build time; reuse is by copying proven code
(template, Sortpuzzle) so each game builds alone and can be archived or handed over.

## Inside a game
```
          input                      draw
Player ---------> Screen --------------------> GPU
                    | asks / applies    ^ reads
                    v                   |
               logic/ + model/  (pure Java, unit-tested, deterministic)
                    |
         services via interfaces: AdProvider, Preferences/SaveData, SoundManager
                    |
   android: AdMobAdProvider (UMP -> SDK)      desktop/tests: NoOpAdProvider
```
Threads: render thread owns GL and game state; Android UI thread shows ads; worker threads run
solvers/generation and SDK init. Crossing happens only through `Gdx.app.postRunnable` and
`runOnUiThread`.

## Optional engines
- **Godot 4**: consider for a 2D/3D game that benefits from a scene editor and animation tools.
  Needs the editor + Android export templates + an AdMob community plugin. Write an ADR first.
- **Unity**: consider for content-heavy 3D or asset-store dependencies. Needs Unity Hub, an Editor
  with Android Build Support, and the Google Mobile Ads Unity plugin; check Unity's licence terms.
Neither is installed; no template exists for them, deliberately.

## Versions (pinned, 2026-10-21)
libGDX 1.13.1, Gradle 8.14.3, AGP 8.11.1, JDK 21 (Temurin), compileSdk/targetSdk 35 -> **36 required for
Play**, minSdk 24, GMA Next-Gen 1.6.0, UMP 4.0.0, JUnit 5.11.4.
