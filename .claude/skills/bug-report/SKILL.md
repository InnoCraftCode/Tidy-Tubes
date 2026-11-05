---
name: bug-report
description: Turn a player complaint, a Play Console crash/ANR, a logcat or a vague "it's broken" into a structured, reproducible bug report with severity, then triage a list of bugs into fix-now / next-release / backlog. Use when a bug report arrives, before a hotfix decision, or to clean up a pile of issues.
argument-hint: "<game> <description | path-to-log>"
---

# Bug report and triage

Adapted from Claude Code Game Studios' `bug-report` and `bug-triage` skills (MIT, see
`.claude/THIRD_PARTY.md`).

## 1. Write the report

```markdown
# BUG: <one-line symptom, what the player sees>
- Game / version: <game> <versionName> (versionCode N)
- Device / Android: <model> / API <n>, orientation, foldable state
- Frequency: always | often | rare (n of m tries)
- Severity: S1 crash/data loss/ads-policy | S2 blocks progress | S3 wrong but workable | S4 cosmetic

## Steps to reproduce
1. ...
## Expected
## Actual
## Evidence
logcat excerpt (keep only the stack and 10 lines around it), screenshot path, save state
## Suspected area
file:line and why, or "unknown". Do not guess a cause without evidence.
```

Reproduce before you diagnose: on desktop (`:lwjgl3:run`), or as a failing JUnit test when
the bug is in `logic/` or `model/`. That test becomes the regression test
(`.claude/rules/test-standards.md`: watch it fail first).

Never paste keystores, `admob.properties`, account e-mails or other secrets from logs.

## 2. Triage a list

| Severity | Frequency always/often | rare |
|---|---|---|
| S1 | fix now, consider `hotfix` | fix now |
| S2 | fix now | next release |
| S3 | next release | backlog |
| S4 | backlog | backlog |

Raise priority for anything that touches ads policy, consent, payments or saves. These are
store and trust risks even when rare. Output: the table of bugs with decision and owner agent
(see CLAUDE.md routing).
