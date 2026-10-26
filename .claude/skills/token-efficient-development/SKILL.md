---
name: token-efficient-development
description: Working method that keeps Claude Code sessions small and cheap in this workspace - targeted reads, script-first checks, single-agent default, narrow tests, concise summaries. Use at the start of a long or multi-step task, or when a session is getting large.
user-invocable: true
---

# Token-efficient development

Local agents and skills do not make tokens free; they avoid re-explaining, re-reading and
irrelevant work. Apply these habits:

## Reading
- Start with the project's README and `GameConfig`; they summarise the architecture.
- Grep/Glob first, then Read only the needed range (`offset`/`limit` for files > 300 lines;
  e.g. Sortpuzzle `GameScreen.java` is ~30 KB - never read it whole for a small change).
- Never open: `build/`, `.gradle/`, `android/libs/`, `.idea/`, `*.apk|aab|jar|webp|png`, Gradle caches.
- Don't re-read a file you just edited; the edit tool fails loudly if it didn't apply.

## Running
- Deterministic scripts beat ad-hoc exploration: `check-environment.ps1`, `validate-workspace.ps1`,
  `build-android.ps1`. Their output is short and structured.
- Filter command output: `| Select-String 'FAILED|BUILD|error'` instead of full Gradle logs.
- Narrowest test first (`--tests "*X*"`), module suite next, Android builds only when Android code changed.
- Long builds in the background; don't poll.

## Delegating
- One primary agent per task (routing table in CLAUDE.md). Add a specialist only for a distinct
  question (e.g. policy check). Don't fan out agents for a small change.
- Give subagents the exact files and question; ask for findings, not file dumps.

## Writing
- Small diffs with Edit; no rewriting whole files for a few lines.
- Don't paste docs into chat or files; link the official page and the skill.
- Don't repeat rules from CLAUDE.md in other files; link instead.

## Asking
Don't ask what the docs already answer (stack, commands, policies are in CLAUDE.md/skills). Ask only
for decisions, credentials, or authorization (installs, deletions, Play/AdMob actions).

## Task summary (end of every task)
```
Goal: ...
Changed: path1, path2
Validated: <command> -> <result>
Open: ...
Next: ...
```
