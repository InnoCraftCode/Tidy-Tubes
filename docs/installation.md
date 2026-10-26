# Installation

Check first; install only what the check reports missing:
```powershell
cd "E:\mobile andoird"
powershell -ExecutionPolicy Bypass -File .\tools\environment-check\check-environment.ps1
```

## Already present on this machine (2026-10-21)
JDK 21 Temurin (JAVA_HOME set), Android Studio, Android SDK (platforms 35 + 37.0, build-tools 35/36,
platform-tools, emulator), Claude Code VS Code extension 2.1.306, Gradle 8.14.3 (via wrapper).

## Recommended additions (need your approval; none were installed automatically)
| Item | Why | How |
|---|---|---|
| Android SDK Platform 36 | Play requires targetSdk 36 | Android Studio > Settings > Languages & Frameworks > Android SDK > SDK Platforms > "Android 16.0 (API 36)" > Apply (accepts its licence) |
| Android SDK Command-line Tools | `sdkmanager`/`avdmanager` for scripted installs | Same screen > SDK Tools > "Android SDK Command-line Tools (latest)" |
| An emulator (AVD) | Testing without a phone | Android Studio > Device Manager > Create device > Pixel, API 36 x86_64 image |
| Git | History and rollback of your games | `winget install --id Git.Git -e` then reopen the terminal |
| ANDROID_HOME (optional) | Gradle from any terminal without local.properties | `setx ANDROID_HOME "%LOCALAPPDATA%\Android\Sdk"` (new terminals only) |

After installing platform 36, raise `compileSdk`/`targetSdk` to 36 per game (template:
`gradle.properties`; Sortpuzzle: `android/build.gradle.kts`), rebuild, and retest on an API 36 device.

## Phone setup (USB debugging)
Settings > About phone > tap Build number 7x > Developer options > USB debugging on. Connect,
accept the RSA prompt, then `& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices`.

## Script execution policy
If scripts are blocked: run them via `powershell -ExecutionPolicy Bypass -File <script>`, or once
for your user: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.
