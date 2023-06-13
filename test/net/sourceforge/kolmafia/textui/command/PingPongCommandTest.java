package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PingPongCommandTest extends AbstractCommandTestBase {

  public PingPongCommandTest() {
    this.command = "pingpong";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("PingPongCommandTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("PingPongCommandTest");
  }

  @Test
  public void noTargetIsError() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(
            withProperty("_pingPongGame", false),
            withItem(ItemPool.PING_PONG_TABLE),
            withContinuationState());

    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("Play ping-pong with whom?"));
      assertErrorState();

      var requests = getRequests();
      assertThat(requests, hasSize(0));
    }
  }

  @Test
  public void noPingPongingTableIsError() {
    HttpClientWrapper.setupFakeClient();
    var cleanups = new Cleanups(withProperty("_pingPongGame", false));

    try (cleanups) {
      String output = execute("Veracity");
      assertThat(output, containsString("You need 1 more portable ping-pong table to continue."));
      assertErrorState();
      var requests = getRequests();
      assertThat(requests, hasSize(0));
    }
  }

  @Test
  public void alreadyPlayedTodayIsError() {
    HttpClientWrapper.setupFakeClient();
    var cleanups =
        new Cleanups(withProperty("_pingPongGame", true), withItem(ItemPool.PING_PONG_TABLE));

    try (cleanups) {
      String output = execute("Veracity");
      assertThat(output, containsString("You've already played ping-pong today."));
      assertErrorState();

      var requests = getRequests();
      assertThat(requests, hasSize(0));
    }
  }

  @Test
  public void canDetectTargetInHardcoreOrRonin() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_pingPongGame", false),
            withItem(ItemPool.PING_PONG_TABLE));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_ping_pong_table_ronin.html"));
      String output = execute("Hairy");
      assertThat(output, containsString("Can't use the item on that player at the moment."));
      assertErrorState();

      var requests = client.getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(
          requests.get(0), "/curse.php", "action=use&whichitem=11059&targetplayer=Hairy");
    }
  }

  @Test
  public void canDetectWinningGame() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_pingPongGame", false),
            withItem(ItemPool.PING_PONG_TABLE));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_ping_pong_table_prowess.html"));
      String output = execute("Blippy Bloppy");
      assertThat(output, containsString("You won the ping-pong game."));
      assertContinueState();

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0), "/curse.php", "action=use&whichitem=11059&targetplayer=Blippy Bloppy");
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
    }
  }

  @Test
  public void canDetectLosingGame() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_pingPongGame", false),
            withItem(ItemPool.PING_PONG_TABLE));
    try (cleanups) {
      client.addResponse(200, html("request/test_use_ping_pong_table_persistence.html"));
      String output = execute("Veracity");
      assertThat(output, containsString("You lost the ping-pong game."));
      assertContinueState();

      var requests = client.getRequests();
      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0), "/curse.php", "action=use&whichitem=11059&targetplayer=Veracity");
      assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
    }
  }
}
