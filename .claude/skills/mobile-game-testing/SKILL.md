---
name: mobile-game-testing
description: Test libGDX Android games - JUnit 5 for pure logic, deterministic seeds, manual device scenarios for lifecycle/ads/back button, build verification, crash and ANR reproduction with logcat. Use after a feature, before a release, or when investigating a bug.
argument-hint: "<project> [TestClass]"
---

# Mobile game testing

## When to use
Writing tests, verifying a build, reproducing a crash. Not for performance profiling (mobile-game-performance).

## Inputs
Project, change or bug description, device/emulator availability.

## Test layers
1. **Unit (automated, every change)** - `core/src/test`, JUnit 5, no libGDX graphics. Rules,
   generators, solvers, pacing, rewards. Inject `Random`/clocks for determinism.
2. **Desktop smoke (manual, fast)** - `:lwjgl3:run`: screens, input, flows. NoOp ads grant rewards instantly.
3. **Device scenarios (manual, before release)** - checklist below on at least one real phone.
4. **Build verification** - debug APK, release APK/AAB build; release install on device.

## Workflow
1. Narrowest first: `.\gradlew.bat :core:test --tests "*ThingTest"`; then `:core:test`.
2. Bug in logic -> write the failing test, fix, keep the test.
3. Bug on device -> capture logs:
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb logcat -c; & $adb logcat -v time *:W > "$env:TEMP\game-logcat.txt"
   ```
   Read only the `FATAL EXCEPTION` block and the lines above it. ANR traces: `adb bugreport` (large; extract `anr/` only).
4. Record scenario results in the format of `shared/testing/device-test-matrix.md`.

## Device scenario checklist
- Cold start to playable < 3 s; no black frame.
- Home button mid-level, return -> state intact. Kill app from recents -> progress saved.
- Back button: in-level -> menu/pause; on home -> exit (no trap).
- Screen off/on, incoming notification, low battery dialog during play.
- Rewarded: complete -> reward once; close early -> nothing; offline -> button hidden or graceful.
- Interstitial: only at breaks, respects caps; offline -> game continues.
- Consent form (EEA debug geography) and privacy options button.
- Small screen (5") and tall (20:9) layouts; font legibility.
- Release build (R8): save/load still works after update install over previous version.

## Standards
`@DisplayName` sentences describing behaviour; one behaviour per test; no sleeps; no network.

## Validation
Command + "BUILD SUCCESSFUL"/test count lines; scenario table with pass/fail.

## Common failures
- Tests touching `Gdx.files`/graphics -> NPE without a backend: move logic out or guard `Gdx.app != null`.
- Flaky tests from unseeded `Random`.
- Release-only bugs from R8: always run the release build on a device before shipping.

## Deliverables
New/updated tests, results, repro steps + severity for failures.

## References
- https://junit.org/junit5/docs/current/user-guide/
- https://developer.android.com/tools/logcat
- https://developer.android.com/topic/performance/vitals/anr
