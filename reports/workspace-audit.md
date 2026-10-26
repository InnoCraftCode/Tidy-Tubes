# Workspace audit - 2026-10-21

Root: `E:\mobile andoird` (folder name keeps its original spelling). OS: Windows 11 Pro (build 26300),
Windows PowerShell 5.1. Not a Git repository (Git not installed).

## Existing components (before restructuring)
| Item | Finding | Action |
|---|---|---|
| `Sortpuzzle/` | Complete libGDX 1.13.1 ball-sort game: `core` (model/logic/screens/ui/save/audio/ads), `lwjgl3`, `android`. 70 JUnit tests, R8 rules, signing via `keystore.properties`, `AdProvider` + `NoOpAdProvider`. Detailed README. | Preserved; later moved to `games/sortpuzzle/` at the user's request (2026-10-21). |
| `Sortpuzzle/core/.../Main.java:38` | **Compile error**: `this(new  ());` (class name deleted). Build and tests failed. | Fixed to `new NoOpAdProvider()` (only possible meaning: import + "no ads" comment). 70/70 tests pass. |
| `Sortpuzzle/gradle/wrapper/gradle-wrapper.properties` | Was 8.14.3 at audit start; rewritten to **9.7.1** at 15:14 during the session by the VS Code Red Hat Java extension (bundled JDK 25 can't run Gradle 8.14). AGP 8.11.1 fails on Gradle 9. | Restored to 8.14.3 (jar/scripts untouched). Added `.vscode/settings.json` -> `java.import.gradle.java.home` = JDK 21 to prevent it. |
| `Sortpuzzle/Claude outputs/AnimatedBackground.java` | Unintegrated earlier Claude output (animated ball backdrop, package `ui`). Not on the build path. | Preserved. Integrate into `core/.../ui/` if wanted, or delete. |
| `Sortpuzzle/Claude outputs/clean-old-scaffold.bat` | Obsolete one-shot cleanup for an old Android Studio scaffold (refers to `E:\Sortpuzzle`); deletes `.idea`, `.gradle`, `build`; self-deletes. Scaffold no longer present. | Preserved, **do not run**; safe to delete. |
| `Sortpuzzle/.idea`, `.gradle`, `build` | IDE/build caches (git-ignored). | Excluded from searches (`.vscode/settings.json`, CLAUDE.md). |
| `Sortpuzzle/android` targetSdk/compileSdk 35 | Google Play requires targetSdk 36 for new apps and updates since 2026-08-31. | Reported; needs platform 36 (not installed) - user approval. |
| Agents / skills / commands / scripts / docs at root | None existed. | Created. |

## Duplicate or obsolete
- `Claude outputs/` folder (above). No duplicate code otherwise.
- Godot/Unity: nothing installed, nothing to remove.

## Environment
See `dependency-inventory.md`. Required tooling present: JDK 21, Android SDK + build-tools, adb,
Android Studio. Missing: Git, SDK platform 36, cmdline-tools, any AVD, Claude CLI on PATH (VS Code
extension present).

## Recommended architecture (implemented)
libGDX as the single engine (ADR-001), AdMob Next-Gen + UMP behind `AdProvider` (ADR-002), one
self-contained Gradle project per game created from `templates/libgdx-2d`, deterministic PowerShell
tooling, concise CLAUDE.md with agent routing, procedures in skills. Details: `docs/architecture.md`.

## Dependencies that genuinely need installation (awaiting user approval)
1. Android SDK Platform 36 (Play target API) - via Android Studio SDK Manager (licence acceptance).
2. Git - `winget install --id Git.Git -e` (version history; strongly recommended).
3. Android SDK Command-line Tools + one AVD (optional; a physical phone works too).
