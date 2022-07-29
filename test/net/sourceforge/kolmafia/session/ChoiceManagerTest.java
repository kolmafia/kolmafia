package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHandlingChoice;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ChoiceManagerTest {

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("choice manager user");
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @BeforeEach
  private void beforeEach() {
    ChoiceManager.lastChoice = 0;
    ChoiceManager.lastDecision = 0;
  }

  @Test
  public void thatUnknownTombDynamicChoicesWork() {
    // There is a riddle and three possible answers. These are based on your character class.
    // The order of the answers is shuffled every time the choice page is reloaded.
    // One answer is right and two are wrong.
    //
    // Choice Manager has special handling to automatically choose the correct answer.
    //
    // These response texts are for an Accordion Thief.
    // The wrong answers are "Stealth" and "an accordion".
    // The right answer is "Music.".

    KoLCharacter.setAscensionClass(AscensionClass.ACCORDION_THIEF);

    String responseText = html("request/test_choice_manager_unknown_tomb_1.html");
    int choice = ChoiceUtilities.extractChoice(responseText);
    assertEquals(1049, choice);
    Map<Integer, String> choices = ChoiceUtilities.parseChoices(responseText);
    assertEquals(3, choices.size());
    assertEquals("&quot;Music.&quot;", choices.get(1));
    // specialChoiceDecision1(int choice, String decision, int stepCount, String responseText)
    String option = ChoiceManager.specialChoiceDecision1(choice, "", 0, responseText);
    assertEquals("1", option);

    responseText = html("request/test_choice_manager_unknown_tomb_2.html");
    choice = ChoiceUtilities.extractChoice(responseText);
    assertEquals(1049, choice);
    choices = ChoiceUtilities.parseChoices(responseText);
    assertEquals(3, choices.size());
    assertEquals("&quot;Music.&quot;", choices.get(2));
    // specialChoiceDecision1(int choice, String decision, int stepCount, String responseText)
    option = ChoiceManager.specialChoiceDecision1(choice, "", 0, responseText);
    assertEquals("2", option);

    responseText = html("request/test_choice_manager_unknown_tomb_3.html");
    choice = ChoiceUtilities.extractChoice(responseText);
    assertEquals(1049, choice);
    choices = ChoiceUtilities.parseChoices(responseText);
    assertEquals(3, choices.size());
    assertEquals("&quot;Music.&quot;", choices.get(3));
    // specialChoiceDecision1(int choice, String decision, int stepCount, String responseText)
    option = ChoiceManager.specialChoiceDecision1(choice, "", 0, responseText);
    assertEquals("3", option);
  }

  @Nested
  class BogusChoices {
    @Test
    public void returnsFalseWithNormalChoice() {
      var cleanup = new Cleanups(withHandlingChoice());

      try (cleanup) {
        var request = new GenericRequest("choice.php?whichchoice=1");
        request.responseText = "Some normal choice text";

        assertThat(ChoiceManager.bogusChoice(request), is(false));
      }
    }

    // "Whoops!" testing (i.e. where it returns true) handled in
    // GenericRequestTest.detectsBogusChoices

    @Test
    public void returnsFalseWithNonChoiceRequest() {
      var cleanup = new Cleanups(withHandlingChoice());

      try (cleanup) {
        var request = new GenericRequest("adventure.php?snarfblat=100");
        request.responseText = "";

        assertThat(ChoiceManager.bogusChoice(request), is(false));
      }
    }

    @Test
    public void returnsFalseWithNonExecutedRequest() {
      var cleanup = new Cleanups(withHandlingChoice());

      try (cleanup) {
        var request =
            new GenericRequest(
                "choice.php?whichchoice=999&pwd&option=1&topper=3&lights=5&garland=1&gift=2");

        assertThat(ChoiceManager.bogusChoice(request), is(false));
      }
    }
  }
}
