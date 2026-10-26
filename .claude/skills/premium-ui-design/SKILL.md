---
name: premium-ui-design
description: Design and implement a premium, coherent UI for a libGDX scene2d game - design tokens (Palette), FreeType typography at device density, SDF-generated shapes and icons, component kit (Ui.java), screen layouts with ExtendViewport, states (loading/empty/error), onboarding, accessibility checks, and verification on real renders. Use for any visual redesign, new screen or design-system change.
argument-hint: "<project> [screen]"
---

# Premium UI for libGDX games

## When to use
Redesigning a game, adding a screen or dialog, changing the design system, or reviewing visual
quality before store captures.

## Reference implementation
`games/sortpuzzle/core/src/main/java/com/ben/laama/sortpuzzle/ui/`:
- `Palette.java` - every colour token (backdrop, surfaces, accents, text, balls).
- `SkinFactory.java` - fonts rasterised with gdx-freetype at `min(screenW/720, screenH/1280)`
  density then scaled back (`setDensityOverride` for off-screen captures); nine-patches
  (`card`, `card-sm`, `pill`, `pill-lg`, outlines); `ball`, `circle`, `glow`, `bg`; `icon-*`.
- `Sdfs.java` + `Tex.fromSdf` - analytic anti-aliased shapes (circle, round box, segment, arc,
  convex polygon, arrow head, tube). `Icons.java` - the icon set, 96 px, 8 px stroke.
- `Ui.java` - component kit: `iconButton`, `button`, `twoLineButton`, `card`, `chip`,
  `divider`, `Toggle`, `dialog(skin, title, onResult)`, `paragraph`, `toast`.
- `TubeTextures.java` - per-size U-shaped tube textures (cannot be nine-patches).
- `BrandMark.java` - the logo built from game actors.
Docs: `docs/design-system.md`, `docs/ui-refonte.md`.

## Workflow
1. Tokens first: edit `Palette`, never hard-code a colour in a screen.
2. Fonts: static TTFs under `android/src/main/assets/fonts/` with their licence file; sizes in
   world units (display 58, title 40, heading 30, body 24, label 20, small 17).
3. Shapes: add an SDF to `Sdfs`/`Icons`, register in `SkinFactory`, name it `icon-<verb>`.
4. Components: extend `Ui`, not the screens. Touch targets >= 64 world units.
5. Screens: `BaseScreen` gives an `ExtendViewport` (720x1280 min, 1000x1720 max); use
   `stage.getWidth()/getHeight()` for hand-placed actors, fill-parent tables otherwise.
6. States: loading indicator, empty/locked states, error toasts (`Ui.toast`), confirm dialogs
   for destructive actions, first-launch "how to play".
7. Verify on renders: `.\tools\store\capture-desktop.ps1 -Game <project>` and read the PNGs
   (downscale with System.Drawing first to save tokens). Then `:core:test`.

## Rules
- Contrast >= 4.5:1 for body text on its surface; dim text only for secondary info.
- Motion: 120-300 ms, `swingOut`/`sine`, never blocks input, clamp `delta` to 1/30.
- No per-frame allocation in `draw()`; every generated Texture is owned by the Skin or a
  `Disposable` cache disposed in `Main.dispose()`.
- No ads vocabulary in an ad-free build; rewards go through `RewardOffer`.

## Validation
`gradlew :core:test` green, `:android:assembleDebug` green, captures reviewed, no overlapping
text at 720x1280 and at 720x1616 (20:9 phones).
