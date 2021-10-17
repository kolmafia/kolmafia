package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BasementRequestTest extends RequestTestBase {

  @BeforeAll
  protected static void injectPreferences() {
    Preferences.saveSettingsToFile = false;
    // Set a username so we can edit preferences and have per-user defaults.
    KoLCharacter.reset("fakeUserName");
  }

  @AfterAll
  protected static void cleanupSession() {
    KoLCharacter.reset("");
    Preferences.saveSettingsToFile = true;
  }

  private static Stream<Arguments> monsterFights() {
    return Stream.of(
        Arguments.of("Commence to Pokin'", "Beast with X Eyes"),
        Arguments.of("Don't Fear the Ear", "Beast with X Ears"),
        Arguments.of("Stone Golem'", "X Stone Golem"),
        Arguments.of("Hydra'", "X-headed Hydra"),
        Arguments.of("Toast that Ghost'", "Ghost of Fernswarthy's Grandfather"),
        Arguments.of("Bottles of Beer on a Golem", "X Bottles of Beer on a Golem"),
        Arguments.of("Collapse That Waveform!", "X-dimensional horror"));
  }

  @ParameterizedTest
  @MethodSource("monsterFights")
  void matchesMonsterFightFromResponse(String encounter, String monster) {
    var req = new BasementRequest("Fernswarthy's Basement, Level 499");

    // Monster fights don't actually have stat requirements, so BasementRequest won't bail before
    // trying to enter the fight if we use run().
    req.responseText = "Fernswarthy's Basement, Level 499: " + encounter;
    req.processResults();

    assertEquals(monster, BasementRequest.basementMonster);
  }

  private static Stream<Arguments> statTests() {
    return Stream.of(
        Arguments.of("Don't Wake the Baby", "Buffed Moxie Test"),
        Arguments.of("Grab a cue", "Buffed Moxie Test"),
        Arguments.of("Smooth Moves", "Buffed Moxie Test"),
        Arguments.of("Lift 'em", "Buffed Muscle Test"),
        Arguments.of("Push it Real Good", "Buffed Muscle Test"),
        Arguments.of("Ring that Bell", "Buffed Muscle Test"),
        Arguments.of("Gathering:  The Magic", "Buffed Mysticality Test"),
        Arguments.of("Mop the Floor", "Buffed Mysticality Test"),
        Arguments.of("Do away with the 'doo", "Buffed Mysticality Test"));
  }

  @ParameterizedTest
  @MethodSource("statTests")
  void matchesImpassableStatTestFromResponse(String encounter, String summary) {
    var req = spy(new BasementRequest("Fernswarthy's Basement, Level 499"));
    expectSuccess(req, "Fernswarthy's Basement, Level 499: " + encounter);
    // Clear the error state, since we can't pass any of these tests.
    KoLmafia.forceContinue();

    req.run();

    assertEquals(499, BasementRequest.getBasementLevel());
    assertEquals(6470, BasementRequest.getBasementTestValue());
    assertEquals(summary + ": 0 current, 6,470 needed", BasementRequest.getBasementLevelSummary());
  }
}
