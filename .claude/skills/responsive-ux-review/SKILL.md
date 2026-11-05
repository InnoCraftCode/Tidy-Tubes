---
name: responsive-ux-review
description: Check that a libGDX game's screens work on every Android form factor - portrait phone, tall 20:9 phone, phone in landscape, 16:10 tablet, unfolded foldable - by rendering real captures at each size and reviewing them against a checklist (clipping, tiny text, empty margins, touch targets, safe areas, dialogs that overflow). Use after any UI change, after "the app is not responsive", or before a release.
argument-hint: "<game> [screen]"
---

# Responsive UX review

Adapted from Claude Code Game Studios' `ux-review` (MIT, see `.claude/THIRD_PARTY.md`) for
libGDX on Android. The rule: **judge renders, not code.**

## 1. Render the device matrix (no phone needed)

The game's `:lwjgl3:storeCapture` task plays a scripted session and saves every key screen.
`-PcaptureOut` redirects the output so the store kit is untouched. Run from the game folder:

```powershell
$out = "$env:TEMP\responsive\<game>"
$matrix = @{ 'phone' = '1080x1920'; 'tall' = '1080x2400'; 'land-phone' = '2400x1080';
             'tablet' = '2560x1600'; 'fold' = '2208x1840' }
foreach ($name in $matrix.Keys) {
    .\gradlew.bat :lwjgl3:storeCapture "-PcaptureOut=$out\$name" "-PcaptureSize=$($matrix[$name])" -q
}
```

A game without a `StoreCapture` launcher: copy it from `games\sortpuzzle\lwjgl3` and adapt
its script. For a quick manual look, `.\gradlew.bat :lwjgl3:run` and resize the window.

## 2. Review each capture against the checklist

Open every PNG (the Read tool shows images). For each screen and size:

| Check | Fails when |
|---|---|
| Fills the screen | content is a thin portrait column with large empty sides in landscape |
| Readable | body text looks smaller than on the portrait phone (compare side by side) |
| Nothing clipped | a pill, card, button or dialog touches or crosses a screen edge |
| No overlap | the HUD covers the board, or two widgets overlap |
| Reachable | primary actions sit in the thumb zone (bottom in portrait, sides in landscape) |
| Touch targets | anything tappable is smaller than about 64 world units |
| Safe area | UI under a camera cut-out or rounded corner (the backdrop may go there) |
| Dialogs fit | a dialog is taller than the screen (Ui.dialog should scale it down) |
| Scroll | content taller than the screen does not scroll |
| Consistency | the same component looks different between orientations |

## 3. Fix in this order

1. The viewport: `BaseScreen` + `ui/Responsive` (orientation-aware minimum world).
2. Font density: `Responsive.fontDensity`, so text is rasterised for the real scale.
3. Per-screen `arrange(landscape)` / `onResized`: columns in landscape, a stack in portrait.
4. Hand-placed content (boards): compute from an area, e.g. `BoardLayout.fit`.

Keep pure arithmetic in a testable class (see `ResponsiveLayoutTest`).

## 4. Report

Write a short table: screen x size, then PASS / FAIL with what is wrong. Re-render after
fixes and confirm portrait did not regress, since portrait is what the store screenshots show.
