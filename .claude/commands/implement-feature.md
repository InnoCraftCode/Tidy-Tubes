---
description: Implement a gameplay feature in a game following the logic/screen split, with tests
argument-hint: "<project> <feature description>"
---

Feature request: $ARGUMENTS

Use the libgdx-game-developer approach (libgdx-game-development skill):
1. Read the project README + GameConfig; Grep for the touch points; read only those files.
2. Rules in logic/model with JUnit tests; screens only draw and route input.
3. Validate narrow-to-broad: the new test, `:core:test`, then `:android:assembleDebug` if Android-visible.
4. Involve a specialist agent only if the feature touches ads (ads-monetization-agent) or the
   Android module (senior-android-developer).
End with the task summary format from CLAUDE.md.
