# Analytics (not integrated yet - by decision)

No analytics SDK ships today. Adding one (Firebase Analytics, etc.) changes the Data safety form,
the privacy policy and consent requirements, so it needs a privacy review first
(`.claude/skills/privacy-and-consent`).

When a game needs it, follow this shape so every game reports the same way:

## Interface
Add `core/.../analytics/Analytics.java` (`void event(String name, Map<String, Object> params)`) with a
`NoOpAnalytics`; the Android launcher injects the real implementation, exactly like `AdProvider`.

## Event taxonomy (snake_case, <= 40 chars, no personal data)
| Event | Params |
|---|---|
| `level_start` | `level`, `attempt` |
| `level_complete` | `level`, `moves`, `seconds`, `stars` |
| `level_fail` | `level`, `reason` |
| `ad_offer_shown` | `placement` |
| `ad_rewarded_complete` | `placement` |
| `ad_interstitial_shown` | `placement` |
| `tutorial_step` | `step` |

Never log identifiers, free text entered by users, or precise location.
