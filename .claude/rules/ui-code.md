---
paths:
  - "games/*/core/src/main/java/**/screens/**"
  - "games/*/core/src/main/java/**/ui/**"
  - "templates/libgdx-2d/core/src/main/java/**/screens/**"
  - "templates/libgdx-2d/core/src/main/java/**/ui/**"
---

# UI code rules (libGDX scene2d)

Adapted from Claude Code Game Studios (MIT, see `.claude/THIRD_PARTY.md`) for this workspace.

- Screens draw and route input only. They never change game state directly: they call
  `logic/` and `model/` classes and render the result.
- **Responsive by default.** Never assume portrait. `BaseScreen` switches the viewport
  between 720x1280 (portrait) and 1280x800 (landscape) minimums (`ui/Responsive`); a screen
  that lays out by hand overrides `onResized` and branches on `isLandscape()`.
- Build widgets once, arrange them in an `arrange(landscape)` method that calls
  `clearChildren()` and re-adds the same instances. Never rebuild state-holding widgets on
  rotation.
- No hard-coded board or HUD offsets: use the reserves exposed by the HUD
  (`HudTable.PORTRAIT_TOP`, `LANDSCAPE_SIDE`, ...) and `BoardLayout.fit` for tube grids.
- Fixed widths (500, 640, ...) are fine for a column, but a column must fit the smallest
  world in its orientation: 720 wide in portrait, 800 high in landscape. Anything taller
  scrolls (`ScrollPane`) or uses `Ui.dialog`, which scales itself to fit.
- **Never call `Label.setFontScale` directly.** It is absolute in libGDX and replaces the
  font's 1/density base scale, so "shrink to 0.8" draws text 2x bigger on a phone. Use
  `Ui.setTextScale(label, factor)` / `Ui.textScale(label)` (relative). Long names in fixed-width
  cells get `setEllipsis(true)` plus `minWidth(0)` on their cell, never an unbounded `expandX`.
- Touch targets are at least 64 world units (about 48 dp). Text is never below the `small`
  style (17 units).
- Never convey state by colour alone: pair it with an icon, a label or a shape
  (`colorLabels` exists for this).
- Animations are 80-300 ms, never block input, and allocate nothing per frame.
- UI sounds go through `SoundManager` cues, never `Gdx.audio` directly.
- **Verify on renders, not by reading code.** Before calling UI work done, run the
  `responsive-ux-review` skill (portrait phone, tall phone, landscape phone, tablet,
  foldable).
