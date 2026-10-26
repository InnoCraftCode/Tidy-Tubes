# Privacy guide

Procedure for Claude: `.claude/skills/privacy-and-consent/SKILL.md`. This page is the human summary.

- **What the games collect**: nothing themselves (progress stays on the device). The AdMob SDK
  collects device identifiers (advertising ID), approximate location from IP, ad interactions and
  diagnostics. That is why Data safety answers are "Yes, collects and shares".
- **Consent**: the template asks Google's UMP on every launch; EEA/UK/US-state users see your AdMob
  consent message; ads load only when allowed; a "Privacy options" button appears where required.
  You must create/publish the messages in AdMob > Privacy & messaging.
- **Privacy policy**: required for apps with ads. Start from
  [privacy-policy-template.md](../shared/privacy/privacy-policy-template.md), adapt it, host it on a
  public URL (e.g. a GitHub Pages or Google Sites page), link it in Play and in the game.
- **Children**: if your audience includes under-13s, Google's Families policy applies (certified ad
  SDKs, child-directed ad requests). The current games are not designed for that; decide explicitly.
- **Secrets**: keystores, passwords and `.env` never go in the repo or in chat. Claude is blocked from
  reading them by `.claude/settings.json`; `validate-workspace.ps1` scans for leaks.
