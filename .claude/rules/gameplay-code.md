---
paths:
  - "games/*/core/src/main/java/**/logic/**"
  - "games/*/core/src/main/java/**/model/**"
  - "templates/libgdx-2d/core/src/main/java/**/logic/**"
  - "templates/libgdx-2d/core/src/main/java/**/model/**"
---

# Gameplay code rules

Adapted from Claude Code Game Studios (MIT, see `.claude/THIRD_PARTY.md`) for this workspace.

- Zero libGDX imports in `logic/` and `model/`: plain Java, testable with JUnit, no
  `Gdx.*` and no `Gdx.app.log`.
- Every tuning value lives in `GameConfig` (or a data table in `logic/`), never inline.
  Example: `GameConfig.TUBE_CAPACITY`, not `4`.
- Time-based rules take elapsed time or a clock as a parameter (frame-rate independent,
  testable). Never read the wall clock inside a rule: pass `LocalDate` / epoch day in.
- Randomness takes a seed or a `Random` argument, so a level or a daily puzzle is
  reproducible in a test.
- Prefer immutable models (`BoardState.apply(Move)` returns a new board). A method that
  mutates says so in its name and Javadoc.
- No UI references and no static mutable singletons for game state. `Main` owns the save,
  the services and the ad provider and passes them down.
- Every rule ships with a JUnit test in the same change (see `test-standards.md`).
- Rewards: granted once, only after the SDK confirms (`RewardGate`). Interstitials only at
  natural breaks (`AdPacing`).
