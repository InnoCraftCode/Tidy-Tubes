# Agents and commands guide

## How to use them in Claude Code
- **Slash commands** (fastest): type `/` in the Claude Code prompt, e.g. `/test Sortpuzzle` or
  `/new-game color-dash com.ben.laama.colordash "Color Dash"`.
- **Agents**: ask in plain words ("use the code-review-agent on ...") or type `@` and pick one
  (`@agent-qa-testing-agent`). Claude also delegates automatically from each agent's description.
- **Skills**: load automatically when the task matches; force one with `/<skill-name>`
  (e.g. `/admob-integration Sortpuzzle`). Catalog: [skills-catalog.md](skills-catalog.md).

Rule: one primary agent per task; specialists only for a distinct need. The routing table is in
[CLAUDE.md](../CLAUDE.md).

## Agents
| Agent | Use it for | Writes code? |
|---|---|---|
| game-architect | new game structure, cross-cutting decisions, ADRs | plans + small edits |
| libgdx-game-developer | gameplay, screens, UI, save, audio | yes |
| senior-android-developer | Gradle/AGP, manifest, signing, SDK levels, logcat | yes |
| game-design-agent | loop, levels, difficulty, economy, ad placement specs | specs only |
| game-art-technical-agent | atlases, fonts, shaders, asset size, draw calls | yes |
| ads-monetization-agent | AdMob wiring, consent, pacing, IDs | yes |
| google-play-release-agent | release checklist, AAB, listing drafts (never uploads) | docs + version bumps |
| qa-testing-agent | tests, scenarios, crash repro | tests |
| performance-agent | FPS, GC, memory, startup, battery | measured fixes |
| security-privacy-agent | permissions, data safety, secrets | read-only |
| code-review-agent | review before merge/release | read-only |
| ui-ux-designer-agent | design system, screen layouts, UX/accessibility review on real renders | specs + UI code review |
| brand-aso-agent | naming, store texts, screenshot captions, icon/feature/video briefs and exports | listing.json + assets |
| play-compliance-auditor-agent | independent Play policy audit after a redesign or before release | read-only report |

Godot/Unity agents were intentionally not created: those engines are not used (ADR-001).

## Commands
| Command | Does |
|---|---|
| `/audit [project]` | env check + validation (+ review of one game) |
| `/new-game <name> <pkg> "<Name>"` | create a game from the template |
| `/implement-feature <project> <desc>` | feature with tests, narrow-to-broad validation |
| `/review <project or files>` | ranked findings, no edits |
| `/test <project> [pattern]` | run tests, summarise failures |
| `/optimize <project> <symptom>` | measure-first performance work |
| `/build-android <project> [Debug/Install/Release/Bundle]` | build + artifact report |
| `/configure-ads <project>` | AdMob/UMP wiring from the template |
| `/prepare-play-store <project>` | checklist, AAB, listing draft - never uploads |

If a newly created agent or skill doesn't appear, restart the Claude Code session (new top-level
folders under `.claude/` are picked up on restart; `/reload-skills` for skills).
