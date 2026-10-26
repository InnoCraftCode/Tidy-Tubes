---
name: privacy-and-consent
description: Handle user privacy for Android games with ads - UMP consent (GDPR/EEA, UK, US states), privacy options entry point, permissions minimisation, Data safety answers, privacy policy, children/Families policy, secret handling. Use when adding SDKs/permissions, before release, or when consent behaviour is in question.
---

# Privacy and consent

## When to use
Any change that affects what data leaves the device (new SDK, permission, analytics), consent UI,
privacy policy, Data safety form, or a pre-release review.

## Inputs
SDK list (`gradle/libs.versions.toml`), merged manifest, target audience/age, regions targeted,
privacy policy URL (or need to draft one).

## Workflow
1. Inventory data flows: for each SDK, what it collects (GMA: device identifiers incl. advertising ID,
   IP-derived coarse location, app interactions, diagnostics). Use Google's SDK data disclosure pages.
2. Consent (template `AdMobAdProvider` already does this):
   - `requestConsentInfoUpdate` every launch -> `loadAndShowConsentFormIfRequired` -> ads only when
     `canRequestAds()`.
   - Show a "Privacy options" button when `getPrivacyOptionsRequirementStatus() == REQUIRED`;
     it calls `showPrivacyOptionsForm`.
   - The consent messages themselves (GDPR, US states) are configured by the user in AdMob >
     Privacy & messaging. Remind them; you cannot do it from code.
3. Test consent: `admob.testDeviceIds` + `ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA`
   (add temporarily in debug), `consentInformation.reset()` to replay first launch. Remove before release.
4. Permissions: the game itself needs only `INTERNET` (ads). Verify the merged manifest; anything
   else needs a reason or a `tools:node="remove"`.
5. Data safety draft: use `shared/privacy/data-safety-worksheet.md`.
6. Privacy policy: start from `shared/privacy/privacy-policy-template.md`; must be hosted at a public
   URL and match the app's real behaviour. The user is responsible for the legal text.
7. Children: if the target audience includes under-13s, Families policy applies - only
   Families-certified ad SDKs, `setTagForChildDirectedTreatment`, no personalised ads. Escalate to the user.

## Secrets
Keystores, passwords, `.env`, API tokens: never committed, never printed, never read by Claude
(settings deny rules). `validate-workspace.ps1` scans for leaks. Ad unit IDs are identifiers, not secrets.

## Validation
- Merged manifest permissions listed and justified.
- EEA debug run shows the consent form; declining still lets the game run (non-personalised or no ads).
- `validate-workspace.ps1` secret scan clean.

## Common failures
- Ads requested before consent -> policy violation in EEA/UK.
- No privacy options entry when required -> users can't change consent.
- Data safety says "no data collected" while GMA is present -> rejection.

## Deliverables
Data-flow table, Data safety answers draft, consent test evidence, policy gaps for the user.

## References
- https://developers.google.com/admob/android/privacy
- https://support.google.com/googleplay/android-developer/answer/10787469 (Data safety)
- https://support.google.com/googleplay/android-developer/answer/9893335 (Families)
- https://support.google.com/admob/answer/10113207 (Privacy & messaging)
