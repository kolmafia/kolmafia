package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;

import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendMessageCommandTest extends AbstractCommandTestBase {

  public SendMessageCommandTest() {
    this.command = "csend";
  }

  @BeforeEach
  public void initializeState() {
    KoLCharacter.reset("csender");
    KoLCharacter.reset(true);
    GenericRequest.sessionId = "csend";
  }

  @Test
  public void itShouldParseMeatWithCommas() {
    var fakeClientBuilder = new FakeHttpClientBuilder();
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    String output = execute(" 1,000,000 meat to buffy");
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var fakeClient = fakeClientBuilder.client;
    var request = fakeClient.request;
    assertNull(request);
  }

  @Test
  public void itShouldParseMeatWithoutCommas() {
    var fakeClientBuilder = new FakeHttpClientBuilder();
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    String output = execute(" 1000000 meat to buffy");
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
    var fakeClient = fakeClientBuilder.client;
    var request = fakeClient.request;
    assertThat(request, notNullValue());
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/sendmessage.php"));
    // assertThat(uri.getQuery(), equalTo("action=closetpush&ajax=1&whichitem=2&qty=1"));
  }
}
