# Validation report - 2026-10-21

## Result
`tools\validation\validate-workspace.ps1 -RunTests` -> **0 failures, 2 warnings** (both: targetSdk 35 < 36).

## Selected engine and rationale
**libGDX 1.13.1 (Java, JDK 21 toolchain)** - see `config/architecture/ADR-001-engine.md`.
A complete libGDX game already existed, all its tooling was installed, it is text/code-first (best fit
for Claude Code), tests run headless in seconds, AdMob integrates with Google's official Android SDK,
and empty APKs are ~4 MB. Godot/Unity documented as optional, not installed, no templates (no concrete need).

## Files modified (existing user work)
| File | Change | Why |
|---|---|---|
| `Sortpuzzle/core/src/main/java/com/ben/laama/sortpuzzle/Main.java:38` | `new  ()` -> `new NoOpAdProvider()` | Compile error blocked every build/test |
| `Sortpuzzle/gradle/wrapper/gradle-wrapper.properties` | 9.7.1 -> 8.14.3 (original value) | Rewritten mid-session by the VS Code Java extension; AGP 8.11 can't run on Gradle 9 |
| `Sortpuzzle/core/bin`, `Sortpuzzle/lwjgl3/bin` | Removed | IDE compiler output generated during this session (only .class files + an identical resource copy) |

## Existing components preserved
Paths below say `Sortpuzzle/` (the location at validation time). The project was then moved, at the
user's request, to **`games/sortpuzzle/`** (same content; tools still accept `-Project Sortpuzzle`).

Entire `Sortpuzzle/` project (sources, tests, README, signing setup, icons, `.idea`), including
`Sortpuzzle/Claude outputs/` (AnimatedBackground.java unintegrated; clean-old-scaffold.bat obsolete -
both left for the user to decide).

## Files created
- Root: `CLAUDE.md` (68 lines), `README.md`, `.gitignore`, `.env.example`, `.vscode/settings.json`
- `.claude/`: `settings.json`; 11 agents; 11 skills (+ `libgdx-game-development/3d.md`); 9 commands
- `config/`: ADR-001 engine, ADR-002 ads SDK, `coding-standards/java-libgdx.md`,
  `release-checklists/google-play-release.md`, `release-checklists/ads-integration.md`
- `templates/libgdx-2d/`: full Gradle project (core/lwjgl3/android), 11 Java sources, 3 test classes,
  AdMob Next-Gen + UMP provider, `admob.properties.example`, `keystore.properties.example`, README
- `shared/`: game-design template, ad placement spec, analytics taxonomy, privacy policy template,
  Data safety worksheet, device test matrix
- `games/README.md` (registry), `docs/` (8 guides), `reports/` (audit, inventory, this report)
- `tools/`: `environment-check/check-environment.ps1`, `validation/validate-workspace.ps1`,
  `android-build/build-android.ps1`, `scripts/new-game.ps1`, `scripts/common.ps1`

## Agents, skills, commands
- Agents: game-architect, libgdx-game-developer (replaces godot/unity developer agents),
  senior-android-developer, game-design-agent, game-art-technical-agent, ads-monetization-agent,
  google-play-release-agent, qa-testing-agent, performance-agent, security-privacy-agent, code-review-agent.
- Skills: game-project-bootstrap, libgdx-game-development (replaces godot-2d/3d; 3D in supporting file),
  android-build-and-export, mobile-game-performance, admob-integration, rewarded-ads-design,
  google-play-publishing, privacy-and-consent, mobile-game-testing, game-architecture-review,
  token-efficient-development. (unity-mobile-development intentionally not created.)
- Commands: /audit /new-game /implement-feature /review /test /optimize /build-android /configure-ads /prepare-play-store

## Discovery / configuration checks
- Frontmatter syntax checked against current Claude Code docs (skills, sub-agents, permissions).
- Claude Code 2.1.306 listed all 11 skills and 9 commands live in this session.
- Agents: frontmatter validated by script (name = file name, description, existing preloaded skills);
  in-session listing requires a session restart (new `.claude/agents` directory).
- `claude doctor`: no invalid-settings warnings; only "CLI not on PATH" (installation, not config).

## Build and test results
| Check | Result |
|---|---|
| Sortpuzzle `:core:test` | 70/70 pass (after Main.java fix) |
| Sortpuzzle `:android:assembleDebug` | OK, 3.9 MB APK |
| Template `:core:test` | 12/12 pass (RoundState, RewardGate, AdPacing) |
| Template `:android:assembleDebug` / `assembleRelease` / `bundleRelease` | OK: 10.6 MB debug APK, 1.4 MB R8 release APK (unsigned), 1.5 MB AAB (unsigned) |
| `new-game.ps1` end-to-end (smoke-test game: generate, rename, test, debug APK) | OK; smoke project then deleted |
| `check-environment.ps1` | exit 0 (required present; warnings listed below) |
| `-Pads.enabled=false` switch | `printAdConfig` reports ads disabled |
| Secret scan negative test | detects literal passwords and production AdMob IDs; ignores Gradle lookups/placeholders |

Not verified (no device/emulator available): running on a phone, test ads displaying, consent form,
desktop window (`:lwjgl3:run` compiles but was not launched interactively).

## Tools installed vs missing
Installed by this work: **nothing system-wide** (only Gradle-cached libraries: GMA Next-Gen 1.5.0, UMP 4.0.0).
Missing / recommended (need your approval): Android SDK Platform 36, Git, SDK Command-line Tools,
an AVD; optional `claude install` to put the CLI on PATH.

## Known limitations
- Both projects target API 35; Google Play has required 36 since 2026-08-31 (extension to 2026-11-01 possible).
- Sortpuzzle has no real ad provider yet (NoOp); porting steps are in the admob-integration skill.
- Release artifacts are unsigned until you create an upload key and `keystore.properties`.
- No Git: no history/rollback; `/review` needs you to name changed files.
- Banner ads are documented but not implemented (template layout reserves no strip).
- Template icon is a placeholder vector.

## Exact commands
```powershell
cd "E:\mobile andoird"
# new game
powershell -ExecutionPolicy Bypass -File .\tools\scripts\new-game.ps1 -Name my-game -PackageId com.ben.laama.mygame -DisplayName "My Game"
# tests
powershell -ExecutionPolicy Bypass -File .\tools\android-build\build-android.ps1 -Project my-game -Task Test
# Android packages
powershell -ExecutionPolicy Bypass -File .\tools\android-build\build-android.ps1 -Project my-game -Task Debug
powershell -ExecutionPolicy Bypass -File .\tools\android-build\build-android.ps1 -Project my-game -Task Bundle
# workspace health
powershell -ExecutionPolicy Bypass -File .\tools\validation\validate-workspace.ps1 -RunTests
```

## Where token usage is reduced
- CLAUDE.md is 68 lines; procedures load only when a skill is triggered (descriptions only in context).
- Routing table picks one agent per task; agents preload only their own skill; no duplicated rules.
- Deterministic scripts replace exploratory checks (env, validation, builds) with short structured output.
- Explicit exclusions (build/, bin/, .gradle/, android/libs/, .idea/, binaries) in CLAUDE.md and VS Code.
- Template + generator remove the cost of re-scaffolding and re-deriving ad/consent code per game.
- Verified facts (SDK versions, signatures, Play requirements, the Gradle 9 pitfall) recorded once,
  so they are not re-researched each session.
These reduce unnecessary context and repeated work; they do not make sessions free.
