# ADR-002: AdMob through the GMA Next-Gen SDK, behind AdProvider

- Status: accepted (2026-10-21)

## Context
AdMob is the default network. Google now ships two Android SDKs: the legacy
`com.google.android.gms:play-services-ads` (25.5.0; its docs state it is in maintenance mode) and the
**GMA Next-Gen SDK** `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` (1.5.0). Consent
uses UMP `com.google.android.ump:user-messaging-platform` (4.0.0). Both require minSdk 24, compileSdk >= 35.

## Decision
- New games use **Next-Gen 1.5.0 + UMP 4.0.0** (verified by compiling the template against them and
  inspecting class signatures with `javap`, 2026-10-21).
- Game code depends only on `core/.../ads/AdProvider`. Platform code (`AdMobAdProvider`) is the only
  place that imports the SDK. Desktop/tests use `NoOpAdProvider`.
- Debug builds always use Google demo unit IDs; release IDs come from git-ignored `admob.properties`;
  release without that file ships with ads disabled.
- Formats: rewarded (opt-in) and interstitial (paced). Banners only when a layout reserves space.
- Mediation is out of scope until a game has traffic; adapters pull the legacy SDK and need exclusions
  (see Google's migration guide).

## Consequences
- Sortpuzzle (Tidy Tubes) ported the stack on 2026-10-26 (`android/.../AdMobAdProvider.java`,
  `RewardGate`, `AdPacing`, `RewardOffer` coin fallback); rewarded flow verified on the Pixel_10
  AVD with demo IDs. Porting into another game remains the copy-based step in
  `.claude/skills/admob-integration/SKILL.md`.
- When Google changes SDK versions: bump `gmaNextGen`/`ump` in `libs.versions.toml`, rebuild the
  template, rerun device checks, update this ADR.

## Revisions
- 2026-10-26: `gmaNextGen` 1.5.0 -> **1.6.0** (released 2026-10-22: rewarded `onUserEarnedReward`
  ordering fix, R8 full-mode consumer rules, hsdp 2.2.0; no breaking change per the release notes
  read the same day). UMP stays 4.0.0 (latest on maven.google.com). Verified by compiling both
  projects and running sortpuzzle's debug build on the AVD.
