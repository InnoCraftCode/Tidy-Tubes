# Google Play release checklist

Copy into the game's folder as `RELEASE-<version>.md` and mark each line: `[x]` pass, `[ ]` open, `[U]` user action.
Requirements change: re-verify the linked pages at release time and note the date checked.

## 1. Identity and versioning
- [ ] applicationId final, reverse-domain, not `com.example.*` (permanent after first upload)
- [ ] App name (<= 30 chars) in `strings.xml` and listing
- [ ] versionCode greater than the last uploaded; versionName updated
- [ ] Release notes written (<= 500 chars per language)

## 2. Build
- [ ] targetSdk >= 36 and compileSdk >= 36 (required for new apps/updates since 2026-08-31) - https://developer.android.com/google/play/requirements/target-sdk
- [ ] minSdk 24 (AdMob requirement); 64-bit ABI included (arm64-v8a)
- [ ] `build-android.ps1 -Task Bundle` -> signed AAB; `jarsigner -verify` OK
- [ ] Play App Signing enrolled; upload key backed up outside the repo (password manager + offline copy)
- [ ] R8 on; release build installed on a device and fully played (save/load after update install)
- [ ] `validate-workspace.ps1` passes (no secrets, no debug ad IDs in release config)

## 3. Quality
- [ ] `:core:test` green
- [ ] Device scenarios (`shared/testing/device-test-matrix.md`) on >= 1 low-end and 1 recent phone
- [ ] No crash/ANR on internal testing track; Android vitals checked (pre-launch report reviewed)
- [ ] Cold start < 3 s; 60 FPS in normal play

## 4. Ads (if any)
- [ ] `config/release-checklists/ads-integration.md` complete
- [ ] Play Console "Contains ads" = Yes
- [ ] Advertising ID declaration completed (GMA uses AD_ID)

## 5. Privacy and policy
- [ ] Privacy policy hosted at a public URL, linked in the listing and reachable in-app
- [ ] Data safety form matches SDK behaviour (`shared/privacy/data-safety-worksheet.md`)
- [ ] Content rating questionnaire (IARC) completed
- [ ] Target audience and content set; if under-13 included -> Families policy satisfied
- [ ] App access: "no special access needed" (or instructions provided)
- [ ] Permissions in the merged manifest all justified

## 6. Store listing
- [ ] Short description (<= 80), full description (<= 4000), no keyword stuffing, no false claims
- [ ] App icon 512x512 PNG; feature graphic 1024x500
- [ ] >= 2 phone screenshots (real gameplay, no misleading UI); optional promo video
- [ ] Category, contact email, (optional) website
- [ ] Countries/regions selected; price = free

## 7. Testing track and rollout
- [ ] Internal testing release done and installed by the developer
- [ ] New personal account: closed test with >= 12 testers opted in for 14 consecutive days, then apply for production - https://support.google.com/googleplay/android-developer/answer/14151465
- [ ] Production: staged rollout (e.g. 20%) for updates; monitor vitals before 100%

**Uploading, publishing and rollout are done by the user in Play Console. Claude prepares only.**
