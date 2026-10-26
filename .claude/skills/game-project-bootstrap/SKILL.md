---
name: game-project-bootstrap
description: Create a new Android game project in games/ from the libGDX template, rename package/app, verify it builds, and adapt it to a game concept. Use when the user wants to start a new game or prototype.
argument-hint: "<name> <package.id> \"Display Name\""
---

# Game project bootstrap

## When to use
Starting a new game or prototype. Not for changes to an existing game (use libgdx-game-development)
and not for editing the template itself unless the change should reach every future game.

## Inputs
- `Name`: folder name, lowercase-kebab (`color-dash`).
- `PackageId`: reverse-domain, permanent on Play (`com.ben.laama.colordash`). Never `com.example.*`.
- `DisplayName`: launcher/store name.
- One-paragraph game concept (genre, core action, session length). Ask only if missing.

## Workflow
1. Check prerequisites (only if not checked this session):
   `.\tools\environment-check\check-environment.ps1`
2. Generate (copies template, renames packages, writes local.properties, runs tests):
   `.\tools\scripts\new-game.ps1 -Name <name> -PackageId <id> -DisplayName "<Display>"`
3. Confirm desktop run: `.\tools\android-build\build-android.ps1 -Project <name> -Task Desktop`
4. Adapt to the concept, in this order, keeping tests green after each step:
   1. `GameConfig` numbers and world size (portrait 720x1280 default).
   2. Replace `logic/RoundState` + `Target` with the game's rule classes (pure Java) + tests.
   3. Replace `screens/PlayScreen` drawing; keep the ad hooks (`continue` button, `playAgain`).
   4. Rename `TemplateGame` if desired (update both launchers).
   5. App name in `android/src/main/res/values/strings.xml`, icon in `res/`.
5. Write the game's README: concept, architecture, commands, ad placements.
6. If the game needs 3D, read [libgdx-game-development/3d.md](../libgdx-game-development/3d.md)
   before writing code.

## Standards
- One game per folder, self-contained Gradle build (own wrapper, same versions as the template).
- Reuse proven code from `games/sortpuzzle/` by copying and adapting, not by cross-project dependencies.

## Validation
```powershell
.\tools\android-build\build-android.ps1 -Project <name> -Task Test
.\tools\android-build\build-android.ps1 -Project <name> -Task Debug
.\tools\validation\validate-workspace.ps1
```

## Common failures
- `games\<name> already exists` - choose another name; the script never overwrites.
- `SDK location not found` - local.properties missing: rerun the env check, or set `ANDROID_HOME`.
- Gradle wrapper silently bumped to 9.x by an IDE - restore `gradle-8.14.3` (docs/troubleshooting.md).

## Deliverables
New project in `games/<name>/`, green tests, debug APK path, README with architecture section.

## References
- libGDX wiki: https://libgdx.com/wiki/
- Play app ID rules: https://developer.android.com/build/configure-app-module#set-application-id
