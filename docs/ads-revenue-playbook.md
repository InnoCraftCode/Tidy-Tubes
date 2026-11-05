# Tidy Tubes: ad revenue playbook

Goal: earn more from AdMob without hurting retention, the number that decides long-term revenue.
Part A is already in the game (1.1.2). Part B is console work only you can do (AdMob / Play /
GitHub accounts); it is ordered by expected impact.

## A. In the game (done in 1.1.2)

| Placement | Type | Rules |
|---|---|---|
| Win card: "WATCH • ×2 COINS" | Rewarded | Opt-in, only when a video is ready, coins credited once and only on the SDK's reward callback |
| Shop TUBES+: "Free coins" | Rewarded | +30 coins, max 5 per local day (`GameConfig.AD_COINS_*`) |
| Home: daily bonus, Free tube, out-of-moves, assists | Rewarded | Unchanged |
| After a level | Interstitial | Not before 5 cleared levels (onboarding), then 1 break in 3, at least 2 min after any full-screen ad, a rewarded view resets the timer |

Measured in Firebase Analytics: `rewarded_double_coins`, `rewarded_free_coins` (plus existing events).
Rewarded videos earn the highest eCPM and players choose them, so they grow revenue without the
churn interstitials cause. Do not add banners in the board or app-open ads: low eCPM here, high
risk of accidental clicks (policy) and of players leaving.

## B. Console actions (yours, in this order)

1. **Publish the consent message (UMP).** AdMob > Privacy & messaging > GDPR (and US states):
   create and publish the message for Tidy Tubes. Without it, users in the EEA/UK get no
   personalised ads at all (often near-zero fill). This is the biggest single gain if not done.
2. **app-ads.txt.** The file is ready: `play-store/sortpuzzle/app-ads.txt`. AdMob reads it at the
   **root of the domain** of the developer website set in Play Console, i.e.
   `https://innocraftcode.github.io/app-ads.txt` (the `/Tidy-Tubes/` sub-folder does not count).
   Create a GitHub repository named exactly `InnoCraftCode.github.io`, put the file at its root,
   enable Pages, then AdMob > Apps > app-ads.txt shows "Verified" after up to 24 h. Unverified apps
   lose demand.
3. **Mediation with bidding.** AdMob > Mediation > create a group for Rewarded and one for
   Interstitial, add bidding partners (AppLovin, Meta Audience Network, Unity Ads, Liftoff,
   Mintegral, Pangle). Each needs an account with that network and an adapter dependency in the app
   (ask Claude to add the adapters once the accounts exist). Typical uplift: +20 to +50 % eCPM.
4. **One ad unit per placement** (optional, better reports): rewarded "double coins", "free coins",
   "assists"; interstitial "level end". Put the ids in `admob.properties`; the code can route them.
5. **Never click your own ads.** Use test device ids (`admob.testDeviceIds`) on your phone.

## C. What to watch (AdMob + Firebase, weekly)

- ARPDAU (revenue / daily active users) and eCPM per format.
- Rewarded: views per daily user (target 1.5 to 3), share of players who use "×2".
- Interstitial: impressions per daily user (keep under about 3) and day-1 / day-7 retention. If
  retention drops after a change, back off the interstitial first.
- Match rate / fill rate: low fill means consent (B1), app-ads.txt (B2) or mediation (B3).

Tuning knobs, all in `GameConfig`: `INTERSTITIAL_MIN_CLEARED`, `INTERSTITIAL_EVERY_N_LEVELS`,
`INTERSTITIAL_MIN_INTERVAL_MS`, `AD_COINS_REWARD`, `AD_COINS_PER_DAY`.
