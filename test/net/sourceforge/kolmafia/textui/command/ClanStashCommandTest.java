package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import internal.network.FakeHttpClientBuilder;
import internal.network.RequestBodyReader;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ClanStashCommandTest extends AbstractCommandTestBase {

  private final FakeHttpClientBuilder fakeClientBuilder = new FakeHttpClientBuilder();

  private List<HttpRequest> getRequests() {
    return fakeClientBuilder.client.getRequests();
  }

  private HttpRequest getLastRequest() {
    return fakeClientBuilder.client.getLastRequest();
  }

  public ClanStashCommandTest() {
    this.command = "stash";
  }

  @BeforeEach
  public void initializeState() {
    GenericRequest.sessionId = "stash"; // do "send" requests
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    GenericRequest.resetClient();
    fakeClientBuilder.client.clear();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
    // don't send a GET request to the server to get the stash
    ClanManager.setStashRetrieved();
  }

  @Test
  void mustMakeCommand() {
    execute("");

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Test
  void mustMakeValidCommand() {
    execute("foo bar");

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Nested
  class Put {
    @Test
    public void storesSealToothInStash() {
      var cleanups = Player.addItem("seal tooth");

      try (cleanups) {
        execute("put 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = getLastRequest();
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/clan_stash.php"));
      assertThat(request.method(), equalTo("POST"));
      var body = new RequestBodyReader().bodyAsString(request);
      assertThat(body, equalTo("action=addgoodies&ajax=1&item1=2&qty1=1"));
    }

    @Test
    public void storesManyItemsInStash() {
      var cleanups = new Cleanups(Player.addItem("seal tooth"), Player.addItem("helmet turtle"));

      try (cleanups) {
        execute("put 1 seal tooth, 1 helmet turtle");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = getLastRequest();
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/clan_stash.php"));
      assertThat(request.method(), equalTo("POST"));
      var body = new RequestBodyReader().bodyAsString(request);
      assertThat(body, equalTo("action=addgoodies&ajax=1&item1=2&qty1=1&item2=3&qty2=1"));
    }

    @Test
    public void doesNotStoreItemsNotInInventory() {
      execute("put 1 seal tooth");

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void doesNotStoreZeroItemsInStash() {
      var cleanups = Player.addItem("seal tooth");

      try (cleanups) {
        execute("put 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void storesMeatInStash() {
      var cleanups = Player.setMeat(100);

      try (cleanups) {
        execute("put 100 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = getLastRequest();
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/clan_stash.php"));
      assertThat(request.method(), equalTo("POST"));
      var body = new RequestBodyReader().bodyAsString(request);
      assertThat(body, equalTo("action=addgoodies&ajax=1&howmuch=100"));
    }

    @Test
    public void storesMoreThanIntMaxMeatInStash() {
      var cleanups = Player.setMeat(3_000_000_000L);

      try (cleanups) {
        execute("put 3000000000 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = getLastRequest();
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/clan_stash.php"));
      assertThat(request.method(), equalTo("POST"));
      var body = new RequestBodyReader().bodyAsString(request);
      assertThat(body, equalTo("action=addgoodies&ajax=1&howmuch=3000000000"));
    }

    @Test
    public void doesNotStoreZeroMeatInStash() {
      var cleanups = Player.setMeat(100);

      try (cleanups) {
        execute("put 0 meat");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }

  @Nested
  class Take {
    @Test
    public void takesSealToothFromStash() {
      var cleanups = Player.addItemToStash("seal tooth");

      try (cleanups) {
        execute("take 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = getLastRequest();
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/clan_stash.php"));
      assertThat(request.method(), equalTo("POST"));
      var body = new RequestBodyReader().bodyAsString(request);
      assertThat(body, equalTo("action=takegoodies&ajax=1&whichitem=2&quantity=1"));
    }

    @Test
    public void doesNotTakeZeroItemsFromStash() {
      var cleanups = Player.addItemToStash("seal tooth");

      try (cleanups) {
        execute("take 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }
}
