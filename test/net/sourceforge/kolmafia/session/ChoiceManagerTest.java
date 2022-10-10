package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ChoiceManagerTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("choice manager user");
  }

  @BeforeEach
  public void beforeEach() {
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
        String urlString = "choice.php?whichchoice=1";
        var request = new GenericRequest(urlString);
        request.responseText = "Some normal choice text";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(false));
      }
    }

    // "Whoops!" testing (i.e. where it returns true) handled in
    // GenericRequestTest.detectsBogusChoices

    @Test
    public void returnsFalseWithNonChoiceRequest() {
      var cleanup = new Cleanups(withHandlingChoice());

      try (cleanup) {
        String urlString = "adventure.php?snarfblat=100";
        var request = new GenericRequest(urlString);
        request.responseText = "";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(false));
      }
    }

    @Test
    public void returnsFalseWithNonExecutedRequest() {
      var cleanup = new Cleanups(withHandlingChoice());

      try (cleanup) {
        String urlString =
            "choice.php?whichchoice=999&pwd&option=1&topper=3&lights=5&garland=1&gift=2";
        var request = new GenericRequest(urlString);

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(false));
      }
    }

    @Test
    public void returnsTrueWithAbortState() {
      var cleanup = new Cleanups(withHandlingChoice(), withContinuationState());

      try (cleanup) {
        String urlString = "choice.php?whichchoice=1234&pwd&option=1";
        var request = new GenericRequest(urlString);
        request.responseText = "Whoops!  You're not actually in a choice adventure.";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(true));
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ABORT));
      }
    }

    @Test
    public void returnsTrueWithoutAbortStateIfPreferenceFalse() {
      var cleanup =
          new Cleanups(
              withHandlingChoice(),
              withContinuationState(),
              withProperty("abortOnChoiceWhenNotInChoice", false));

      try (cleanup) {
        String urlString = "choice.php?whichchoice=1234&pwd&option=1";
        var request = new GenericRequest(urlString);
        request.responseText = "Whoops!  You're not actually in a choice adventure.";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(true));
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
      }
    }

    @Test
    public void returnsTrueWithoutAbortState() {
      var cleanup = new Cleanups(withHandlingChoice(), withContinuationState());

      try (cleanup) {
        String urlString = "choice.php";
        var request = new GenericRequest(urlString);
        request.responseText = "Whoops!  You're not actually in a choice adventure.";

        assertThat(ChoiceManager.bogusChoice(urlString, request), is(true));
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
      }
    }
  }

  @Nested
  class ChoiceRedirection {
    @Test
    public void canRedirectToChoiceInGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("test"),
              // Avoid health warning
              withHP(100, 100, 100));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, "test1");

        var url = "inv_use.php?which=3&whichitem=4509&pwd=test";
        var request =
            new GenericRequest(url) {
              @Override
              protected boolean shouldFollowRedirect() {
                return true;
              }
            };
        request.run();
        assertEquals("test1", request.responseText);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=4509&pwd=test");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      }
    }

    @Test
    public void canProcessChoiceInGenericRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("test"),
              // Avoid health warning
              withHP(100, 100, 100));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, "test2");

        var url = "inv_use.php?which=3&whichitem=4509&pwd=test";
        var request =
            new GenericRequest(url) {
              @Override
              protected boolean shouldFollowRedirect() {
                return false;
              }
            };
        request.run();
        assertEquals("test2", request.responseText);

        // Although shouldFollowRedirect() was false, since it redirected to
        // choice.php, GenericRequest let ChoiceManager handle it

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=4509&pwd=test");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      }
    }

    @Test
    public void canNotRedirectToChoiceInRelayRequest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("test"),
              // Avoid health warning
              withHP(100, 100, 100));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, "test3");

        var url = "inv_use.php?which=3&whichitem=4509&pwd=test";
        var request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();
        assertEquals("", request.responseText);

        var choice = new GenericRequest("choice.php?forceoption=0", false);
        choice.run();
        assertEquals("test3", choice.responseText);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=4509&pwd=test");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      }
    }
  }
}
