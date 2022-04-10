package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Player;
import internal.network.FakeHttpClientBuilder;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
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
  public void doesNotStoreZeroItemsInCloset() {
    var cleanups = Player.addItem("seal tooth");

    try (cleanups) {
      execute("put 0 seal tooth");
    }

    var requests = getRequests();

    assertThat(requests, empty());
  }

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
}
