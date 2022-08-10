package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ZapCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  public ZapCommandTest() {
    this.command = "zap";
  }

  @Test
  public void mustSpecifyItem() {
    String output = execute("");

    assertErrorState();
    assertThat(output, containsString("Zap what?"));
  }

  @Test
  public void cantZapUnmatchedItems() {
    execute("foo_bar");

    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Test
  public void cantZapItemsNotInInventory() {
    execute("bugbear beanie");

    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Test
  public void requireWandToZap() {
    var cleanups = withItem("bugbear beanie");

    try (cleanups) {
      execute("bugbear beanie");
    }

    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Test
  public void zapSimpleItem() {
    var cleanups = new Cleanups(withItem("hexagonal wand"), withItem("bugbear beanie"));

    try (cleanups) {
      execute("bugbear beanie");
    }

    var requests = getRequests();
    assertThat(requests.size(), equalTo(1));
    assertPostRequest(requests.get(0), "/wand.php", "action=zap&whichwand=1270&whichitem=169");
  }

  @Test
  public void zapNoItems() {
    var cleanups = new Cleanups(withItem("hexagonal wand"), withItem("bugbear beanie"));

    try (cleanups) {
      execute("0 bugbear beanie");
    }

    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Test
  public void zapManyItems() {
    var cleanups =
        new Cleanups(
            withItem("hexagonal wand"),
            withItem("bugbear beanie"),
            withItem("cursed swash buckle", 2));

    try (cleanups) {
      execute("1 bugbear beanie, 2 cursed swash buckle");
    }

    var requests = getRequests();
    assertThat(requests.size(), equalTo(3));
  }
}
