package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Player;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClosetCommandTest extends AbstractCommandTestBase {

  public ClosetCommandTest() {
    this.command = "closet";
  }

  @BeforeEach
  public void initializeState() {
    KoLCharacter.reset("closet");
    KoLCharacter.reset(true);
    GenericRequest.sessionId = "closet"; // do "send" requests
  }

  @Test
  public void storesSealToothInCloset() {
    // setup fake client
    var fakeClientBuilder = new FakeHttpClientBuilder();
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);

    Player.addItem("seal tooth");

    execute("put 1 seal tooth");

    var fakeClient = fakeClientBuilder.client;
    var request = fakeClient.request;

    assertThat(request, notNullValue());
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/inventory.php"));
    assertThat(uri.getQuery(), equalTo("action=closetpush&ajax=1&whichitem=2&qty=1"));
  }

  @Test
  public void doesNotStoreZeroItemsInCloset() {
    // setup fake client
    var fakeClientBuilder = new FakeHttpClientBuilder();
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);

    execute("put 0 seal tooth");

    var fakeClient = fakeClientBuilder.client;
    var request = fakeClient.request;

    assertThat(request, nullValue());
  }

}
