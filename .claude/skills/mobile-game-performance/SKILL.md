---
name: mobile-game-performance
description: Measure and fix libGDX performance on Android - FPS, frame spikes, GC stutter, draw calls, texture memory, startup time, battery/thermal, low-end devices. Use when the game is slow, janky, hot, large, or before release on low-end hardware.
---

# Mobile game performance

## When to use
Reported jank/slowness/heat, large APK, slow startup, or a pre-release perf pass. Not for
speculative micro-optimisation without a measurement.

## Inputs
Device model + Android version (or emulator), build type (measure **release** for final numbers),
the scene/level where it happens.

## Workflow
1. Baseline. Add a temporary debug overlay or log once per second:
   - `Gdx.graphics.getFramesPerSecond()`, `Gdx.graphics.getDeltaTime()` max per second
   - `SpriteBatch.renderCalls` / `maxSpritesInBatch` after `end()`
   - `Gdx.app.getJavaHeap()`, `Gdx.app.getNativeHeap()`
   - `GLProfiler` (calls, textureBindings, shaderSwitches) - enable, read, `reset()` each frame
2. Device-side views:
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb shell dumpsys gfxinfo <applicationId>
   & $adb shell dumpsys meminfo <applicationId>
   & $adb shell am start -W -n <applicationId>/.android.AndroidLauncher   # TotalTime = cold start
   ```
   Android Studio Profiler (CPU/memory) attaches to debuggable builds.
3. Classify the bottleneck: CPU logic, GC, draw calls/texture binds, fill rate/overdraw, I/O at load.
4. Fix one thing, re-measure, keep the change only if the number moved.

## Standards / usual fixes
- GC: no `new` in `render()`/`act()`; reuse `Vector2`, `Rectangle`, `GlyphLayout`; `Pools` for actors/effects.
- Batching: one atlas per screen; avoid alternating textures/fonts; `ShapeRenderer` begin/end breaks batches.
- Fill rate: avoid full-screen translucent layers; disable blending when drawing opaque backgrounds.
- Heavy algorithms (search/generation) off the render thread (Sortpuzzle `GameScreen` pattern).
- Startup: generate procedural textures once; lazy-load secondary screens; use `AssetManager` with a loading screen for >1 s of assets.
- Battery: `config.useAccelerometer/useCompass/useGyroscope = false` unless used; consider
  `Gdx.graphics.setContinuousRendering(false)` + `requestRendering()` for turn-based screens.
- Size: compress audio (.ogg, mono), pack atlases, keep R8 + resource shrinking on.

## Validation
Before/after table (FPS min/avg, worst frame ms, render calls, heap, cold start). `:core:test` green.

## Common failures
- Measuring a debug build (slower, no R8) and "fixing" non-issues.
- Emulator GPU numbers that don't match phones - confirm on hardware.
- vsync caps at 60: "FPS 60" can hide frame spikes; track the worst frame time.

## Deliverables
Numbers before/after, root cause, change, residual risk.

## References
- https://libgdx.com/wiki/graphics/profiling
- https://developer.android.com/topic/performance/vitals
