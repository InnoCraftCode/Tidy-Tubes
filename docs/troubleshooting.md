# Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Failed to apply plugin 'com.android.internal.application'` ... `InternalProblems` | Gradle wrapper bumped to 9.x; AGP 8.11 needs Gradle 8.x | In `gradle/wrapper/gradle-wrapper.properties` set `distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip` |
| Wrapper changes to 9.x by itself after opening the folder in VS Code | Red Hat Java extension imports Gradle with its bundled JDK 25 and offers/does an upgrade | Keep `.vscode/settings.json` -> `java.import.gradle.java.home` = JDK 21. Decline "upgrade Gradle" prompts. |
| `SDK location not found` | No `local.properties` / `ANDROID_HOME` | Use `build-android.ps1` (sets it per run) or open once in Android Studio |
| `Failed to find target with hash string 'android-36'` | Platform 36 not installed | Install via SDK Manager (installation.md) |
| `error: <identifier> expected` in `Main.java` | Was a real bug in Sortpuzzle (`new  ()`), fixed 2026-10-21 | - |
| Script "cannot be loaded because running scripts is disabled" | Execution policy | `powershell -ExecutionPolicy Bypass -File <script>` |
| `gradlew.bat is not recognized` | Run from the wrong folder | `cd` into the game folder or use `build-android.ps1 -Project` |
| Release build loses progress | R8 stripped a `Json` class | `-keep class <package>.save.** { *; }` in proguard-rules.pro |
| App crashes at launch: "Missing application ID" | AdMob/UMP manifest meta-data missing | Template's `${admobAppId}` placeholder + build.gradle.kts block |
| Ads never load in debug | No network, or production IDs on a non-test device | Debug uses demo IDs; check logcat `Ads`/`AdMobAdProvider` |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Installed app signed with another key | `adb uninstall <applicationId>` (deletes local progress) |
| `... is located outside the root directory ...` during dexing, after moving a project folder | Incremental build state still holds the old path | `.\gradlew.bat --stop` then `.\gradlew.bat clean :android:assembleDebug --no-build-cache` |
| Agent/skill not listed in Claude Code | Created during an open session | Restart the session or `/reload-skills` |
| Gradle build very slow first time | Downloads Gradle, AGP, SDK artifacts | Normal once; later builds use the cache |
