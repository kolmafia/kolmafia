package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import internal.listeners.FakeListener;
import internal.network.FakeHttpClientBuilder;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ClosetCommandTest extends AbstractCommandTestBase {

  private final FakeHttpClientBuilder fakeClientBuilder = new FakeHttpClientBuilder();

  private List<HttpRequest> getRequests() {
    return fakeClientBuilder.client.getRequests();
  }

  public ClosetCommandTest() {
    this.command = "closet";
  }

  @BeforeEach
  public void initializeState() {
    GenericRequest.sessionId = "closet"; // do "send" requests
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    GenericRequest.resetClient();
    fakeClientBuilder.client.clear();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  void mustMakeValidCommand() {
    String output = execute("foobar");

    assertErrorState();
    assertThat(output, containsString("Invalid closet command."));
  }

  @Test
  void lessThanFourChars() {
    String output;
    var cleanups = Player.addItemToCloset("seal tooth");

    try (cleanups) {
      output = execute("ls");
    }

    assertContinueState();
    assertThat(output, notNullValue());
    assertThat(output, containsString("seal tooth"));
  }

  @Nested
  class Filter {
    @Test
    public void listsCloset() {
      String output;
      var cleanups = Player.addItemToCloset("seal tooth");

      try (cleanups) {
        output = execute("");
      }

      assertContinueState();
      assertThat(output, notNullValue());
      assertThat(output, containsString("seal tooth"));
    }

    @Test
    public void listsClosetWithFilter() {
      String output;
      var cleanups =
          new Cleanups(Player.addItemToCloset("seal tooth"), Player.addItemToCloset("disco mask"));

      try (cleanups) {
        output = execute("list seal");
      }

      assertContinueState();
      assertThat(output, notNullValue());
      assertThat(output, containsString("seal tooth"));
      assertThat(output, not(containsString("disco mask")));
    }
  }

  @Nested
  class Empty {
    @Test
    public void emptiesCloset() {
      var cleanups = Player.addItemToCloset("seal tooth");

      try (cleanups) {
        execute("empty");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = requests.get(0);
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/closet.php"));
      assertThat(request.method(), equalTo("POST"));
    }
  }

  @Nested
  class Put {
    @Test
    public void storesSealToothInCloset() {
      var cleanups = Player.addItem("seal tooth");

      try (cleanups) {
        execute("put 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = requests.get(0);
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/inventory.php"));
      assertThat(uri.getQuery(), equalTo("action=closetpush&ajax=1&whichitem=2&qty=1"));
    }

    @Test
    public void doesNotStoreItemsNotInInventory() {
      execute("put 1 seal tooth");

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void doesNotStoreZeroItemsInCloset() {
      var cleanups = Player.addItem("seal tooth");

      try (cleanups) {
        execute("put 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void storesMeatInCloset() {
      var cleanups = Player.setMeat(100);

      try (cleanups) {
        execute("put 100 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = requests.get(0);
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/closet.php"));
      assertThat(request.method(), equalTo("POST"));
    }

    @Test
    public void doesNotStoreZeroMeatInCloset() {
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
    public void takesSealToothFromCloset() {
      var cleanups = Player.addItemToCloset("seal tooth");

      try (cleanups) {
        execute("take 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = requests.get(0);
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/inventory.php"));
      assertThat(uri.getQuery(), equalTo("action=closetpull&ajax=1&whichitem=2&qty=1"));
    }

    @Test
    public void doesNotTakeZeroItemsFromCloset() {
      var cleanups = Player.addItemToCloset("seal tooth");

      try (cleanups) {
        execute("take 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void takesMeatFromCloset() {
      var cleanups = Player.setClosetMeat(100);

      try (cleanups) {
        execute("take 100 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      var request = requests.get(0);
      var uri = request.uri();
      assertThat(uri.getPath(), equalTo("/closet.php"));
      assertThat(request.method(), equalTo("POST"));
    }

    @Test
    public void doesNotTakeZeroMeatFromCloset() {
      var cleanups = Player.setClosetMeat(100);

      try (cleanups) {
        execute("take 0 meat");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }

  @Test
  public void firesHatListenerIfItemIsHat() {
    var cleanups = Player.addItem("disco mask");
    var listener = new FakeListener();
    PreferenceListenerRegistry.registerPreferenceListener("(hats)", listener);

    try (cleanups) {
      execute("put 1 disco mask");
    }

    assertThat(listener.getUpdateCount(), equalTo(1));
  }
}
