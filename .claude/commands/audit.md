---
description: Audit the workspace or one game - environment, structure, secrets, SDK levels, tests - and update reports/
argument-hint: "[project]"
---

Audit target: $ARGUMENTS (empty = whole workspace).

1. Run `.\tools\environment-check\check-environment.ps1` and `.\tools\validation\validate-workspace.ps1`.
2. If a project was named, also run `.\tools\android-build\build-android.ps1 -Project $ARGUMENTS -Task Test`
   and apply the game-architecture-review skill to it (targeted Greps, no full-file dumps).
3. Report: OK / warnings / failures, each with the fix. Update `reports/workspace-audit.md` only if
   the findings changed. Don't install or delete anything.
