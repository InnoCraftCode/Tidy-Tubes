# Skills catalog

Skills live in `.claude/skills/<name>/SKILL.md`. Only their one-line descriptions sit in context
until a task needs one; then the body loads (progressive disclosure). Supporting files (e.g.
`libgdx-game-development/3d.md`) load only when referenced.

| Skill | Trigger | Main output |
|---|---|---|
| game-project-bootstrap | start a new game | `games/<name>/` building, README |
| libgdx-game-development (+3d.md) | gameplay/UI/save/audio work | code + JUnit tests |
| android-build-and-export | builds, signing, SDK levels, Gradle errors | APK/AAB + warnings |
| mobile-game-performance | jank, heat, memory, size, startup | before/after numbers |
| admob-integration | add/port/debug AdMob | AdMobAdProvider wiring, config |
| rewarded-ads-design | where/how often ads go | placement table, caps |
| google-play-publishing | release preparation | filled checklist, listing draft |
| privacy-and-consent | SDKs, permissions, consent, Data safety | data-flow table, answers |
| mobile-game-testing | tests, device scenarios, crash repro | tests + results |
| game-architecture-review | review a diff or game | ranked findings |
| token-efficient-development | long tasks, large sessions | working method |
| premium-ui-design | redesign, new screen, design-system change | tokens, components, verified renders |
| store-asset-production | icon, feature graphic, screenshots, promo video, validation | validated `store-assets/` kit |

Not created on purpose: godot-2d/3d-development, unity-mobile-development (engines not used; see
ADR-001). Create them only together with an ADR adopting the engine.
