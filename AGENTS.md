# KoLmafia

Java 25 desktop tool for [Kingdom of Loathing](https://kingdomofloathing.com).
Main class: `net.sourceforge.kolmafia.KoLmafia`.

## Build & test

```sh
./gradlew spotlessApply    # format (Google Java Style, enforced on PRs)
./gradlew :test --tests "fully.qualified.TestName"   # single test
./gradlew shadowJar        # build fat jar in dist/
./gradlew runShadow        # build and run
```

Tests run with `src/test/root` as working dir. JUnit 5 + Hamcrest.

## Test patterns

- Fixture HTML files in `src/test/root/request/` loaded via `Networking.html("request/test_foo.html")`
- State setup via `Player.withXxx()` helpers — `withProperty`, `withChoice`, `withFight`, `withClass`, `withNextMonster`, `withItem`, etc.
- Cleanup via try-with-resources on `Cleanups` (collects restore runnables, runs on close in order)
- Test directory mirrors source: `src/test/java/net/sourceforge/kolmafia/…` matches `src/main/java/net/sourceforge/kolmafia/…`
- Choice pages: use `Player.withChoice(choiceId, html)` to set `ChoiceManager.lastChoice`, `lastResponseText`, and `handlingChoice`
- Assert with Hamcrest `assertThat` matchers, not JUnit `assertEquals`
- Add new tests to the bottom of existing test files, or in a relevant `@Nested` `class`. Group related tests in a `@Nested class` when adding.
- Writing user preferences (e.g. `withProperty("autoXxx", true)`) silently no-ops unless a user context exists. Add a `@BeforeAll` with `KoLCharacter.reset("<TestClassName>")` to the test class, otherwise `Preferences.getBoolean` keeps returning the default (`false`).

## Structure

- `src/main/java/net/sourceforge/kolmafia/` — core, session, webui, utilities, request, textui
- `src/main/resources/data/` — bundled data files
- `src/test/java/internal/helpers/` — test utilities (Networking, Player, Cleanups, etc.)
- `src/test/root/request/` — HTML/JSON test fixtures

## Game content
You can get information from the wiki at https://wiki.kingdomofloathing.com.

Note that if you're using `curl`, you'll need to provide a user agent to get through Cloudfront. For example:
```
curl https://wiki.kingdomofloathing.com/Main_Page -A 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36'
```

## Pull Requests

- Please mention in the Pull Request that you are an automated LLM agent, and note your model name and version for our records.

## Specific tasks

If a task matches one of the entries below, read the linked guide before starting and follow it.

| Task | Guide |
| --- | --- |
| Add a banish | [devdoc/adding-a-banish.md](devdoc/adding-a-banish.md) |
