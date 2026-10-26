---
description: Add, port or configure AdMob (Next-Gen SDK + UMP) in a game through AdProvider
argument-hint: "<project>"
---

Ads for: $ARGUMENTS

Use the ads-monetization-agent (admob-integration + privacy-and-consent skills). Copy from
`templates/libgdx-2d` instead of writing new ad code. Debug builds keep Google demo IDs. If the user
wants production IDs, have them create `admob.properties` from `admob.properties.example` themselves.
Verify: tests green, debug APK builds, `:android:printAdConfig`. List the AdMob/Play account steps
that remain for the user. Never claim the game will earn revenue.
