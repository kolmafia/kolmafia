package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Networking.printRequests;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.webui.RelayAgent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class RelayAutomationTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("relay automation");
    Preferences.saveSettingsToFile = false;
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("relay automation");
  }

  @AfterAll
  public static void afterAll() {
    Preferences.saveSettingsToFile = true;
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
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
        return i;
      }

      @Test
      public void canAutomateDvoraksRevengeFromRelayBrowser() {
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

          // Simulate user typing "solve" button
          url = "KoLmafia/specialCommand?cmd=dvorak&pwd=dvorak";
          request = new RelayRequest(false);
          request.constructURLString(url);
          request.run();

          // Wait until the submitted command is done
          request.waitForCommandCompletion();

          // Verify that expected requests were submitted
          var requests = client.getRequests();
          assertThat(requests, hasSize(15));

          int i = 0;
          assertGetRequest(requests.get(i++), "/tiles.php", null);
          validateDvorakRequests(builder, i);
        }
      }

      @Test
      public void canAutomateDvoraksRevengeFromAdventureRequest() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups = new Cleanups(withDvorak(builder));
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
          assertThat(requests, hasSize(17));

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

      @Test
      public void canAutomateHiddenTemplePuzzleChain() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups = new Cleanups(withDvorak(builder));
        try (cleanups) {
          client.addResponse(200, html("request/test_temple_puzzle_0.html"));
          client.addResponse(200, html("request/test_temple_puzzle_1.html"));
          client.addResponse(302, Map.of("location", List.of("tiles.php")), "");
          client.addResponse(200, html("request/test_automation_dvorak_0.html"));
          addDvorakResponses(builder);

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

          // Verify that expected requests were submitted
          var requests = client.getRequests();
          printRequests(requests);

          /*
          assertThat(requests, hasSize(15));
          */

          int i = 0;
          assertPostRequest(requests.get(i++), "/choice.php", "whichchoice=580&option=1");
          assertPostRequest(
              requests.get(i++), "/choice.php", "whichchoice=123&option=2&pwd=dvorak");

          /*
          // choice.php?whichchoice=123&option=2&pwd
          assertGetRequest(requests.get(i++), "/tiles.php", null);
          i = validateDvorakRequests(builder, i);
          */
        }
      }
    }

    // At Least It's Not Full Of Trash
    // Hit "auto" button
    // GET /choice.php?action=auto HTTP/1.1
    // choice.php?whichchoice=123&option=2 (Unlock Quest Puzzle)
    // temple.0.html
    // <a href=choice.php>Continue down the corridor...</a>
    // Now What?
    // *****
    // Encountered choice adventure with no choices.
    // <a href=main.php target=mainpane class=error>Click here to continue in the relay
    // browser.</a><br>
    // aborted automation?
    // choice.php?forceoption=0
    // Field: location = [tiles.php]
    // HTTP/1.1 302 Found
    // GET /tiles.php HTTP/1.1
    // tiles.php
    // dvorak.0.html
    //
    // Unhandled redirect to tiles.php
    // POST /KoLmafia/specialCommand?cmd=dvorak&pwd=3f79884939f76280e902d1baa0d4a14c HTTP/1.1
    // choice.php?whichchoice=125&option=3&pwd (in CHOICE_HANDLER)
    // api.php

  }
}
