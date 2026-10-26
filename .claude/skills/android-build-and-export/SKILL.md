---
name: android-build-and-export
description: Build, sign and export libGDX games for Android - debug APK, release APK, Play AAB, signing with an upload key, SDK levels, R8 rules, Gradle/AGP versions, installing on devices, reading logcat. Use for build failures, release builds, or Android configuration changes.
argument-hint: "<project> [Debug|Release|Bundle|Install]"
---

# Android build and export

## When to use
Any Gradle/AGP/manifest/signing/SDK-level task, build failure, or producing an installable/uploadable
artifact. Not for gameplay code.

## Inputs
Project name or path; build type; whether `keystore.properties` exists (check with Test-Path - never
open it; settings deny reading it).

## Workflow
1. Build through the script (sets ANDROID_HOME, prints artifacts and release warnings):
   `.\tools\android-build\build-android.ps1 -Project <p> -Task Debug|Install|Release|Bundle`
2. Release signing (one time per game, done by the user):
   - Create the upload key OUTSIDE the repo with `keytool -genkeypair ... -alias upload` (command in
     `keystore.properties.example`), store passwords in a password manager.
   - Copy `keystore.properties.example` -> `keystore.properties`; enrol in Play App Signing on first upload.
3. Versioning: bump `app.versionCode` (template, `gradle.properties`) or `versionCode` (Sortpuzzle,
   `android/build.gradle.kts`) on every upload; `versionName` is user-visible.
4. SDK levels: Play requires targetSdk 36 since 2026-08-31 (extension possible to 2026-11-01).
   Install "Android 16 (API 36)" in SDK Manager, set compileSdk/targetSdk 36, rebuild, retest
   edge-to-edge/back behaviour on an API 36 device or emulator.
5. Install and observe:
   ```powershell
   $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
   & $adb devices
   & $adb install -r games\<p>\android\build\outputs\apk\debug\android-debug.apk
   & $adb logcat -s AndroidRuntime:E libGDX:I Ads:I
   ```

## Standards
- Gradle wrapper 8.14.3 with AGP 8.11.1. AGP 8.x does not run on Gradle 9 - never let an IDE bump it.
- Upgrades (AGP/Gradle/libGDX) are separate, tested changes with the compatibility table checked:
  https://developer.android.com/build/releases/gradle-plugin#updating-gradle
- `minSdk 24` (AdMob requirement), ABIs: armeabi-v7a, arm64-v8a, x86, x86_64 (libGDX natives).
- Release: `isMinifyEnabled = true`, `isShrinkResources = true`, keep rules for reflective classes.
- No secrets in any Gradle file; signing data only from git-ignored `keystore.properties`.

## Validation
`:android:assembleDebug` succeeds; `:android:bundleRelease` produces `android-release.aab`;
with a keystore: `& "$env:JAVA_HOME\bin\jarsigner.exe" -verify -verbose:summary <aab>` shows it signed.

## Common failures
| Symptom | Cause / fix |
|---|---|
| `Failed to apply plugin 'com.android.internal.application'` ... `InternalProblems` | Gradle 9 with AGP 8: restore wrapper to 8.14.3 |
| `SDK location not found` | local.properties missing: set `ANDROID_HOME` or rerun new-game/Android Studio sync |
| `Failed to find target with hash string 'android-36'` | Platform 36 not installed |
| Release crashes, debug fine | R8 stripped a reflective class: add `-keep` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Signature differs: `adb uninstall <appId>` (loses local data) |
| Play: "You uploaded an APK or Android App Bundle that was signed in debug mode" | keystore.properties missing |

## Deliverables
Artifact path + size, build log tail, list of warnings that matter for Play.

## Not for
Uploading to Play (google-play-publishing; requires explicit user authorization).

## References
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/google/play/requirements/target-sdk
- https://developer.android.com/build/shrink-code
