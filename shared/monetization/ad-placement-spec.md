# Ad placement spec: <Game name>

Copy per game. Numbers go to `GameConfig`; caps are enforced in code (`AdPacing`) and tested.

| # | Trigger (natural break / stuck point) | Format | Offer text | Reward | Cap | On decline / no fill |
|---|---|---|---|---|---|---|
| 1 | Out of moves | Rewarded | "Watch a video for +5 moves" | +5 moves, resume in place | 1 per level | Level lost screen, retry free |
| 2 | Hint after free one used | Rewarded | "Watch to unlock hints for this level" | Hints for the rest of the level | 1 per level | Hint button stays locked |
| 3 | Round/level complete -> Next | Interstitial | - | - | every 3rd break, >= 120 s apart, never first 2 breaks | Continue immediately |
| 4 | (optional) Menu screens | Adaptive banner | - | - | menus only, reserved strip | Strip collapses |

## Limits
- Max 3 interstitials per 10 minutes of play.
- No ad within 30 s of launch, during input, or on app exit.
- A rewarded view resets the interstitial timer.

## Metrics to watch (once analytics is approved)
Rewarded opt-in rate per placement, interstitials per session, D1/D7 retention vs. ad frequency.

Policy: https://support.google.com/admob/answer/6128543
