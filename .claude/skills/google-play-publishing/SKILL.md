---
name: google-play-publishing
description: Prepare a game for Google Play - release checklist, signed AAB, version bump, target API, store listing text and graphics specs, Data safety, ads declaration, content rating, closed-testing requirement for new personal accounts, release notes. Use before any Play release. Never uploads or publishes.
argument-hint: "<project>"
---

# Google Play publishing preparation

## When to use
First release or update of a game. This skill prepares everything; **the user performs every
Play Console action** unless they explicitly authorize a specific step.

## Inputs
Project, release type (first release / update), developer account type (personal created after
2023-11-13 -> closed testing requirement applies), privacy policy URL, support email.

## Workflow
1. Walk `config/release-checklists/google-play-release.md` top to bottom; mark each item pass/fail/user.
2. Technical gates:
   - applicationId final and not `com.example.*`; versionCode > last uploaded.
   - targetSdk >= 36 (required for new apps and updates since 2026-08-31; extension form to 2026-11-01).
     Re-check https://developer.android.com/google/play/requirements/target-sdk at release time.
   - `.\tools\android-build\build-android.ps1 -Project <p> -Task Bundle` -> signed AAB (keystore.properties present).
   - Release build tested on a device: launch, play, pause/resume, back button, rotate lock, ads (demo IDs
     only in debug; release with production IDs is tested via internal testing track, no clicking).
3. Listing assets live in `play-store/<game>/` (see play-store/README.md): edit `listing.json`,
   regenerate the Word listing with `.\tools\store\build-listing-doc.ps1 -Game <game>` (it enforces
   the character limits), capture screenshots with `tools\store\capture-screenshot.ps1`.
   Specs (verify in Play Console help at release time):
   app icon 512x512 PNG (32-bit, <= 1 MB), feature graphic 1024x500, at least 2 phone screenshots
   (16:9 or 9:16, 320-3840 px per side), short description <= 80 chars, full description <= 4000 chars,
   title <= 30 chars.
4. Policy forms: privacy policy URL, Data safety (from privacy-and-consent), "Contains ads" = yes if any
   ad SDK, advertising ID declaration (GMA uses AD_ID), content rating questionnaire (IARC), target
   audience and content (if under 13 is included -> Families policy), app access (none needed for these games).
5. Testing tracks: internal testing first (fast, up to 100 testers). New personal accounts: closed test with
   >= 12 testers opted in for 14 consecutive days before applying for production access.
6. Release notes (<= 500 chars per language), staged rollout percentage suggestion for updates.

## Standards
- Play App Signing with a separate upload key; keystore and passwords never in the repo or chat.
- One versionCode per upload; tag the release in git when Git is installed.
- Check Android vitals (crash rate, ANR rate) on the internal track before production.

## Validation
`validate-workspace.ps1` passes (no secrets, targetSdk check), AAB signed (`jarsigner -verify`),
checklist has no "fail" items, remaining "user" items listed.

## Common failures
- "Version code already used" -> bump versionCode.
- "Your app targets API level 35" -> raise targetSdk to 36.
- Data safety rejected -> declared data doesn't match SDKs (AD_ID, device identifiers via ads).
- Ads declaration "No" while GMA is included -> mismatch warning/rejection.

## Deliverables
Filled checklist, AAB path/size, listing draft (title, short/full description, release notes),
and the list of Play Console / AdMob actions for the user.

## References
- https://support.google.com/googleplay/android-developer/answer/9859152 (app requirements / listing)
- https://support.google.com/googleplay/android-developer/answer/14151465 (testing requirements)
- https://support.google.com/googleplay/android-developer/answer/10787469 (Data safety)
- https://developer.android.com/guide/app-bundle
