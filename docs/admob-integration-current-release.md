# AdMob in the current release (Tidy Tubes 1.0.0)

Game: `games/sortpuzzle`. Date: 2026-10-26.

> History of the decision. The mission brief asked for AdMob to be **fully inactive** in this
> release; that was implemented first (`AdsConfig.ADS_ENABLED = false`, `DisabledAdProvider`,
> coin-only rewards, no SDK on the classpath). The user then changed the requirement mid-task
> ("je change mon avis, gadi ndir fiha ads AdMob"), so this release **ships with AdMob**. The
> ad-free architecture was kept as the fallback for builds without ad IDs; this document covers
> both states.

## 1. Integration map

| Concern | Where | Notes |
|---|---|---|
| Interface the game talks to | `core/.../ads/AdProvider.java` | `initialize`, `isRewardedReady`, `showRewarded`, `isInterstitialReady`, `showInterstitial`, `isPrivacyOptionsRequired`, `showPrivacyOptions`, `dispose`. Callbacks on the render thread, exactly once. |
| Release switch | `core/.../ads/AdsConfig.java` | `ADS_ENABLED = true`. `false` would give every platform `DisabledAdProvider`. |
| Provider selection | `core/.../ads/AdProviders.java` | Desktop: `NoOpAdProvider` (dev stub, grants without video). Android: the real network, or `DisabledAdProvider` when the build has no ads. Never the stub on a device. |
| SDK code (the only class importing the SDK) | `android/.../AdMobAdProvider.java` | GMA Next-Gen `ads-mobile-sdk:1.5.0` + UMP `user-messaging-platform:4.0.0`. Consent info requested every launch; `MobileAds.initialize` on a worker thread only when `canRequestAds()`; one rewarded + one interstitial preloaded; exponential back-off 1 s to 64 s on no-fill; ad objects single-use. |
| Exactly-once reward | `core/.../ads/RewardGate.java` (+ `RewardGateTest`, 4 tests) | Earned + dismissed settle once; early close = no reward. |
| Frequency caps | `core/.../ads/AdPacing.java` (+ `AdPacingTest`, 5 tests), numbers in `GameConfig` | Interstitial at most every **3** completed levels, never the first break of a session, never sooner than **90 s** after any full-screen ad (rewarded included). |
| Reward policy | `core/.../logic/RewardOffer.java` (+ 6 tests) | Video when the provider has one ready; otherwise coins (hint unlock 40, +5 moves 60); otherwise nothing, with an honest message. |
| Placements | `screens/GameScreen.java` | Rewarded: hint unlock after the free hint (`onHint`), +5 moves when out of moves (`watchAdForMoves`). Interstitial: only in `afterBreak`, i.e. when the player presses NEXT or REPLAY on the level-cleared card. No banner (the design reserves no strip). |
| Privacy options | `screens/SettingsScreen.java` | "Privacy options" card shown only when UMP reports it required. About card states "Free with ads", AdMob and the advertising ID. |
| Build config | `android/build.gradle.kts`, `gradle.properties`, `admob.properties.example` | Debug: Google demo IDs always. Release: IDs from git-ignored `admob.properties`; **without that file the release build has `ADS_ENABLED=false` and ships ad-free**. `-Pads.enabled=false` disables ads in any build. `gradlew :android:printAdConfig` prints the resolved state. |
| Manifest | `android/src/main/AndroidManifest.xml` | `INTERNET` permission; `com.google.android.gms.ads.APPLICATION_ID` meta-data from the Gradle placeholder. The SDK merges `ACCESS_NETWORK_STATE` and `com.google.android.gms.permission.AD_ID`. |
| R8 | `android/proguard-rules.pro` | SDKs ship consumer rules; nothing added. |

## 2. Verification performed (2026-10-26)

| Check | Command / evidence | Result |
|---|---|---|
| Unit tests (ads) | `gradlew :core:test` | `AdProvidersTest` 5, `RewardGateTest` 4, `AdPacingTest` 5, `RewardOfferTest` 6: all PASSED |
| Debug build | `gradlew :android:assembleDebug` | BUILD SUCCESSFUL, `android-debug.apk` 15.4 MB |
| On device (Pixel_10 AVD, Android 17 / API 37, 16 KB pages) | `adb install -r`, `am start`, `logcat -s AdMobAdProvider UserMessagingPlatform` | UMP: consent info updated (`IABTCF_gdprApplies 0` outside the EEA, no form); `GMA SDK initialised`; `Interstitial loaded`; `Rewarded loaded` (demo units). No crash, no ANR. |
| Game starts with ads unavailable | Desktop capture run (`NoOpAdProvider`) and `DisabledAdProvider` unit test | Hint and extra-move flows fall back to coins; buttons never dead-end. |
| No ad code outside the provider | `grep -r "libraries.ads\|android.ump" core/` | 0 matches in `core/` |

Not verified here: production IDs (none exist yet), real fill rates, the EEA consent form (needs a
test device hashed ID in `admob.properties` or an EEA location), rewarded close-early on a real
network (covered by `RewardGate` tests + Google's demo ads behave the same way).

## 3. Switching ads off for a release

1. Build without `admob.properties` (release) or with `-Pads.enabled=false` (any build): the
   launcher receives `null` and `AdProviders.forAndroid` returns `DisabledAdProvider`. No SDK
   initialisation, no consent request, no network call. Store declaration: still "contains ads"
   = **no** only if the SDK is removed as well (see 2), because Play's declaration is about the
   app's capability, and the SDK remains on the classpath.
2. For a truly ad-free build: set `AdsConfig.ADS_ENABLED = false`, remove `libs.gma.nextgen`
   and `libs.ump` from `android/build.gradle.kts`, delete `AdMobAdProvider.java`, drop the
   `INTERNET` permission and the `APPLICATION_ID` meta-data, update `AdProvidersTest`, the
   Settings about text, `listing.json` and the Data safety answers.

## 4. What the user must do before ads earn anything (outside the repo)

- AdMob account approved, payment profile complete; app created in AdMob; one rewarded and one
  interstitial unit created; IDs copied into `games/sortpuzzle/admob.properties`.
- Privacy & messaging in AdMob: GDPR and US state messages created and **published** (UMP shows
  nothing otherwise).
- Developer website with `app-ads.txt` at its root, listed on the Play listing.
- Play Console: "Contains ads" = Yes; advertising ID declaration = Yes; Data safety answers from
  `shared/privacy/data-safety-worksheet.md`; privacy policy URL (mandatory) naming AdMob.
- After publication: link the AdMob app to the Play listing.
- Never click your own live ads; register your phone as a test device
  (`admob.testDeviceIds`, hashed ID printed in logcat).

See also `config/release-checklists/ads-integration.md` and `docs/google-play-compliance-report.md`.
