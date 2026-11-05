---
name: smoke-check
description: Fast go/no-go gate for a libGDX Android game before handing a build to testers or cutting a release - unit tests, debug build, desktop launch, store-capture run through the critical path, then a short manual device checklist (launch, play a level, win, ads with test IDs, back button, rotate, background/resume). Use after a feature lands or before any closed-testing build.
argument-hint: "<game> [quick]"
---

# Smoke check

Adapted from Claude Code Game Studios' `smoke-check` (MIT, see `.claude/THIRD_PARTY.md`).
**A build that fails the smoke check does not go to testers.**

## Automated (run all, from the workspace root)

```powershell
.\tools\android-build\build-android.ps1 -Project <game> -Task Test     # JUnit suite
.\tools\android-build\build-android.ps1 -Project <game> -Task Debug    # APK compiles + packs
```
Then, from `games\<game>`, unless `quick`:
```powershell
.\gradlew.bat :lwjgl3:storeCapture "-PcaptureOut=$env:TEMP\smoke\<game>" -q   # scripted critical path
```
The capture run plays home -> level -> hint -> win -> world -> settings -> shop. If it
completes and the PNGs look right, the critical path works on desktop.

## Manual (on a device or emulator; ask the user to confirm each)

| # | Check |
|---|---|
| 1 | Cold launch reaches home in under 3 s, no consent loop |
| 2 | Play and win a level; stars and coins update once |
| 3 | Rewarded ad (test ID) grants exactly once; closing early grants nothing |
| 4 | Interstitial only at a natural break, never mid-move |
| 5 | Android back on every screen goes back or home; never exits silently mid-level |
| 6 | Rotate or unfold mid-level: board re-lays out, no state lost |
| 7 | Home button, wait, resume: no black textures, music resumes correctly |
| 8 | Airplane mode: game fully playable, ad buttons degrade gracefully |

Untested items are **NOT RUN**, not PASS.

## Verdict

- **PASS**: every automated step green, every manual item confirmed.
- **PASS WITH WARNINGS**: automated green, some manual items NOT RUN (list them).
- **FAIL**: any automated failure or any manual item failing. Report it, do not auto-fix.
