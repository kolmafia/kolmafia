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
- Add new tests to the bottom of existing test files, or in a relevant `@Nested` `class`. Group related tests in a `@Nested class` when adding.

## Structure

- `src/net/sourceforge/kolmafia/` — core, session, webui, utilities, request, textui
- `src/data/` — bundled data files
- `test/internal/helpers/` — test utilities (Networking, Player, Cleanups, etc.)
- `test/root/request/` — HTML/JSON test fixtures

## Game content
You can get information from the wiki at https://wiki.kingdomofloathing.com.

Note that if you're using `curl`, you'll need to provide a user agent to get through Cloudfront. For example:
```
curl https://wiki.kingdomofloathing.com/Main_Page -A 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36'
```

### Adding a banish

Follow an existing one end-to-end (e.g. `BANISHING_SHOUT`, `PUNCH_OUT_YOUR_FOE`):

1. `src/data/classskills.txt` — source of skill id/name/image/tags (e.g. skill 253 "Order a Kneecapping" lives here)
2. `SkillPool.java` — add `public static final int NAME = <skillId>`
3. `BanishManager.java` — add a `Banisher` enum entry `NAME("display name", duration, queueSize, isTurnFree, resetType)`; use `Reset.ROLLOVER_RESET` for "until rollover" banishes, `duration = -1` unless turn-based; `isTurnFree = false` unless the wiki mentions the skill does not take a turn
4. `FightRequest.payActionCost` — add `case SkillPool.NAME` matching a distinctive `responseText` substring, calling `BanishManager.banishMonster(monster, Banisher.NAME)`; set `skillSuccess` / `skillRunawaySuccess = true` (also drives the dailylimits increment below)
5. Daily limit (N/day): add `Cast\t<skill>\t<pref>\tN` to `src/data/dailylimits.txt` and `user\t<pref>\t0` to `src/data/defaults.txt`
6. Test: fixture in `test/root/request/test_fight_*.html` + `FightRequestTest` case via `parseCombatData(..., "fight.php?action=skill&whichskill=<id>")`

Note: wiki "When Used" text may be a placeholder ("This skill needs its in-game message"); treat it as a starting point and verify the matched substring against a real combat log. Also, try to avoid parts of the string that may change against plural monsters, or monsters with different pronouns (e.g. he / she / it).

## Pull Requests

- Please mention in the Pull Request that you are an automated LLM agent, and note your model name and version for our records.
