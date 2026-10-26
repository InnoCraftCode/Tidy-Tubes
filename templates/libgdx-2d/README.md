# libGDX 2D game template

A minimal, playable Android game (tap the targets before they shrink, 30-second rounds) wired for
AdMob from day one. Copy it with `tools\scripts\new-game.ps1`; don't edit the template to make a game.

| | |
|---|---|
| Engine | libGDX 1.13.1, Java 21 toolchain (core compiled to Java 17 for D8) |
| Build | Gradle 8.14.3 wrapper, AGP 8.11.1, Kotlin DSL |
| Modules | `core` (all game code), `lwjgl3` (desktop, for iteration), `android` (shipping target) |
| Ads | GMA Next-Gen SDK 1.6.0 + UMP 4.0.0 behind the `AdProvider` interface |

## Commands (from the project folder)

```powershell
.\gradlew.bat :core:test                  # unit tests, no GPU needed
.\gradlew.bat :lwjgl3:run                 # play on the desktop (NoOp ads: rewards granted instantly)
.\gradlew.bat :android:assembleDebug      # debug APK with Google demo ads
.\gradlew.bat :android:installDebug       # install on a connected device/emulator
.\gradlew.bat :android:bundleRelease      # release AAB (signed if keystore.properties exists)
.\gradlew.bat :android:assembleDebug -Pads.enabled=false   # no ad SDK traffic at all
```

## Layout

```
core/src/main/java/.../
  GameConfig.java        every tuning value, ad pacing included
  TemplateGame.java      Game subclass: shared batch/shapes/font, prefs, AdProvider, AdPacing
  logic/                 pure Java, no libGDX — RoundState, Target (unit-tested)
  ads/                   AdProvider, NoOpAdProvider, RewardGate, AdPacing (the last two unit-tested)
  screens/PlayScreen     draws and routes input; holds no rules
android/src/main/java/.../android/
  AndroidLauncher        picks AdMobAdProvider or NoOp from BuildConfig.ADS_ENABLED
  AdMobAdProvider        consent (UMP) → SDK init on a worker thread → preload, backoff retry
```

Rule of the template: game rules live in plain-Java classes with tests; screens only draw.

## Ads configuration

- **Debug** always uses Google's demo unit IDs (Gradle injects them). Safe to tap.
- **Release** reads `admob.properties` (copy `admob.properties.example`). Missing file → release
  builds ship with ads disabled, never with demo IDs.
- Placements: rewarded "+10s" on game over (opt-in, once per round); interstitial after "Play again",
  at most every 3rd round and 2 minutes apart (`GameConfig`, enforced by `AdPacing`).
- Rewards are granted exactly once and only after the SDK reports them earned (`RewardGate`).
- Privacy options button appears automatically where UMP requires it.

Full procedure: `.claude/skills/admob-integration/SKILL.md` at the workspace root.

## Before a Play release

1. Change `applicationId`/`namespace` (the new-game script does it) and the app name.
2. Raise `android.compileSdk`/`android.targetSdk` to 36 in `gradle.properties` once the API 36
   platform is installed — Play requires targetSdk 36 since 2026-08-31.
3. Replace the placeholder icon; add `keystore.properties`; bump `app.versionCode`.
4. Walk through `config/release-checklists/google-play-release.md`.
