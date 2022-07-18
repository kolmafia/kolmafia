package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.setupFakeResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BasementRequestTest {

  @BeforeAll
  protected static void injectPreferences() {
    // Set a username so we can edit preferences and have per-user defaults.
    KoLCharacter.reset("fakeUserName");
  }

  @ParameterizedTest
  @CsvSource({
    "Commence to Pokin', Beast with X Eyes",
    "Don't Fear the Ear, Beast with X Ears",
    "Stone Golem', X Stone Golem",
    "Hydra', X-headed Hydra",
    "Toast that Ghost', Ghost of Fernswarthy's Grandfather",
    "Bottles of Beer on a Golem, X Bottles of Beer on a Golem",
    "Collapse That Waveform!, X-dimensional horror"
  })
  void matchesMonsterFightFromResponse(String encounter, String monster) {
    var req = new BasementRequest("Fernswarthy's Basement, Level 499");

    // Monster fights don't actually have stat requirements, so BasementRequest won't bail before
    // trying to enter the fight if we use run().
    req.responseText = "Fernswarthy's Basement, Level 499: " + encounter;
    req.processResults();

    assertEquals(monster, BasementRequest.basementMonster);
  }

  @ParameterizedTest
  @CsvSource({
    "Don't Wake the Baby, Buffed Moxie Test",
    "Grab a cue, Buffed Moxie Test",
    "Smooth Moves, Buffed Moxie Test",
    "Lift 'em, Buffed Muscle Test",
    "Push it Real Good, Buffed Muscle Test",
    "Ring that Bell, Buffed Muscle Test",
    "Gathering:  The Magic, Buffed Mysticality Test",
    "Mop the Floor, Buffed Mysticality Test",
    "Do away with the 'doo, Buffed Mysticality Test"
  })
  void matchesImpassableStatTestFromResponse(String encounter, String summary) {
    var req = new BasementRequest("Fernswarthy's Basement, Level 499");

    var cleanups = setupFakeResponse(200, "Fernswarthy's Basement, Level 499: " + encounter);

    try (cleanups) {
      req.run();
    }

    // Clear the error state, since we can't pass any of these tests.
    KoLmafia.forceContinue();

    assertEquals(499, BasementRequest.getBasementLevel());
    assertEquals(6470, BasementRequest.getBasementTestValue());
    assertEquals(summary + ": 0 current, 6,470 needed", BasementRequest.getBasementLevelSummary());
  }
}
