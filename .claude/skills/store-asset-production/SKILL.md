---
name: store-asset-production
description: Produce and validate every Google Play listing asset for a game - 512x512 app icon, Android adaptive/legacy launcher icons, 1024x500 feature graphic, real-app phone screenshots (1080x1920, 24-bit), promo videos with FFmpeg, the JSON validation report and the store-assets export folder. Use when preparing or refreshing store graphics, screenshots or the promo video.
argument-hint: "<game>"
---

# Store asset production

## Specs (support.google.com/googleplay/android-developer/answer/9866151, read 2026-10-26)
| Asset | Spec |
|---|---|
| App icon | 512x512, 32-bit PNG with alpha, <= 1024 KB, no badges/claims |
| Feature graphic | 1024x500, JPEG or 24-bit PNG, no alpha |
| Phone screenshots | 2-8, JPEG/24-bit PNG no alpha, 320-3840 px, long side <= 2x short side; featuring eligibility: >= 4 at >= 1080 px, 9:16 or 16:9 |
| Tablet 7"/10" | optional, 1080-7680 px, 16:9 / 9:16; only if the app really supports tablets |
| Promo video | YouTube URL (public/unlisted, embeddable, ads off, not age-restricted) |
Launcher icons: adaptive 108 dp (safe zone 66 dp) foreground + background (+ monochrome), legacy
48/72/96/144/192 px.

## Pipeline (from the workspace root)
```powershell
.\tools\store\render-brand-assets.ps1 -Game <game>          # icon 512, launcher PNGs, feature graphic, video backdrop, preview sheet
.\tools\store\capture-desktop.ps1 -Game <game> [-Video]      # real screenshots (+ promo videos via FFmpeg)
.\tools\store\validate-store-assets.ps1 -Game <game>         # JSON report, non-zero exit on FAIL
.\tools\store\export-store-assets.ps1 -Game <game>           # copies the kit to store-assets\ and revalidates
.\tools\store\build-listing-doc.ps1 -Game <game>             # Word listing from listing.json
```
Screenshots come from `lwjgl3/.../StoreCapture.java` (scripted, solver-played gameplay rendered
off-screen) or from a device with `capture-screenshot.ps1`. Never ship a mock-up as a screenshot.

## Rules
- Icon and feature graphic are rendered by script (GDI+), in the game's palette; the source is
  the script itself - regenerate rather than hand-edit.
- Screenshots: one real feature per image, no text overlays that lie, no Play badges, no
  invented numbers; captions + alt text in `listing.json`.
- Video: the game's own synthesised music (no licence claims); ads must stay off on YouTube.
- Every export passes `validate-store-assets.ps1`; content checks (readability, honesty) are
  flagged for human review in the report, never auto-PASSed.

## Deliverables
`play-store/<game>/` (working kit) and `store-assets/` (final exports + `validation-report/asset-validation.json`,
screenshot `validation-report.md`, promo-video storyboard/script).
