# Data safety worksheet (AdMob game, no accounts, no analytics)

Starting point for Play Console > App content > Data safety. **Verify against Google's current
GMA SDK data disclosure** (https://developers.google.com/admob/android/privacy/play-data-disclosure)
before submitting; answers depend on SDK versions and your settings.

| Question | Typical answer for the template | Why |
|---|---|---|
| Does your app collect or share user data? | Yes | GMA SDK collects device identifiers etc. |
| Is all data encrypted in transit? | Yes | SDK uses HTTPS |
| Can users request deletion? | Explain: no account data held by developer; on-device data deleted on uninstall | |
| Location - approximate | Collected, shared; purpose: Advertising, Analytics, Fraud prevention | IP-derived |
| App activity - app interactions | Collected, shared; Advertising, Analytics | ad interactions |
| App info and performance - crash logs, diagnostics | Collected; Analytics, Fraud prevention | SDK diagnostics |
| Device or other IDs | Collected, shared; Advertising, Analytics, Fraud prevention | Advertising ID / app set ID |
| Personal info (name, email...) | Not collected | |
| Is collection optional? | Ads processing is required for the free app; personalisation subject to consent | |

Also in App content: Ads = "Yes, my app contains ads"; Advertising ID = used for advertising/analytics.

If you add Firebase/analytics/crash reporting, redo this sheet.
