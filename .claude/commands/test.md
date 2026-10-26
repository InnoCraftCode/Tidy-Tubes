---
description: Run a game's tests narrow-to-broad and report results (optionally one test class)
argument-hint: "<project> [TestClassPattern]"
---

Arguments: $ARGUMENTS (first = project, optional second = test class pattern).

- With a pattern: from the project folder run `.\gradlew.bat :core:test --tests "*<pattern>*" --console=plain`.
- Otherwise: `.\tools\android-build\build-android.ps1 -Project <project> -Task Test`.
Filter output to FAILED/PASSED counts and BUILD lines. On failure, show the assertion message and
the test source lines involved, then propose the fix (mobile-game-testing skill). Don't run Android
builds unless asked.
