# Ads integration checklist (AdMob)

## Code (verified by Claude)
- [ ] Game uses only `AdProvider`; SDK imports confined to `android/.../AdMobAdProvider.java`
- [ ] Debug build uses Google demo unit IDs (`ca-app-pub-3940256099942544/...`)
- [ ] Release IDs come from `admob.properties` (git-ignored); `:android:printAdConfig` shows it present
- [ ] Manifest has `com.google.android.gms.ads.APPLICATION_ID` (UMP needs it)
- [ ] UMP consent requested every launch; ads requested only when `canRequestAds()`
- [ ] Privacy options button shown when required
- [ ] `MobileAds.initialize` off the main thread
- [ ] Rewards settle exactly once (`RewardGate` tests green); early close = no reward
- [ ] Interstitials only at natural breaks; caps in `GameConfig`, enforced by `AdPacing` tests
- [ ] No-fill/offline handled: buttons hidden or graceful message; game never blocks on an ad
- [ ] `-Pads.enabled=false` build makes no ad requests (dev/QA builds)
- [ ] Test device IDs only in debug

## Account side (user)
- [ ] AdMob account approved, payment profile complete
- [ ] App added in AdMob; ad units created (rewarded, interstitial)
- [ ] After Play publication: AdMob app linked to the Play listing
- [ ] `app-ads.txt` published at the root of the developer website listed on Play
- [ ] Privacy & messaging: GDPR (and US state) messages created and published in AdMob
- [ ] Never click own live ads; testers use test devices

Integration does not mean revenue: serving starts after app review/approval, and fill varies.
