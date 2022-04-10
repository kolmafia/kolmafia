package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Player;
import internal.network.FakeHttpClientBuilder;
import internal.network.RequestBodyReader;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendMessageCommandTest extends AbstractCommandTestBase {

  public SendMessageCommandTest() {
    this.command = "csend";
  }

  private final FakeHttpClientBuilder fakeClientBuilder = new FakeHttpClientBuilder();

  private List<HttpRequest> getRequests() {
    return fakeClientBuilder.client.getRequests();
  }

  @BeforeEach
  public void initializeState() {
    GenericRequest.sessionId = "csend";
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    GenericRequest.resetClient();
    fakeClientBuilder.client.clear();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
  }

  @Test
  public void itShouldSendMeatWithOutCommas() {
    String output;
    var cleanups = Player.setMeat(1000000);
    try (cleanups) {
      output = execute(" 1000000 meat to buffy");
    }
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = requests.get(0);
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    assertThat(request.method(), equalTo("POST"));
    var body = new RequestBodyReader().bodyAsString(request);
    assertThat(
        body,
        equalTo(
            "action=send&towho=buffy&message=Keep+the+contents+of+this+message+top-sekrit%2C+ultra+hush-hush.&sendmeat=1000000"));
  }
}
