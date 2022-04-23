package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import internal.helpers.Player;
import internal.network.FakeHttpClientBuilder;
import internal.network.RequestBodyReader;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SkeletonCommandTest extends AbstractCommandTestBase {

  public SkeletonCommandTest() {
    this.command = "skeleton";
  }

  private final FakeHttpClientBuilder fakeClientBuilder = new FakeHttpClientBuilder();

  private List<HttpRequest> getRequests() {
    return fakeClientBuilder.client.getRequests();
  }

  @BeforeEach
  public void initializeState() {
    GenericRequest.sessionId = "skeleton";
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    GenericRequest.resetClient();
    fakeClientBuilder.client.clear();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
  }

  @Test
  void providesUsageIfNoParameters() {
    String output = execute("");

    assertThat(output, containsString("Usage: skeleton warrior | cleric | wizard | rogue | buddy"));
  }

  @Test
  void questionsIfInvalidSkeleton() {
    String output = execute("drunk");

    assertErrorState();
    assertThat(output, containsString("I don't understand what a 'drunk' skeleton is"));
  }

  @Test
  void errorsIfNoSkeletons() {
    String output = execute("buddy");

    assertErrorState();
    assertThat(output, containsString("You have no skeletons"));
  }

  @Test
  void sendsRequestsIfSkeleton() {
    var cleanups = Player.addItem(ItemPool.SKELETON);

    try (cleanups) {
      execute("buddy");
    }

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, not(empty()));
    assertThat(requests, hasSize(2));
    var first = requests.get(0);
    var uri = first.uri();
    assertThat(uri.getPath(), equalTo("/inv_use.php"));
    var body = new RequestBodyReader().bodyAsString(first);
    assertThat(body, equalTo("which=3&whichitem=5881"));

    var second = requests.get(1);
    uri = second.uri();
    assertThat(uri.getPath(), equalTo("/choice.php"));
    body = new RequestBodyReader().bodyAsString(second);
    assertThat(body, equalTo("whichchoice=603&option=5"));
  }
}
