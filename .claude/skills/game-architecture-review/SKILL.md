---
name: game-architecture-review
description: Review a libGDX game's code or a diff against this workspace's architecture rules - logic/screen separation, threading, resource disposal, ad integration safety, release (R8) safety, dependencies and scope. Use before merging significant changes or a release, or when asked to audit a game.
argument-hint: "<project or path>"
---

# Game architecture review

## When to use
Reviewing a change set or auditing a whole game. Read-only; produce findings, don't refactor.

## Inputs
Target (project, folder, or list of changed files). Without Git there is no `git diff`: ask which files
changed or compare against the template/README description.

## Workflow
1. Read the project README and the file list (Glob `core/src/**/*.java`); skip build outputs.
2. Check each rule below with targeted Greps before opening files:
   - Logic purity: `Grep "import com.badlogic" core/src/main/java/**/logic/**` and `**/model/**` -> must be empty.
   - Per-frame allocation: `Grep "new (Vector2|Rectangle|Color|GlyphLayout)"` inside `render(`/`act(` methods.
   - Disposal: every class creating `Texture|Pixmap|BitmapFont|Skin|Stage|ShapeRenderer|SpriteBatch` has `dispose()`.
   - Threads: `new Thread|Executor` results returned via `postRunnable`; GL calls only on render thread.
   - Ads: rewards only via `RewardGate`-like single settlement; interstitials only from break points; no
     hard-coded production `ca-app-pub-` IDs outside `admob.properties`.
   - R8: classes used with `Json`/reflection have `-keep` rules.
   - Config: magic numbers in screens that belong in `GameConfig`.
   - Tests: each logic class has a test; tests deterministic.
3. Run `.\gradlew.bat :core:test` if logic changed.
4. Rank findings: Critical (crash/data loss/policy), Major (bug/regression), Minor (maintainability).

## Output format
```
[Critical] path/File.java:123 - what is wrong - when it breaks (concrete scenario) - fix
```
End with: tests run + result, and "No significant issues" if that is the case.

## Common false positives
- Allocation in `show()`/constructors is fine; only per-frame paths matter.
- `NoOpAdProvider` granting rewards is intentional (desktop testability).
- Sortpuzzle's `Main` is in package root, not `screens` - acceptable legacy layout.

## Deliverables
Ranked findings list with file:line, test evidence.

## Not for
Rewriting code (hand findings to libgdx-game-developer), performance measurement (performance-agent).
