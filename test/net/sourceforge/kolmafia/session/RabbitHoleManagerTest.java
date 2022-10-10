package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withPasswordHash;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
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
    public void afterEach() {}

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
      assertGetRequest(requests.get(i++), "/choice.php", "pwd&whichchoice=442&option=5");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=5,6&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=2,3&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=4,2&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=5,2&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=4,1&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=6,1&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=2,5&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=4,6&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=6,5&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=1,5&pwd=chess");
      assertPostRequest(
          requests.get(i++), "/choice.php", "whichchoice=443&option=1&xy=0,6&pwd=chess");
      assertPostRequest(
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
              // Avoid looking at your vinyl boots
              withGender(KoLCharacter.FEMALE));
      try (cleanups) {
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
        url = "choice.php?pwd&whichchoice=442&option=5";
        request = new RelayRequest(false);
        request.constructURLString(url, false);
        request.run();

        // You have selected The Great Big Chessboard
        // Simulate user typing "solve" button
        url = "/KoLmafia/specialCommand?cmd=chess+solve&pwd=chess";
        request = new RelayRequest(false);
        request.constructURLString(url);
        request.run();

        // Wait until the submitted command is done
        request.waitForCommandCompletion();
        // RelayRequest tells browser to redirect to see the map

        // Verify that expected requests were submitted
        var requests = client.getRequests();
        assertThat(requests, hasSize(15));
        int i = 0;
        assertGetRequest(requests.get(i++), "/choice.php", "forceoption=0");
        i = validateChessPuzzleRequests(builder, i);
        assertPostRequest(requests.get(i++), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
