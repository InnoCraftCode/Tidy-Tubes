---
paths:
  - "games/*/core/src/test/**"
  - "templates/libgdx-2d/core/src/test/**"
---

# Test standards (JUnit 5)

Adapted from Claude Code Game Studios (MIT, see `.claude/THIRD_PARTY.md`) for this workspace.

- One test class per production class: `<Class>Test` in the same package.
- Method names state the scenario and the expected result in camelCase:
  `landscapePhoneNoLongerShrinksToHalfSize()`, `rewardIsGrantedOnlyOnce()`. Never `test1()`.
- Arrange / act / assert, visibly separated. One behaviour per test.
- Deterministic: fixed seeds, injected dates (`LocalDate`, epoch day), no network, no files
  outside a `@TempDir`, no `Thread.sleep`.
- Exact assertions (`assertEquals(75, health)`), with an epsilon for floats. Not
  `assertTrue(x < 100)`.
- No libGDX backend in unit tests. Code that needs one belongs in a screen, verified on
  desktop renders (`:lwjgl3:storeCapture`), not in JUnit.
- **Every bug fix ships a regression test, and you must see it fail first.** Run it against
  the unfixed code, confirm it fails, then apply the fix and confirm it passes. A test you
  have only seen pass is not known to test anything.
- Narrowest run first: `.\gradlew.bat :core:test --tests <Class>`, the full suite only
  after that passes.
