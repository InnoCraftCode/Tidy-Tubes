# play-store/

Everything you upload to Google Play, one folder per game (same name as in `games/`).

```
play-store/<game>/
  <AppName> - Fiche Google Play.docx   listing to copy into Play Console (generated)
  listing.json                         source of the texts - edit this, then regenerate the .docx
  graphics/icon-512.png                app icon 512x512, 32-bit PNG
  graphics/feature-graphic-1024x500.png  feature graphic, 24-bit PNG (no alpha)
  screenshots/phone/01-....png         2 to 8 phone screenshots (recommended 4+, 1080x1920)
  graphics/video-background-1920x1080.png  backdrop of the landscape promo video
  video/promo-landscape-1920x1080.mp4  promo video: upload to YouTube, paste the link in Play Console
  video/promo-portrait-1080x1920.mp4   same gameplay, vertical (Shorts / TikTok / Instagram)
```

Desktop capture needs a `StoreCapture` launcher in the game (`games/sortpuzzle/lwjgl3/.../StoreCapture.java`
is the reference: copy it and adapt its scripted scenes for another game).

## Commands (from the workspace root)
```powershell
# brand assets rendered by script (icon 512, Android launcher icons, feature graphic, video backdrop, preview sheet)
.\tools\store\render-brand-assets.ps1 -Game <game>
# technical validation against the Play specs -> <kit>\validation-report\asset-validation.json
.\tools\store\validate-store-assets.ps1 -Game <game>
# final deliverable folder store-assets\ (+ validation there)
.\tools\store\export-store-assets.ps1 -Game <game>
# folder + draft graphics + Word listing for a game (new-game.ps1 already does this)
.\tools\store\new-store-folder.ps1 -Game <game> -AppName "<Name>" -PackageId <package.id>
# redraw draft graphics (-Force overwrites; -Motif Tubes or Circles; colours as hex)
.\tools\store\generate-store-graphics.ps1 -Game <game> -Title "<Name>" -Force
# screenshots WITHOUT a phone: the desktop build plays itself and renders 1080x1920 (24-bit PNG)
.\tools\store\capture-desktop.ps1 -Game <game>
# ... plus the promo videos (needs FFmpeg): video\promo-landscape-1920x1080.mp4 + portrait version
.\tools\store\capture-desktop.ps1 -Game <game> -Video
# or take a screenshot from a connected phone/emulator
.\tools\store\capture-screenshot.ps1 -Game <game> -Name 01-accueil
# regenerate the Word listing after editing listing.json or adding screenshots (needs Microsoft Word)
.\tools\store\build-listing-doc.ps1 -Game <game>
```

The Word generator refuses texts over Google Play limits (title 30, short description 80,
full description 4000, release notes 500 characters).

## Notes
- Generated graphics are drafts in the game's colours; replace them with final art (same names/sizes).
- Screenshots must show real gameplay; no misleading UI. Take them from a release-like build with
  test ads hidden (`-Pads.enabled=false`) so no "Test Ad" banner appears in store images.
- `listing.json` is the source of truth. If you edit the .docx by hand, copy the change back to
  `listing.json` or it will be lost at the next regeneration.
- `_template/` is used for new games.
