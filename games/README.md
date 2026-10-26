# games/

One folder per game, each a self-contained Gradle project created from `templates/libgdx-2d`:

```powershell
.\tools\scripts\new-game.ps1 -Name my-game -PackageId com.ben.laama.mygame -DisplayName "My Game"
.\tools\android-build\build-android.ps1 -Project my-game -Task Test
```

## Registry
| Game | Location | Package | Status | Notes |
|---|---|---|---|---|
| Ball Sort | `sortpuzzle/` (moved from the workspace root on 2026-10-21) | `com.ben.laama.sortpuzzle` | Playable, 70 tests green, debug APK builds | NoOp ads only; targetSdk 35 -> must be 36 before Play release. See its README. |
| (from template) | `mon-jeu/` | see its `android/build.gradle.kts` | Created from the template | Rename/adapt or delete if it was only a trial. |

Each game has its Google Play kit (images + Word listing) in `../play-store/<same name>/`.

In Android Studio, open games that moved from their new folder (File > Open); the old entry in
"Recent projects" points to a path that no longer exists.

Add a row when you create a game.
