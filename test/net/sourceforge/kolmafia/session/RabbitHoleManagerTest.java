package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.RelayRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class RabbitHoleManagerTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("rabbit hole");
    RabbitHoleManager.reset();
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("rabbit hole");
  }

  @Nested
  class Chess {
    @AfterEach
    public void afterEach() {
      InventoryManager.resetInventory();
      RelayRequest.reset();
    }

    public void addChessPuzzleResponses(FakeHttpClientBuilder builder) {
      var client = builder.client;
      client.addResponse(200, html("request/test_chess_00.html"));
      client.addResponse(200, html("request/test_chess_01.html"));
      client.addResponse(200, html("request/test_chess_02.html"));
      client.addResponse(200, html("request/test_chess_03.html"));
      client.addResponse(200, html("request/test_chess_04.html"));
      client.addResponse(200, html("request/test_chess_05.html"));
      client.addResponse(200, html("request/test_chess_06.html"));
      client.addResponse(200, html("request/test_chess_07.html"));
      client.addResponse(200, html("request/test_chess_08.html"));
      client.addResponse(200, html("request/test_chess_09.html"));
      client.addResponse(200, html("request/test_chess_10.html"));
      client.addResponse(200, html("request/test_chess_11.html"));
      client.addResponse(200, html("request/test_chess_12.html"));
    }

    public int validateChessPuzzleRequests(FakeHttpClientBuilder builder, int i) {
      var client = builder.client;
      var requests = client.getRequests();
      assertPostRequest(requests.get(i++), "/choice.php", "whichchoice=442&option=5&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=5,6&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=2,3&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=4,2&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=5,2&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=4,1&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=6,1&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=2,5&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=4,6&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=6,5&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=1,5&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=0,6&pwd=chess");
      assertGetRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=6,0&pwd=chess");
      return i;
    }

    @Test
    public void canAutomateChessPuzzleFromRelayBrowser() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("chess"),
              // Avoid health warning
              withHP(100, 100, 100),
              // Avoid looking at your vinyl boots
              withGender(KoLCharacter.FEMALE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_rabbithole_reflection.html"));
        addChessPuzzleResponses(builder);
        client.addResponse(200, ""); // api.php

        // Use a reflection of a map
        var url = "inv_use.php?which=3&whichitem=4509&pwd=chess";
        var request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // Continue
        url = "choice.php?forceoption=0";
        request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        // You are now have many options.
        url = "choice.php?whichchoice=442&option=5&pwd=chess";
        request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // You have selected The Great Big Chessboard
        // Simulate user typing "solve" button
        url = "/KoLmafia/specialCommand?cmd=chess+solve&pwd=chess";
        request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // Wait until the submitted command is done
        request.waitForCommandCompletion();

        // We beat the puzzle and got a queen cookie!
        assertTrue(InventoryManager.hasItem(ItemPool.QUEEN_COOKIE));

        // Verify that expected requests were submitted
        var requests = client.getRequests();
        assertThat(requests, hasSize(16));
        int i = 0;
        assertPostRequest(requests.get(i++), "/inv_use.php", "which=3&whichitem=4509&pwd=chess");
        assertGetRequest(requests.get(i++), "/choice.php", "forceoption=0");
        i = validateChessPuzzleRequests(builder, i);
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canStepThroughChessPuzzleFromRelayBrowser() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("chess"),
              // Avoid health warning
              withHP(100, 100, 100),
              // Avoid looking at your vinyl boots
              withGender(KoLCharacter.FEMALE),
              withHandlingChoice(false));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_rabbithole_reflection.html"));
        addChessPuzzleResponses(builder);
        client.addResponse(200, ""); // api.php

        // Use a reflection of a map
        var url = "inv_use.php?which=3&whichitem=4509&pwd=chess";
        var request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // Continue
        url = "choice.php?forceoption=0";
        request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        // You are now have many options.
        url = "choice.php?whichchoice=442&option=5&pwd=chess";
        request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // You have selected The Great Big Chessboard
        // RabbitHoleManager parsed it and saved in in a Board
        assertTrue(ChoiceManager.handlingChoice);

        // Simulate user repeatedly typing "step" button
        int count = 100;
        while (ChoiceManager.handlingChoice && count-- > 0) {
          url = "/KoLmafia/redirectedCommand?cmd=chess+step&pwd=chess";
          request = new RelayRequest(false);
          request.constructURLString(url);
          request.run();

          // Wait until the submitted command is done
          request.waitForCommandCompletion();

          // For redirected command, the browser follows the redirect
          url = RelayRequest.redirectedCommandURL;
          request = new RelayRequest(false);
          request.constructURLString(url, false);
          request.run();
        }

        // We beat the puzzle and got a queen cookie!
        assertTrue(InventoryManager.hasItem(ItemPool.QUEEN_COOKIE));

        // Verify that expected requests were submitted
        var requests = client.getRequests();
        assertThat(requests, hasSize(15));
        int i = 0;
        assertPostRequest(requests.get(i++), "/inv_use.php", "which=3&whichitem=4509&pwd=chess");
        assertGetRequest(requests.get(i++), "/choice.php", "forceoption=0");
        i = validateChessPuzzleRequests(builder, i);
      }
    }
  }
}
