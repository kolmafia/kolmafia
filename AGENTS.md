# KoLmafia

Java 21 desktop tool for [Kingdom of Loathing](https://kingdomofloathing.com).
Main class: `net.sourceforge.kolmafia.KoLmafia`.

## Build & test

```sh
./gradlew spotlessApply    # format (Google Java Style, enforced on PRs)
./gradlew :test --tests "fully.qualified.TestName"   # single test
./gradlew shadowJar        # build fat jar in dist/
./gradlew runShadow        # build and run
```

Tests run with `test/root` as working dir. JUnit 5 + Hamcrest.

## Test patterns

- Fixture HTML files in `test/root/request/` loaded via `Networking.html("request/test_foo.html")`
- State setup via `Player.withXxx()` helpers — `withProperty`, `withChoice`, `withFight`, `withClass`, `withNextMonster`, `withItem`, etc.
- Cleanup via try-with-resources on `Cleanups` (collects restore runnables, runs on close in order)
- Test directory mirrors source: `test/net/sourceforge/kolmafia/…` matches `src/net/sourceforge/kolmafia/…`
- Choice pages: use `Player.withChoice(choiceId, html)` to set `ChoiceManager.lastChoice`, `lastResponseText`, and `handlingChoice`
- Assert with Hamcrest `assertThat` matchers, not JUnit `assertEquals`

## Structure

- `src/net/sourceforge/kolmafia/` — core, session, webui, utilities, request, textui
- `src/data/` — bundled data files
- `test/internal/helpers/` — test utilities (Networking, Player, Cleanups, etc.)
- `test/root/request/` — HTML/JSON test fixtures (1537+ files)
