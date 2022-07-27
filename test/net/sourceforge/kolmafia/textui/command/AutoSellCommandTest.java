package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMeat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AutoSellCommandTest extends AbstractCommandTestBase {

  public AutoSellCommandTest() {
    this.command = "autosell";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  public void sellsSealTooth() {
    var cleanups = withItem("seal tooth", 5);

    try (cleanups) {
      execute("1 seal tooth");
    }

    var requests = getRequests();

    assertThat(requests, not(empty()));
    assertPostRequest(
        requests.get(0), "/sellstuff.php", "action=sell&ajax=1&type=quant&howmany=1&whichitem[]=2");
  }

  @Test
  public void sellsManyItems() {
    var cleanups = new Cleanups(withItem("seal tooth"), withItem("helmet turtle"));

    try (cleanups) {
      execute("1 seal tooth, 1 helmet turtle");
    }

    var requests = getRequests();

    assertThat(requests, not(empty()));
    assertPostRequest(
        requests.get(0),
        "/sellstuff.php",
        "action=sell&ajax=1&type=all&howmany=1&whichitem[]=2&whichitem[]=3");
  }

  @Test
  public void doesNotSellAbsentItems() {
    execute("1 seal tooth");

    var requests = getRequests();

    assertThat(requests, empty());
  }

  @Test
  public void doesNotSellZeroItems() {
    var cleanups = withItem("seal tooth");

    try (cleanups) {
      execute("0 seal tooth");
    }

    var requests = getRequests();

    assertThat(requests, empty());
  }

  @Test
  public void doesNotSellMeat() {
    var cleanups = withMeat(100);

    try (cleanups) {
      execute("50 meat");
    }

    var requests = getRequests();

    assertThat(requests, empty());
  }

  @Test
  public void sellsAllButOneItem() {
    var cleanups = withItem("seal tooth", 5);

    try (cleanups) {
      execute("-1 seal tooth");
    }

    var requests = getRequests();

    assertThat(requests, not(empty()));
    assertPostRequest(
        requests.get(0),
        "/sellstuff.php",
        "action=sell&ajax=1&type=allbutone&howmany=1&whichitem[]=2");
  }
}
