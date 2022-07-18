package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import internal.helpers.HttpClientWrapper;
import internal.helpers.Player;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SkeletonCommandTest extends AbstractCommandTestBase {

  public SkeletonCommandTest() {
    this.command = "skeleton";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
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
    assertPostRequest(requests.get(0), "/inv_use.php", "which=3&whichitem=5881");
    assertPostRequest(requests.get(1), "/choice.php", "whichchoice=603&option=5");
  }
}
