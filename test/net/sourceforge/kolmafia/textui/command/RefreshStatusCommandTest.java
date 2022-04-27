package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import internal.network.FakeHttpClientBuilder;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RefreshStatusCommandTest extends AbstractCommandTestBase {

  private final FakeHttpClientBuilder fakeClientBuilder = new FakeHttpClientBuilder();

  private List<HttpRequest> getRequests() {
    return fakeClientBuilder.client.getRequests();
  }

  private HttpRequest getLastRequest() {
    return fakeClientBuilder.client.getLastRequest();
  }

  public RefreshStatusCommandTest() {
    this.command = "refresh";
  }

  @BeforeEach
  public void initializeState() {
    GenericRequest.sessionId = "refresh"; // do "send" requests
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    GenericRequest.resetClient();
    fakeClientBuilder.client.clear();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  public void shouldError() {
    String output = execute("foobar");

    var requests = getRequests();
    assertThat(requests, empty());
    assertErrorState();
    assertThat(output, containsString("foobar cannot be refreshed"));
  }

  @Test
  public void shouldRefreshStash() {
    execute("stash");

    var requests = getRequests();
    assertThat(requests, not(empty()));
    var request = getLastRequest();
    var uri = request.uri();
    assertThat(uri.getPath(), equalTo("/clan_stash.php"));
  }
}
