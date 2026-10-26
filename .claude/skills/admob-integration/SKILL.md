---
name: admob-integration
description: Wire Google AdMob into a libGDX game through the AdProvider interface using the GMA Next-Gen SDK and UMP consent - rewarded, interstitial, optional banner, demo/test IDs, production IDs per environment, retries, exactly-once rewards, disabling ads in dev. Use for adding, porting, debugging or configuring ads.
argument-hint: "<project>"
---

# AdMob integration (libGDX + GMA Next-Gen SDK)

## When to use
Adding ads to a game, porting the template's ad stack into an existing game (e.g. Sortpuzzle),
debugging no-fill/crashes, or switching IDs for release. For *where* ads go, see rewarded-ads-design.

## Inputs
- Project; whether it already has an `AdProvider` (Sortpuzzle and template do).
- For release only: AdMob app ID + unit IDs from the user's AdMob account (user supplies them;
  never invent or reuse someone else's).

## Reference implementation (copy from, don't reinvent)
`templates/libgdx-2d/`:
- `core/.../ads/AdProvider.java` - interface; callbacks on render thread, exactly once.
- `core/.../ads/RewardGate.java` - earned + dismissed -> one outcome. `AdPacing.java` - frequency caps.
- `android/.../AdMobAdProvider.java` - UMP -> init on worker thread -> preload -> backoff retries.
- `android/build.gradle.kts` - demo IDs in debug, `admob.properties` in release, `-Pads.enabled=false`.
Versions verified 2026-10-21: `ads-mobile-sdk:1.5.0`, `user-messaging-platform:4.0.0`, minSdk 24,
compileSdk >= 35. Legacy `play-services-ads` is in maintenance mode - don't add it alongside Next-Gen.

## Workflow: port into an existing game
1. Add `gmaNextGen`/`ump` versions + libraries to `gradle/libs.versions.toml`; add both to
   `android` dependencies. Add `buildFeatures { buildConfig = true }`.
2. Copy the ad-config block (loadProps, demo IDs, buildConfigFields, manifestPlaceholders) from the
   template's `android/build.gradle.kts`.
3. Manifest: `INTERNET` permission + `com.google.android.gms.ads.APPLICATION_ID` meta-data with
   `${admobAppId}` (UMP requires it; a missing entry crashes at startup).
4. Extend the game's `AdProvider` with `isPrivacyOptionsRequired()/showPrivacyOptions()` and a
   "Privacy options" button in settings/menu. Sortpuzzle's `RewardListener.onRewarded(int coins)`
   can stay; pass 0.
5. Copy `RewardGate` (+ test) into core, `AdMobAdProvider` into android; fix package names.
6. Launcher: `BuildConfig.ADS_ENABLED ? new AdMobAdProvider(this, config) : new NoOpAdProvider()`.
7. Keep `NoOpAdProvider` for desktop/tests.

## Rules
- Debug = demo unit IDs (`ca-app-pub-3940256099942544/...`). Your own phone: add its hashed ID to
  `admob.testDeviceIds` from the logcat hint. **Never click live ads, never ask others to.**
- Request ads only when `ConsentInformation.canRequestAds()`; call `requestConsentInfoUpdate` every launch.
- `MobileAds.initialize` on a background thread. SDK callbacks may come on background threads:
  touch game state only via `Gdx.app.postRunnable`; show ads via `runOnUiThread`.
- Ad objects are single-use: null the field before `show`, load the next on dismiss/fail.
- No-fill is normal: exponential backoff (1 s -> 64 s), never a tight retry loop.
- Banners (optional): adaptive anchored banner in a reserved strip the game never draws under;
  requires a layout wrapping the libGDX view - only add if the design reserves the space.

## Validation
```powershell
.\gradlew.bat :core:test --tests "*RewardGateTest" --tests "*AdPacingTest"
.\gradlew.bat :android:installDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" logcat -s AdMobAdProvider:* Ads:* UserMessagingPlatform:*
.\gradlew.bat :android:printAdConfig          # before release: confirms admob.properties is picked up
```
On device: ads show the "Test Ad" label; rewarded grants once; closing early grants nothing;
`-Pads.enabled=false` build makes no ad requests.

## Common failures
- Crash "Missing application ID" -> manifest meta-data absent/placeholder not set.
- Always `onAdFailedToLoad` with no fill in debug -> using production IDs on an unregistered device, or no network.
- Reward granted twice -> listener called from both earn and dismiss without `RewardGate`.
- ANR at launch -> `MobileAds.initialize` on the main thread.
- Ads never load in EEA test -> consent not given in the form; `canRequestAds()` false is correct behaviour.
- Duplicate class errors -> legacy `play-services-ads` pulled in by a mediation adapter; exclude it.

## Deliverables
Building debug + release, tests green, README "Ads" section (placements, caps, config), and the
user-side checklist: AdMob account approved, app added + linked to Play listing, app-ads.txt on the
developer website, payment profile, policy review. Revenue is not guaranteed by integration.

## Not for
Placement/economy design (rewarded-ads-design); consent text and Data safety (privacy-and-consent).

## References
- https://developers.google.com/admob/android/next-gen/quick-start
- https://developers.google.com/admob/android/next-gen/rewarded , .../next-gen/interstitial
- https://developers.google.com/admob/android/test-ads
- https://developers.google.com/admob/android/privacy (UMP)
- https://support.google.com/admob/answer/6128543 (AdMob program policies)
