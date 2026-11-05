---
name: playtest-report
description: Produce a blank mobile playtest form, or turn raw playtest notes, closed-testing feedback or store reviews into a structured report - first-minute comprehension, flow, pain points, delight, difficulty spikes, ad annoyance, bugs - with prioritised actions. Use before or after a playtest session or a closed-testing round.
argument-hint: "new | analyze <path-to-notes>"
---

# Playtest report

Adapted from Claude Code Game Studios' `playtest-report` (MIT, see `.claude/THIRD_PARTY.md`)
for casual mobile games.

## `new`: blank form to hand a tester

```markdown
# Playtest: <game> <version>
- Date / tester / device / orientation / session length:
- First time or returning player:

## First minute
- Understood what to do without help? yes / partly / no
- First tap was on:
- Feeling: calm / engaged / confused / bored / frustrated

## Flow (levels played: from - to)
- Worked well:
- Got stuck at (level, why):
- Felt too easy / too hard at:
- Moments of delight:

## Ads and rewards
- Any ad felt intrusive? when?
- Watched a rewarded ad voluntarily? why or why not?

## Bugs (what, where, how often)

## Would play again tomorrow? 1-5, why
```

## `analyze <notes>`: structured findings

1. Read the notes (or the reviews / closed-testing feedback export).
2. Group observations into: comprehension, difficulty curve, controls and feel, UI and
   responsiveness, ads and economy, bugs, retention signals.
3. Mark each with how many testers raised it. One tester is an anecdote, three are a pattern.
4. Output a table of **Top actions** (at most 5), each with evidence, the affected file or
   system, and the owner agent from CLAUDE.md routing. Bugs go through the `bug-report` skill.

Keep testers' names and e-mails out of files committed to git.
