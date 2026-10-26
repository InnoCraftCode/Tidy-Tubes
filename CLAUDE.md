# mobile-android: Android game workspace

Reusable environment for building, testing, monetizing and publishing Android games.
OS: Windows 11, shell: Windows PowerShell 5.1 (no `&&`, no `?:`; use `;` and `if`).

## Stack (decided, see config/architecture/ADR-001-engine.md)
- **libGDX 1.13.1 + Java 21 toolchain**, Gradle 8.14.3 wrapper, AGP 8.11.1. Core compiles to Java 17 (D8).
- Ads: **AdMob GMA Next-Gen SDK 1.6.0 + UMP 4.0.0**, always behind the `AdProvider` interface.
- Godot/Unity are NOT installed and not used. Don't propose them unless asked.
- **Never change a project's Gradle wrapper to 9.x**: AGP 8.x fails on it (happened once, via VS Code's Java extension).

## Layout
| Path | What |
|---|---|
| `games/sortpuzzle/` | First game (Ball Sort), most mature code: reference for patterns. Read its README first. |
| `games/<name>/` | One Gradle project per game; new ones created only via `tools/scripts/new-game.ps1` |
| `play-store/<name>/` | Google Play upload kit per game: icon, feature graphic, screenshots, Word listing (from `listing.json`) |
| `templates/libgdx-2d/` | Source template (playable, AdMob-wired). Change it only to improve every future game. |
| `tools/` | Deterministic scripts: environment check, validation, build, new game |
| `config/` | ADRs, coding standards, release checklists (canonical checklists live here) |
| `shared/` | Reusable design, monetization, privacy, testing references |
| `docs/` | Human guides. `reports/` audit + validation reports |
| `.claude/` | Agents, skills, commands, settings |

## Commands (run from the workspace root)
```powershell
.\tools\environment-check\check-environment.ps1                       # prerequisites
.\tools\validation\validate-workspace.ps1                              # structure, frontmatter, secrets, SDK levels
.\tools\scripts\new-game.ps1 -Name my-game -PackageId com.ben.laama.mygame -DisplayName "My Game"
.\tools\android-build\build-android.ps1 -Project <name> -Task Test     # Test|Desktop|Debug|Install|Release|Bundle
.\tools\store\build-listing-doc.ps1 -Game <name>                       # Word listing from play-store\<name>\listing.json
.\tools\store\capture-desktop.ps1 -Game <name> [-Video]                 # store screenshots (+ promo videos) without a device
.\tools\store\render-brand-assets.ps1 -Game <name>                      # icon 512, launcher icons, feature graphic (GDI+)
.\tools\store\validate-store-assets.ps1 -Game <name>                    # Play specs check -> JSON report (exit 1 on FAIL)
.\tools\store\export-store-assets.ps1 -Game <name>                      # final kit in store-assets\ + validation
```
Store texts: edit `play-store/<name>/listing.json` (never only the .docx), then regenerate the Word file.
If execution policy blocks scripts: `powershell -ExecutionPolicy Bypass -File <script> ...`.
Gradle from a project folder: `.\gradlew.bat :core:test`, `:lwjgl3:run`, `:android:assembleDebug`, `:android:bundleRelease`.

## Non-negotiable rules
1. Game rules live in plain-Java classes (`logic/`, `model/`) with JUnit tests. Screens draw and route input only.
2. Ads only through `AdProvider`. Debug builds use Google demo IDs; production IDs only in git-ignored `admob.properties`.
3. Rewards are granted once, only after the SDK confirms (`RewardGate`). Interstitials only at natural breaks, paced (`AdPacing`).
4. Never commit or print keystores, passwords, `keystore.properties`, `.env`. Never click real ads.
5. Never upload, publish or release to Google Play, or create Play/AdMob resources, without explicit user authorization.
6. Ask before installs, SDK license acceptance, PATH/env changes, or deleting user files. Back up before destructive edits.
7. Verify SDK/API/CLI details against official docs or installed tooling (`javap`, `--help`); never invent them.
8. Google Play currently requires **targetSdk 36** (since 2026-08-31). Projects on 35 must be raised before release.

## Token discipline
- Never read or search: `build/`, `bin/`, `.gradle/`, `android/libs/`, `.idea/`, `*.apk`, `*.aab`, `*.jar`, images.
- Start from a project's README and `GameConfig`; open only the files the task touches. Prefer Grep over reading.
- Run the narrowest check first (`:core:test --tests <Class>`), broader builds only when justified.
- Pick ONE primary agent; add a specialist only for a distinct need. No agent fan-out for small tasks.
- Detailed procedures are in skills; load one only when its task comes up. Don't paste docs into chat.
- End each task with: goal, files changed, validation run + result, open issues, next action.

## Agent routing (one primary; specialists on demand)
| Task | Primary agent | Skill |
|---|---|---|
| New game, engine/architecture choice | game-architect | game-project-bootstrap |
| Gameplay code, screens, input, save | libgdx-game-developer | libgdx-game-development |
| Manifest, lifecycle, signing, Gradle/AGP, SDK levels | senior-android-developer | android-build-and-export |
| Core loop, levels, difficulty, retention | game-design-agent | rewarded-ads-design |
| Textures, atlases, fonts, draw calls | game-art-technical-agent | mobile-game-performance |
| AdMob, placements, consent wiring | ads-monetization-agent | admob-integration, privacy-and-consent |
| Store listing, AAB, release readiness | google-play-release-agent | google-play-publishing |
| Tests, regression, crash repro | qa-testing-agent | mobile-game-testing |
| FPS, memory, battery, startup | performance-agent | mobile-game-performance |
| Permissions, data safety, secrets | security-privacy-agent | privacy-and-consent |
| Review a diff before merge/release | code-review-agent | game-architecture-review |
| Visual redesign, design system, new screen, UX check | ui-ux-designer-agent | premium-ui-design |
| Naming, store texts/ASO, icon/feature graphic/screenshots/video | brand-aso-agent | store-asset-production |
| Independent Google Play policy audit (read-only) | play-compliance-auditor-agent | privacy-and-consent |

Slash commands: `/audit /new-game /implement-feature /review /test /optimize /build-android /configure-ads /prepare-play-store`.
