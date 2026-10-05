package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withMeatInCloset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.listeners.FakeListener;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ClosetCommandTest extends AbstractCommandTestBase {

  public ClosetCommandTest() {
    this.command = "closet";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  void mustMakeValidCommand() {
    String output = execute("foobar");

    assertErrorState();
    assertThat(output, containsString("Invalid closet command."));
  }

  @Test
  void lessThanFourChars() {
    String output;
    try (var _ = withItemInCloset("seal tooth")) {
      output = execute("ls");
    }

    assertContinueState();
    assertThat(output, notNullValue());
    assertThat(output, containsString("seal tooth"));
  }

  @Nested
  class Filter {
    @Test
    public void listsCloset() {
      String output;
      try (var _ = withItemInCloset("seal tooth")) {
        output = execute("");
      }

      assertContinueState();
      assertThat(output, notNullValue());
      assertThat(output, containsString("seal tooth"));
    }

    @Test
    public void listsClosetWithFilter() {
      String output;
      try (var _ = new Cleanups(withItemInCloset("seal tooth"), withItemInCloset("disco mask"))) {
        output = execute("list seal");
      }

      assertContinueState();
      assertThat(output, notNullValue());
      assertThat(output, containsString("seal tooth"));
      assertThat(output, not(containsString("disco mask")));
    }
  }

  @Nested
  class Empty {
    @Test
    public void emptiesCloset() {
      try (var _ = withItemInCloset("seal tooth")) {
        execute("empty");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(requests.get(0), "/closet.php", "action=pullallcloset");
    }
  }

  @Nested
  class Put {
    @Test
    public void storesSealToothInCloset() {
      try (var _ = withItem("seal tooth")) {
        execute("put 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertGetRequest(
          requests.get(0), "/inventory.php", "action=closetpush&ajax=1&whichitem=2&qty=1");
    }

    @Test
    public void storesManyItemsInCloset() {
      try (var _ = new Cleanups(withItem("seal tooth"), withItem("helmet turtle"))) {
        execute("put 1 seal tooth, 1 helmet turtle");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertThat(requests.size(), equalTo(2));
      assertGetRequest(
          requests.get(0), "/inventory.php", "action=closetpush&ajax=1&whichitem=2&qty=1");
      assertGetRequest(
          requests.get(1), "/inventory.php", "action=closetpush&ajax=1&whichitem=3&qty=1");
    }

    @Test
    public void doesNotStoreItemsNotInInventory() {
      execute("put 1 seal tooth");

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void doesNotStoreZeroItemsInCloset() {
      try (var _ = withItem("seal tooth")) {
        execute("put 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void storesMeatInCloset() {
      try (var _ = withMeat(100)) {
        execute("put 100 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0), "/closet.php", "action=addtakeclosetmeat&addtake=add&quantity=100");
    }

    @Test
    public void storesMoreThanIntMaxMeatInCloset() {
      try (var _ = withMeat(3_000_000_000L)) {
        execute("put 3000000000 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0),
          "/closet.php",
          "action=addtakeclosetmeat&addtake=add&quantity=3000000000");
    }

    @Test
    public void doesNotStoreZeroMeatInCloset() {
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
    public void takesSealToothFromCloset() {
      try (var _ = withItemInCloset("seal tooth")) {
        execute("take 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertGetRequest(
          requests.get(0), "/inventory.php", "action=closetpull&ajax=1&whichitem=2&qty=1");
    }

    @Test
    public void doesNotTakeZeroItemsFromCloset() {
      try (var _ = withItemInCloset("seal tooth")) {
        execute("take 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void takesMeatFromCloset() {
      try (var _ = withMeatInCloset(100)) {
        execute("take 100 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0), "/closet.php", "action=addtakeclosetmeat&addtake=take&quantity=100");
    }

    @Test
    public void doesNotTakeZeroMeatFromCloset() {
      try (var _ = withMeatInCloset(100)) {
        execute("take 0 meat");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }

  @Test
  public void firesHatListenerIfItemIsHat() {
    var cleanups = withItem("disco mask");
    var listener = new FakeListener();
    PreferenceListenerRegistry.registerPreferenceListener("(hats)", listener);

    try (cleanups) {
      execute("put 1 disco mask");
    }

    assertThat(listener.getUpdateCount(), equalTo(1));
  }
}
