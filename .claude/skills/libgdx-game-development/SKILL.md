---
name: libgdx-game-development
description: Implement libGDX gameplay in Java - screens, scene2d UI, input, game rules, save data, audio, worker threads, desktop testing - following the workspace's pure-logic/thin-screen pattern. Use for any feature work inside a game's core module. 3D specifics are in 3d.md.
---

# libGDX game development

## When to use
Writing or changing gameplay, UI, input, persistence or audio in `core/`. Not for Gradle/manifest
(android-build-and-export) or ad SDK code (admob-integration).

## Inputs
Target project, the feature/bug description, acceptance criteria. Read the project README and
`GameConfig` before anything else.

## Workflow
1. Locate: Grep for the class/feature; read only those files.
2. Model the rule in `logic/` or `model/` (no `com.badlogic` imports). Immutable state + `apply(move)`
   returning a new state is the preferred shape (Sortpuzzle `BoardState`).
3. Test it in `core/src/test/java` with JUnit 5 and `@DisplayName` sentences describing the rule.
4. Wire the screen: input -> ask logic -> update state -> animate (scene2d Actions or interpolation).
5. Tuning numbers -> `GameConfig`. Colours -> a `Palette` class. Strings shown to players -> one place.
6. Run desktop to feel it: `.\gradlew.bat :lwjgl3:run`.

## Standards
- Viewport: `FitViewport` on the virtual resolution; unproject touches through it.
- Screens: extend a base screen; dispose the Stage/Textures in `dispose()`; switch screens via a
  deferred `postRunnable` (never dispose a stage mid-dispatch - Sortpuzzle `Main.switchTo`).
- No allocation in `render()`: reuse vectors, `Pool`/`Array`, cache GlyphLayouts.
- Worker threads for solvers/generation; publish results with `Gdx.app.postRunnable`. Never touch GL
  off the render thread.
- Persistence: libGDX `Preferences`; JSON via `Json` for structured saves; add a ProGuard `-keep`
  for every reflectively serialised class; version the save format.
- Back button: `Gdx.input.setCatchKey(Input.Keys.BACK, true)` and handle it (pause/menu/exit).
- Lifecycle: `pause()` must save progress; assume the process can die after `pause()`.
- Audio: `Sound` for short SFX, `Music` for streams; respect a mute setting; .ogg only.

## Validation
```powershell
.\gradlew.bat :core:test --tests "*FeatureTest"
.\gradlew.bat :core:test
.\gradlew.bat :lwjgl3:run
.\gradlew.bat :android:assembleDebug      # if Android-visible behaviour changed
```

## Common failures
- Works in debug, progress lost in release -> missing ProGuard keep for a `Json` class.
- Black screen after resume -> textures created outside `create()`/not reloaded; prefer managed textures.
- Touch offset on some phones -> touches not unprojected through the viewport.
- Stutter every few seconds -> per-frame allocation (GC). Check with the performance skill.
- `GdxRuntimeException: No OpenGL context` in tests -> test touched libGDX graphics; move logic out.

## Deliverables
Code + tests green + short summary. Update the README if architecture or rules changed.

## Not for
3D rendering details -> read [3d.md](3d.md). Ads -> admob-integration. Build issues -> android-build-and-export.

## References
- https://libgdx.com/wiki/  (scene2d, viewports, file handling, threading, preferences)
- https://libgdx.com/wiki/app/threading
