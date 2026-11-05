---
name: hotfix
description: Emergency fix workflow for a game already on Google Play - confirm severity, branch, minimal fix with a regression test seen failing first, version bump in every synced place, release notes in all store languages, signed AAB, smoke check, then hand over for a staged rollout. Never uploads. Use for a crash spike, a broken progression, an ads-policy problem or data loss in production.
argument-hint: "<game> <bug summary>"
---

# Hotfix

Adapted from Claude Code Game Studios' `hotfix` (MIT, see `.claude/THIRD_PARTY.md`).

1. **Qualify.** Only S1/S2 bugs from the `bug-report` triage justify a hotfix. Anything else
   waits for the next release.
2. **Branch** from the released commit: `git switch -c hotfix/<version>`. Ask before
   committing; never push or publish without explicit authorisation (CLAUDE.md rule 5).
3. **Reproduce as a test.** Write the regression test, run it, watch it fail.
4. **Smallest possible fix.** No refactors, no unrelated changes, no new features. Test
   goes green, then the full `:core:test` suite.
5. **Bump the version** following the game's versioning rule (for Tidy Tubes: versionCode
   +1 and versionName in every synced place, plus release notes in all store languages).
   Check the game README / release checklist for the exact list.
6. **Build**: `.\tools\android-build\build-android.ps1 -Project <game> -Task Bundle`.
7. **Smoke check** with the `smoke-check` skill (at least the automated part and manual
   items related to the bug).
8. **Hand over**: AAB path, versionCode/name, release notes, and a recommendation for a
   staged rollout (e.g. 20 % -> 100 % after 24 h without new crashes). The user uploads.
9. Merge the hotfix branch back to `main` once released.
