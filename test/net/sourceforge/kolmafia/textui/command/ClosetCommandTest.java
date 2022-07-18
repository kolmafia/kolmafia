package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.helpers.Player;
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
    var cleanups = Player.addItemToCloset("seal tooth");

    try (cleanups) {
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
      var cleanups = Player.addItemToCloset("seal tooth");

      try (cleanups) {
        output = execute("");
      }

      assertContinueState();
      assertThat(output, notNullValue());
      assertThat(output, containsString("seal tooth"));
    }

    @Test
    public void listsClosetWithFilter() {
      String output;
      var cleanups =
          new Cleanups(Player.addItemToCloset("seal tooth"), Player.addItemToCloset("disco mask"));

      try (cleanups) {
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
      var cleanups = Player.addItemToCloset("seal tooth");

      try (cleanups) {
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
      var cleanups = Player.addItem("seal tooth");

      try (cleanups) {
        execute("put 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertGetRequest(
          requests.get(0), "/inventory.php", "action=closetpush&ajax=1&whichitem=2&qty=1");
    }

    @Test
    public void storesManyItemsInCloset() {
      var cleanups = new Cleanups(Player.addItem("seal tooth"), Player.addItem("helmet turtle"));

      try (cleanups) {
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
      var cleanups = Player.addItem("seal tooth");

      try (cleanups) {
        execute("put 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void storesMeatInCloset() {
      var cleanups = Player.setMeat(100);

      try (cleanups) {
        execute("put 100 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0), "/closet.php", "action=addtakeclosetmeat&addtake=add&quantity=100");
    }

    @Test
    public void storesMoreThanIntMaxMeatInCloset() {
      var cleanups = Player.setMeat(3_000_000_000L);

      try (cleanups) {
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
      var cleanups = Player.setMeat(100);

      try (cleanups) {
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
      var cleanups = Player.addItemToCloset("seal tooth");

      try (cleanups) {
        execute("take 1 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertGetRequest(
          requests.get(0), "/inventory.php", "action=closetpull&ajax=1&whichitem=2&qty=1");
    }

    @Test
    public void doesNotTakeZeroItemsFromCloset() {
      var cleanups = Player.addItemToCloset("seal tooth");

      try (cleanups) {
        execute("take 0 seal tooth");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }

    @Test
    public void takesMeatFromCloset() {
      var cleanups = Player.setClosetMeat(100);

      try (cleanups) {
        execute("take 100 meat");
      }

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0), "/closet.php", "action=addtakeclosetmeat&addtake=take&quantity=100");
    }

    @Test
    public void doesNotTakeZeroMeatFromCloset() {
      var cleanups = Player.setClosetMeat(100);

      try (cleanups) {
        execute("take 0 meat");
      }

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }

  @Test
  public void firesHatListenerIfItemIsHat() {
    var cleanups = Player.addItem("disco mask");
    var listener = new FakeListener();
    PreferenceListenerRegistry.registerPreferenceListener("(hats)", listener);

    try (cleanups) {
      execute("put 1 disco mask");
    }

    assertThat(listener.getUpdateCount(), equalTo(1));
  }
}
