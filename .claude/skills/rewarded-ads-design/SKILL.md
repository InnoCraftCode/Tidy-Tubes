---
name: rewarded-ads-design
description: Design where and how often ads appear in a casual/puzzle game - rewarded offers, interstitial pacing, banner placement - balancing revenue, retention and AdMob policy. Use when deciding ad placements, reward values or frequency caps; produces a spec with numbers.
---

# Rewarded and interstitial ad design

## When to use
Planning or revising monetization for a game. Implementation is admob-integration.

## Inputs
Core loop and session length, level/round structure, economy (coins, boosters, hints), target audience
(if children may play, stop and apply Families policy via privacy-and-consent).

## Workflow
1. List natural breaks (level complete, round over, returning to menu) and stuck points (out of moves,
   out of time, no hints left).
2. Rewarded offers go on stuck points and optional bonuses. For each, write: trigger, offer text,
   reward, cap, what happens on decline/failure.
3. Interstitials go on natural breaks only, with caps. Start conservative:
   never in the first session's first 2 breaks, at most every 3rd break, >= 120 s apart, and a
   rewarded view resets the timer (template `AdPacing` defaults).
4. Banner: only if the layout reserves a strip outside the play area; otherwise skip.
5. Put all numbers in `GameConfig`; write acceptance tests (pacing, once-per-round offers).
6. Define what you will measure later (rewarded opt-in rate, retention D1/D7, ARPDAU) - without
   adding analytics SDKs until privacy review approves them.

## Proven patterns (from Sortpuzzle and the template)
- "Out of moves -> watch to get +5 moves and continue in place" (high opt-in, saves the session).
- Hint unlocked for the rest of the level, not one hint per ad - a stuck player shouldn't face another ad 4 moves later.
- "+10 s" continue once per round, offered on the game-over panel.
- Offer text states the exact reward; declining has no penalty.

## Policy lines never to cross
No ad on app launch/exit interstitials without content in between, no ads during active input,
no accidental-click layouts (buttons next to ad edges), no rewarding clicks or installs, no
"fake" UI that looks like game content, no forcing a rewarded ad to progress normally.
Check against https://support.google.com/admob/answer/6128543 and the rewarded ad policies
https://support.google.com/admob/answer/7313578.

## Validation
Spec reviewed against the policy list; `AdPacing` tests encode the caps; playtest a full session
and count ads per 10 minutes (target: <= 3 interstitials).

## Deliverables
Placement table (trigger, format, reward, cap, decline path), `GameConfig` values, test cases.

## Not for
SDK wiring or consent implementation.
