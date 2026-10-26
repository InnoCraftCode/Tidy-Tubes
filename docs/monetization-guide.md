# Monetization guide (AdMob)

What Claude handles: code and configuration (`/configure-ads`, skill `admob-integration`).
What only you can do: everything in your AdMob and Play accounts, below.

## Path to real ad revenue
1. **AdMob account** - sign up at https://admob.google.com with your Google account; complete
   payment/tax profile. Approval can take days.
2. **Develop with demo ads** - debug builds already use Google demo IDs. Register your phone as a
   test device (`admob.testDeviceIds` in `admob.properties`; the ID is printed in logcat).
   **Never click real ads on your own device and never ask friends to** - invalid traffic gets the
   account suspended.
3. **Create the app in AdMob** (platform Android; "not published yet" is fine) -> note the App ID
   (`ca-app-pub-...~...`). Create a Rewarded and an Interstitial ad unit -> unit IDs (`.../...`).
4. **Consent messages** - AdMob > Privacy & messaging: create and publish the GDPR message (and US
   states message). The game's UMP code shows them automatically where required.
5. **Release config** - in the game folder: `Copy-Item admob.properties.example admob.properties`,
   fill the IDs, then `.\gradlew.bat :android:printAdConfig`.
6. **Publish on Play** (see [google-play-guide.md](google-play-guide.md)), then in AdMob **link the
   app to its Play listing**.
7. **app-ads.txt** - publish the line AdMob gives you at `https://<your-site>/app-ads.txt`, using the
   website set in your Play developer profile.
8. **Wait for review** - AdMob reviews the app before full serving; fill and eCPM vary by country
   and season. Integration alone does not guarantee revenue.

## Design rules (summary of skill `rewarded-ads-design`)
Rewarded = opt-in at stuck points with a clear reward. Interstitial = only at natural breaks,
paced (template: every 3rd break, >= 2 min apart, never in the first 2). No ads during input, at
launch, or on exit. Banners only in reserved space.

## Optimisation, later
Once there is traffic: compare rewarded opt-in per placement, test frequency caps one at a time,
consider mediation only then (ADR-002 notes the legacy-SDK exclusion it requires).

Policies: https://support.google.com/admob/answer/6128543
