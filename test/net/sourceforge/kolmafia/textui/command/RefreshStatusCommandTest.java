package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getLastRequest;
import static internal.helpers.HttpClientWrapper.getRequests;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RefreshStatusCommandTest extends AbstractCommandTestBase {

  public RefreshStatusCommandTest() {
    this.command = "refresh";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
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
