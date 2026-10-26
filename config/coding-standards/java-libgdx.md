# Java / libGDX coding standard

Applies to every project in `games/`, the template and Sortpuzzle. Sortpuzzle is the reference style.

## Structure
```
core/src/main/java/<package>/
  GameConfig.java   all tuning constants      model/  immutable state, no libGDX
  <Game>.java       Game subclass, services   logic/  rules, generators, solvers, no libGDX
  screens/          Screen classes            ui/     actors, skin, palette, procedural textures
  ads/ audio/ save/ platform services behind interfaces
core/src/test/java/<package>/  JUnit 5 mirrors model/ and logic/
```

## Rules
- Java 17 language level in `core` (D8 constraint); toolchain 21. No preview features.
- `final` classes by default; immutable value objects; `private` fields; no public mutable statics.
- No libGDX imports in `model/` and `logic/`. Inject `Random` and clocks.
- Constants: `UPPER_SNAKE` in `GameConfig`; no magic numbers in screens.
- Comments explain *why* (constraints, trade-offs), not what. Javadoc on public types and non-obvious methods.
- Errors: fail fast on programmer errors (`IllegalArgumentException`); never swallow exceptions silently;
  log with `Gdx.app.log/error(TAG, ...)`.
- Threads: only the render thread touches GL and scene2d; workers return via `Gdx.app.postRunnable`.
- Resources: whoever creates a `Disposable` disposes it; screens dispose in `dispose()`.
- No per-frame allocation in `render`/`act`/`draw`.
- Persistence classes used by `Json` get ProGuard keep rules and a version field.
- Dependencies: versions only in `gradle/libs.versions.toml`; every new library needs a reason in the README.

## Tests
- JUnit 5, `@DisplayName("behaviour sentence")`, one behaviour per test, deterministic, no sleeps/network.
- Every logic class has a test class; bugs get a regression test before the fix.

## Formatting
4-space indent, 120-column soft limit, UTF-8, imports grouped: java.*, android.*, third-party, project.
