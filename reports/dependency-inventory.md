# Dependency inventory - 2026-10-21

Produced by inspection and `tools\environment-check\check-environment.ps1`. Re-run the script to refresh.

## Machine tooling
| Tool | Status | Version / location | Needed for |
|---|---|---|---|
| JDK | Installed | Temurin 21.0.12.1, `JAVA_HOME` set | Gradle, javac |
| Android Studio | Installed | `C:\Program Files\Android\Android Studio` | SDK Manager, profiler, AVDs |
| Android SDK | Installed | `%LOCALAPPDATA%\Android\Sdk` (`ANDROID_HOME` not set; tools resolve it) | Android builds |
| SDK platforms | Partial | android-35, android-37.0. **android-36 missing** | targetSdk 36 for Play |
| Build-tools | Installed | 35.0.0, 36.0.0 | |
| Platform-tools | Installed | adb 1.0.41 (37.0.1) | install/logcat |
| Emulator | Installed, no AVD | `emulator.exe` | device-less testing |
| SDK cmdline-tools | Missing | - | scripted sdkmanager/avdmanager (optional) |
| Gradle | Wrapper only | 8.14.3 per project (9.6.0/9.7.1 dists also cached, unused) | builds |
| Git | **Missing** | - | history/rollback |
| Claude Code | VS Code extension | 2.1.306; CLI binary not on PATH (`claude doctor` warns) | agents/skills |
| Python | Store stub only | `WindowsApps\python.exe` | not needed |
| Node, .NET, Godot, Unity | Not installed | - | not needed (ADR-001) |
| winget | Available | | optional installs |

## Project libraries (pinned in `gradle/libs.versions.toml`)
| Library | Version | Projects | Notes |
|---|---|---|---|
| libGDX (gdx, backends, platform natives) | 1.13.1 | Sortpuzzle, template | Apache 2.0 |
| Android Gradle Plugin | 8.11.1 | both | Needs Gradle 8.x (not 9) |
| Gradle wrapper | 8.14.3 | both | |
| JUnit Jupiter (BOM) | 5.11.4 | both | |
| GMA Next-Gen SDK `ads-mobile-sdk` | 1.5.0 | template | minSdk 24, compileSdk >= 35; verified by compile + javap |
| UMP `user-messaging-platform` | 4.0.0 | template | needs APPLICATION_ID in manifest |
| Kotlin stdlib (transitive via GMA) | 2.0.0 | template | no Kotlin code in project |

## SDK levels
| Project | compileSdk | targetSdk | minSdk | Play-compliant today? |
|---|---|---|---|---|
| Sortpuzzle | 35 | 35 | 24 | No - needs 36 |
| templates/libgdx-2d | 35 (gradle.properties) | 35 | 24 | No - set `android.targetSdk=36` once platform 36 is installed |
