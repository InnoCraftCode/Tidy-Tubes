# Device test matrix

Copy into the game folder per release (`TEST-<version>.md`). Scenarios are defined in
`.claude/skills/mobile-game-testing/SKILL.md`.

| Device | Android / API | RAM | Screen | Build |
|---|---|---|---|---|
| Low-end (e.g. 2019, 2-3 GB) | | | | release |
| Recent mid/high | | | | release |
| Emulator API 36 (x86_64) | 16 / 36 | | 20:9 | debug |

| Scenario | Low-end | Recent | Emulator | Notes |
|---|---|---|---|---|
| Cold start < 3 s, no black frame | | | | |
| Full level/round playable, 60 FPS | | | | |
| Home + return keeps state | | | | |
| Kill from recents keeps progress | | | | |
| Back button behaviour | | | | |
| Rewarded: complete -> reward once | | | | |
| Rewarded: close early -> nothing | | | | |
| Interstitial only at breaks, caps respected | | | | |
| Offline: game runs, ad buttons hidden/graceful | | | | |
| EEA consent form + privacy options | | | | |
| Update over previous version keeps save (R8 build) | | | | |
| Small (5") and tall (20:9) layouts legible | | | | |

Result legend: OK / FAIL (+ issue link or note) / N/A.
