package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withQuestProgress;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.webui.RelayAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RelayAutomationTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("relay automation");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("relay automation");
  }

  @AfterEach
  public void afterEach() {
    RelayRequest.reset();
  }

  @Nested
  class HiddenTemple {
    @Nested
    class Dvorak {

      @AfterEach
      public void afterEach() {
        DvorakManager.reset();
      }

      private static final KoLAdventure HIDDEN_TEMPLE =
          AdventureDatabase.getAdventureByName("The Hidden Temple");

      public Cleanups withDvorak(FakeHttpClientBuilder builder) {
        var cleanups = new Cleanups(withHttpClientBuilder(builder), withPasswordHash("dvorak"));
        return cleanups;
      }

      public void addDvorakResponses(FakeHttpClientBuilder builder) {
        var client = builder.client;
        client.addResponse(200, html("request/test_automation_dvorak_1.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_automation_dvorak_2.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_automation_dvorak_3.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_automation_dvorak_4.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_automation_dvorak_5.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_automation_dvorak_6.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_automation_dvorak_7.html"));
        client.addResponse(200, ""); // api.php
      }

      public int validateDvorakRequests(FakeHttpClientBuilder builder, int i) {
        var client = builder.client;
        var requests = client.getRequests();
        assertPostRequest(requests.get(i++), "/tiles.php", "action=jump&whichtile=4&pwd=dvorak");
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(i++), "/tiles.php", "action=jump&whichtile=6&pwd=dvorak");
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(i++), "/tiles.php", "action=jump&whichtile=3&pwd=dvorak");
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(i++), "/tiles.php", "action=jump&whichtile=5&pwd=dvorak");
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(i++), "/tiles.php", "action=jump&whichtile=7&pwd=dvorak");
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(i++), "/tiles.php", "action=jump&whichtile=6&pwd=dvorak");
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(i++), "/tiles.php", "action=jump&whichtile=3&pwd=dvorak");
        assertGetRequest(requests.get(i++), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
        return i;
      }

      @ParameterizedTest
      @ValueSource(strings = {"None", "Quantum Terrarium"})
      public void canAutomateDvoraksRevengeFromRelayBrowser(String pathName) {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var path = AscensionPath.nameToPath(pathName);
        var cleanups = new Cleanups(withDvorak(builder), withPath(path));
        try (cleanups) {
          client.addResponse(200, html("request/test_automation_dvorak_0.html"));
          addDvorakResponses(builder);

          // Visit tiles.php
          var url = "tiles.php";
          var request = new RelayRequest(false);
          request.constructURLString(url);
          request.run();

          // Simulate user typing "solve" button
          url = "KoLmafia/specialCommand?cmd=dvorak&pwd=dvorak";
          request = new RelayRequest(false);
          request.constructURLString(url);
          request.run();

          // Wait until the submitted command is done
          request.waitForCommandCompletion();
          assertThat(RelayRequest.specialCommandResponse.length(), greaterThan(0));

          // Verify that expected requests were submitted
          var requests = client.getRequests();
          assertThat(requests, hasSize(16));

          int i = 0;
          assertGetRequest(requests.get(i++), "/tiles.php", null);
          validateDvorakRequests(builder, i);
        }
      }

      @Test
      public void canStepThroughDvoraksRevengeFromRelayBrowser() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups = new Cleanups(withDvorak(builder));
        try (cleanups) {
          client.addResponse(200, html("request/test_automation_dvorak_0.html"));
          addDvorakResponses(builder);

          // Visit tiles.php
          var url = "tiles.php";
          var request = new RelayRequest(false);
          request.constructURLString(url);
          request.run();

          // Simulate user repeatedly typing "step" button
          int count = 100;
          while (!ChoiceManager.handlingChoice && count-- > 0) {
            url = "/KoLmafia/waitSpecialCommand?cmd=dvorak+step&pwd=dvorak";
            request = new RelayRequest(false);
            request.constructURLString(url);
            request.run();

            // Wait until the submitted command is done
            request.waitForCommandCompletion();
          }

          // Verify that expected requests were submitted
          var requests = client.getRequests();
          assertThat(requests, hasSize(16));

          int i = 0;
          assertGetRequest(requests.get(i++), "/tiles.php", null);
          validateDvorakRequests(builder, i);
        }
      }

      @ParameterizedTest
      @ValueSource(strings = {"None", "Quantum Terrarium"})
      public void canAutomateDvoraksRevengeFromAdventureRequest(String pathName) {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var path = AscensionPath.nameToPath(pathName);
        var cleanups = new Cleanups(withDvorak(builder), withPath(path));
        try (cleanups) {
          client.addResponse(302, Map.of("location", List.of("tiles.php")), "");
          client.addResponse(200, html("request/test_automation_dvorak_0.html"));
          client.addResponse(200, ""); // api.php
          addDvorakResponses(builder);

          // AdventureRequest redirecting to tiles.php
          var request = HIDDEN_TEMPLE.getRequest();
          request.run();

          // Verify that expected requests were submitted
          var requests = client.getRequests();
          assertThat(requests, hasSize(18));

          int i = 0;
          assertPostRequest(
              requests.get(i++),
              "/adventure.php",
              "snarfblat=" + HIDDEN_TEMPLE.getAdventureNumber() + "&pwd=dvorak");
          assertGetRequest(requests.get(i++), "/tiles.php", null);
          assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
          validateDvorakRequests(builder, i);
        }
      }

      @ParameterizedTest
      @ValueSource(strings = {"None", "Quantum Terrarium"})
      public void canAutomateHiddenTemplePuzzleChain(String pathName) {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var path = AscensionPath.nameToPath(pathName);
        var cleanups =
            new Cleanups(
                withDvorak(builder), withQuestProgress(Quest.WORSHIP, "step2"), withPath(path));
        try (cleanups) {
          client.addResponse(200, html("request/test_temple_puzzle_0.html"));
          client.addResponse(200, html("request/test_temple_puzzle_1.html"));
          client.addResponse(302, Map.of("location", List.of("tiles.php")), "");
          client.addResponse(200, html("request/test_automation_dvorak_0.html"));
          addDvorakResponses(builder);
          client.addResponse(200, html("request/test_temple_puzzle_2.html"));
          if (path == Path.QUANTUM) {
            // At the end, we are no longer in a choice, and we will look at the Quantum Terrarium
            client.addResponse(200, "");
          }

          // Visit first puzzle choice
          // *** Need actual HTML
          var url = "choice.php?whichchoice=580&option=1";
          var request = new RelayRequest(false);
          request.constructURLString(url);
          request.run();
          assertThat(ChoiceManager.handlingChoice, equalTo(true));
          assertThat(ChoiceManager.lastChoice, equalTo(123));

          // Simulate user typing "auto" button
          url = "choice.php?action=auto";
          request = new RelayRequest(false);
          request.constructURLString(url);
          RelayAgent.automateChoiceAdventure(request);
          assertThat(ChoiceManager.handlingChoice, equalTo(false));
          assertThat(Quest.WORSHIP, isStep("step3"));

          // Verify that expected requests were submitted
          var requests = client.getRequests();
          int expectedRequests = path == Path.QUANTUM ? 21 : 20;
          assertThat(requests, hasSize(expectedRequests));

          int i = 0;
          assertPostRequest(requests.get(i++), "/choice.php", "whichchoice=580&option=1");
          assertPostRequest(
              requests.get(i++), "/choice.php", "whichchoice=123&option=2&pwd=dvorak");
          assertGetRequest(requests.get(i++), "/choice.php", null);
          assertGetRequest(requests.get(i++), "/tiles.php", null);
          i = validateDvorakRequests(builder, i);
          assertPostRequest(
              requests.get(i++), "/choice.php", "whichchoice=125&option=3&pwd=dvorak");
          if (path == Path.QUANTUM) {
            assertGetRequest(requests.get(i++), "/qterrarium.php", null);
          }
        }
      }
    }
  }
}
