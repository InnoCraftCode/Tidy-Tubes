# Promo video — script, production and YouTube procedure

## Script (no voice-over; on-screen is the real UI only)

1. **Open** on the home screen for 1.6 s. The brand mark bobs; the title reads "Tidy Tubes".
2. **Play** level 6 in full (solver-driven, 0.62 s per move). Let the praise words and the
   "Perfect!" celebration play; hold the result card 2.4 s.
3. **Hint**: open level 28, play three moves, press Hint; hold 2.2 s while the gold rims pulse and
   the ghost ball flies three times.
4. **Scale**: open level 104 (ten colours), four moves, hold 2.8 s.
5. **Close** on the home screen, 1.6 s, fade to black.

Optional captions for a cut with text (not in the delivered files, keep them short and true):
"Every level solvable" · "Unlimited undo" · "Hints from a real solver" · "Up to 10 colours".

## Reproduce / edit

```powershell
# frames (1080x1920, 30 fps) + both MP4s
.\tools\store\capture-desktop.ps1 -Game sortpuzzle -Video
# backdrop of the landscape cut
.\tools\store\render-brand-assets.ps1 -Game sortpuzzle
```
Change the scenes in `StoreCapture.videoScript()`; change encoding (CRF 18, slow preset, yuv420p,
faststart) in `tools/store/make-promo-video.ps1`. Frames stay in
`games/sortpuzzle/lwjgl3/build/capture/frames/` (not shipped).

## Publish on YouTube (manual, by the account owner)

1. Upload `promo-landscape-1920x1080.mp4` (Play displays promo videos in 16:9). Title:
   "Tidy Tubes — Ball Sort Puzzle (gameplay)". Description: one line + the Play link once live.
2. Visibility **Public or Unlisted** (not Private). **Not** made for kids unless the Play target
   audience includes children. **No age restriction.**
3. **Monetisation off** for this video; if YouTube flags a copyright claim, replace the audio
   (the delivered audio is original, so no claim is expected).
4. Allow embedding (Advanced settings). Do not use a playlist URL or timecodes.
5. Paste the plain video URL into Play Console › Store listing › Promo video.
6. Check the listing preview: the first 30 s autoplay muted.

Nothing here is automated: no upload is performed by the scripts.
