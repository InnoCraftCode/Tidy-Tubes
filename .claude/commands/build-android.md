---
description: Build a game for Android (Debug, Install, Release APK or Play AAB) and report the artifact
argument-hint: "<project> [Debug|Install|Release|Bundle]"
---

Build: $ARGUMENTS (default task: Debug)

Run `.\tools\android-build\build-android.ps1 -Project <project> -Task <task>` and report the artifact
path, size, and any warnings (unsigned, targetSdk < 36). On failure apply the android-build-and-export
skill's failure table before reading more logs. Never upload the result anywhere.
