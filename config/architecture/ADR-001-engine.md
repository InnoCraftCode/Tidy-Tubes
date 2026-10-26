# ADR-001: libGDX as the primary game engine

- Status: accepted (2026-10-21)
- Context: workspace audit, `reports/workspace-audit.md`

## Context
The workspace already contains **Sortpuzzle**, a complete libGDX 1.13.1 game (Java, Gradle Kotlin DSL,
70 passing unit tests, Android + desktop modules, AdProvider abstraction, release signing). The
machine has JDK 21, Android Studio, Android SDK (platforms 35 and 37.0, build-tools 35/36). No
Godot or Unity installation exists. Target games: 2D casual, puzzle, arcade, hyper-casual; some
light 3D possible.

## Options considered
| | libGDX (Java) | Godot 4 (GDScript) | Unity (C#) |
|---|---|---|---|
| Already installed / in use | Yes, shipped-quality game exists | No | No |
| Extra install | None | Editor + Android export templates | Hub + Editor + Android module (several GB) |
| Text-only workflow (fits Claude Code) | Excellent: everything is code + Gradle | Good (scenes are text, editor still central) | Weak: scene/prefab assets, editor-driven |
| Unit testing | JUnit on pure Java, runs headless in seconds | GUT/gdUnit add-ons | Unity Test Framework inside editor |
| AdMob | Official Android SDK directly (Java) | Community plugin, version lag risk | Official Google Mobile Ads Unity plugin |
| 2D casual fit | Excellent | Excellent | Good, heavier runtime |
| Heavy 3D | Possible, manual pipeline | Good | Best |
| APK size (empty) | ~4 MB (Sortpuzzle debug: 3.9 MB) | ~25-35 MB | ~20-40 MB |
| Licence | Apache 2.0 | MIT | Proprietary (Personal tier conditions apply) |

## Decision
Use **libGDX** for all games by default. Reuse Sortpuzzle's proven patterns and the template
`templates/libgdx-2d`.

Godot and Unity are documented as optional; install only after a new ADR names a concrete need
(e.g. a 3D game whose content pipeline outgrows libGDX, or an asset-store dependency). Native
Kotlin is used only for Android-side integrations, inside the `android` module.

## Consequences
- No new toolchain to maintain; builds and tests run from PowerShell and in Claude Code.
- Ads integrate with Google's official Android SDK, no engine plugin lag.
- No visual scene editor: UI built in code (scene2d), art via atlases or procedural Pixmaps.
- 3D games need more hand-work; see `.claude/skills/libgdx-game-development/3d.md`.
