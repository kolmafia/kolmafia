package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.setupFakeResponse;
import static internal.helpers.Player.withBasementLevel;
import static internal.helpers.Player.withContinuationState;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BasementRequestTest {

  @BeforeAll
  protected static void injectPreferences() {
    KoLCharacter.reset("BasementRequestTest");
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
    var cleanups =
        new Cleanups(
            setupFakeResponse(200, "Fernswarthy's Basement, Level 499: " + encounter),
            withBasementLevel(),
            withContinuationState());

    try (cleanups) {
      var req = new BasementRequest("Fernswarthy's Basement, Level 499");
      // Monster fights don't actually have stat requirements, so BasementRequest won't bail before
      // trying to enter the fight if we use run().
      req.run();
      assertEquals(monster, BasementRequest.basementMonster);
    }
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
    var cleanups =
        new Cleanups(
            setupFakeResponse(200, "Fernswarthy's Basement, Level 499: " + encounter),
            withBasementLevel(),
            withContinuationState());

    try (cleanups) {
      var req = new BasementRequest("Fernswarthy's Basement, Level 499");
      req.run();

      assertEquals(499, BasementRequest.getBasementLevel());
      assertEquals(6470, BasementRequest.getBasementTestValue());
      assertEquals(
          summary + ": 0 current, 6,470 needed", BasementRequest.getBasementLevelSummary());
    }
  }
}
