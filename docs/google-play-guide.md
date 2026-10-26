# Google Play guide

Checklist (canonical): [config/release-checklists/google-play-release.md](../config/release-checklists/google-play-release.md).
Claude prepares with `/prepare-play-store <project>`; **you** perform every Play Console action.

## One-time account steps (you)
1. Create a Play Console developer account (one-time fee), verify identity.
2. Personal accounts created after 2023-11-13: before production you must run a **closed test with
   at least 12 testers opted in for 14 consecutive days**, then apply for production access.
   Plan testers (friends/family Google accounts) early.

## Per game
1. Raise targetSdk/compileSdk to **36** (required since 2026-08-31) - see [installation.md](installation.md).
2. Create the upload key and `keystore.properties` (template file explains the `keytool` command).
   Back up the `.jks` and passwords outside the workspace. Losing the upload key = support request to reset.
3. Build: `.\tools\android-build\build-android.ps1 -Project <name> -Task Bundle` -> `android-release.aab`.
4. Play Console > Create app > fill App content: privacy policy URL, ads = yes, Data safety
   ([worksheet](../shared/privacy/data-safety-worksheet.md)), content rating, target audience, advertising ID.
5. Store listing: everything is prepared in `play-store/<game>/` - open the Word file
   `<AppName> - Fiche Google Play.docx` and copy each field; upload `graphics/icon-512.png`,
   `graphics/feature-graphic-1024x500.png` and the `screenshots/phone/` images (see play-store/README.md).
6. Testing > Internal testing: upload the AAB, install via the opt-in link, play it fully.
7. Closed testing (if required for your account), then Production with a staged rollout.
8. Each update: bump versionCode, rebuild AAB, release notes.

Requirements change; recheck https://developer.android.com/google/play/requirements/target-sdk and
https://support.google.com/googleplay/android-developer/answer/14151465 before each release.
