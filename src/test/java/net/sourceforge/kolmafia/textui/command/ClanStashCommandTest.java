package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getLastRequest;
import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInStash;
import static internal.helpers.Player.withMeat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.session.ClanManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ClanStashCommandTest extends AbstractCommandTestBase {

  public ClanStashCommandTest() {
    this.command = "stash";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
    // don't send a GET request to the server to get the stash
    ClanManager.setStashRetrieved();
  }

  @Test
  void mustMakeCommand() {
    execute("");

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Test
  void mustMakeValidCommand() {
    execute("foo bar");

    assertContinueState();
    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Nested
  class Put {
    @Test
    public void storesSealToothInStash() {
      try (var _ = withItem("seal tooth")) {
        execute("put 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          getLastRequest(), "/clan_stash.php", "action=addgoodies&ajax=1&item1=2&qty1=1");
    }

    @Test
    public void storesManyItemsInStash() {
      try (var _ = new Cleanups(withItem("seal tooth"), withItem("helmet turtle"))) {
        execute("put 1 seal tooth, 1 helmet turtle");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          getLastRequest(),
          "/clan_stash.php",
          "action=addgoodies&ajax=1&item1=2&qty1=1&item2=3&qty2=1");
    }

    @Test
    public void doesNotStoreItemsNotInInventory() {
      execute("put 1 seal tooth");

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void doesNotStoreZeroItemsInStash() {
      try (var _ = withItem("seal tooth")) {
        execute("put 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void storesMeatInStash() {
      try (var _ = withMeat(100)) {
        execute("put 100 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          getLastRequest(), "/clan_stash.php", "action=addgoodies&ajax=1&howmuch=100");
    }

    @Test
    public void storesMoreThanIntMaxMeatInStash() {
      try (var _ = withMeat(3_000_000_000L)) {
        execute("put 3000000000 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          getLastRequest(), "/clan_stash.php", "action=addgoodies&ajax=1&howmuch=3000000000");
    }

    @Test
    public void doesNotStoreZeroMeatInStash() {
      try (var _ = withMeat(100)) {
        execute("put 0 meat");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }

  @Nested
  class Take {
    @Test
    public void takesSealToothFromStash() {
      try (var _ = withItemInStash("seal tooth")) {
        execute("take 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          getLastRequest(), "/clan_stash.php", "action=takegoodies&ajax=1&whichitem=2&quantity=1");
    }

    @Test
    public void doesNotTakeZeroItemsFromStash() {
      try (var _ = withItemInStash("seal tooth")) {
        execute("take 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }
}
