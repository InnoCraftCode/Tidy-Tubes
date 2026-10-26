# Game design one-pager: <Game name>

Fill this before writing code; keep it to one screen. The game-design-agent reviews it.

## Pitch
One sentence: "<verb> <objects> to <goal> before <pressure>."

## Core loop (seconds)
Action -> feedback -> reward -> next action. Session target: __ min. First win within __ s.

## Controls
Single tap / drag / swipe. One-handed portrait unless justified.

## Progression
| Dial (function of level n) | L1 | L10 | L50 | L100 | Ceiling |
|---|---|---|---|---|---|
| e.g. colours / speed / targets | | | | | |
Deterministic seed per level? yes/no. Bands asserted by a unit test.

## Fail state and recovery
What ends a level/round; what the player can do (retry, rewarded continue, undo).

## Economy
Currency earned per win, sinks (boosters, hints), daily cap. All numbers in `GameConfig`.

## Monetization (link the placement spec)
`shared/monetization/ad-placement-spec.md` copy for this game.

## Onboarding
Level 1 teaches by doing. Text <= 1 line per screen.

## Juice
Sounds, particles, haptics (respect mute; haptics optional).

## Acceptance criteria (testable)
- [ ] ...
