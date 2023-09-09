package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.fakeClientBuilder;
import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UntinkerCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    HttpClientWrapper.setupFakeClient();
  }

  public UntinkerCommandTest() {
    this.command = "untinker";
  }

  @Test
  void completesQuest() {
    var cleanups = new Cleanups(withSign(ZodiacSign.VOLE));
    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("Accepting quest to find the Untinker's screwdriver"));
      assertContinueState();
    }

    var requests = getRequests();
    assertThat(requests.size(), equalTo(2));

    assertPostRequest(
        requests.get(0),
        "/place.php",
        "whichplace=forestvillage&action=fv_untinker_quest&preaction=screwquest");
    assertPostRequest(requests.get(1), "/place.php", "whichplace=knoll_friendly&action=dk_innabox");
  }

  @Test
  void canUntinkerItems() {
    var cleanups = new Cleanups(withItem("badass belt", 1));
    setFakeResponse("You acquire <b>skull of the Bonerdagon</b>");
    try (cleanups) {
      String output = execute("badass belt");

      assertThat(output, containsString("Untinkering badass belt"));
      assertContinueState();
    }

    var requests = getRequests();
    assertThat(requests, not(empty()));
    assertPostRequest(
        requests.get(0),
        "/place.php",
        "whichplace=forestvillage&action=fv_untinker&preaction=untinker&whichitem=677&untinkerall=on");
  }

  @Test
  void canUntinkerManyItems() {
    var cleanups = new Cleanups(withItem("badass belt", 5));
    setFakeResponse("You acquire <b>skull of the Bonerdagon</b>");
    try (cleanups) {
      String output = execute("4 badass belt");

      assertThat(output, containsString("Untinkering badass belt (4)"));
      assertContinueState();
    }

    var requests = getRequests();
    assertThat(requests.size(), equalTo(4));
    assertPostRequest(
        requests.get(0),
        "/place.php",
        "whichplace=forestvillage&action=fv_untinker&preaction=untinker&whichitem=677");
  }

  @Test
  void noRequestForUntinkerNoItems() {
    var cleanups = new Cleanups(withItem("badass belt", 1));
    try (cleanups) {
      execute("0 badass belt");

      assertContinueState();
    }

    var requests = getRequests();
    assertThat(requests, empty());
  }

  @Test
  void usesLoathingLegionScrewdriverIfAccessible() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER, 1),
            withItem("badass belt", 3));

    try (cleanups) {
      setFakeResponse("You acquire <b>skull of the Bonerdagon</b>");
      try (cleanups) {
        String output = execute("2 badass belt");

        assertThat(output, containsString("Unscrewing badass belt"));
        assertContinueState();
      }

      var requests = getRequests();
      assertThat(requests, hasSize(2));
      assertGetRequest(
          requests.get(0),
          "/inv_use.php",
          "pwd=&ajax=1&whichitem="
              + ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER
              + "&action=screw&dowhichitem="
              + ItemPool.BADASS_BELT);
    }
  }

  @Test
  void shortcutUntinkeringAllUsignScrewdriver() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER, 1),
            withItem("badass belt", 3));

    try (cleanups) {
      setFakeResponse("You acquire <b>skull of the Bonerdagon</b>");
      try (cleanups) {
        String output = execute("* badass belt");

        assertThat(output, containsString("Unscrewing badass belt"));
        assertContinueState();
      }

      var requests = getRequests();
      assertThat(requests, hasSize(1));
      assertGetRequest(
          requests.get(0),
          "/inv_use.php",
          "pwd=&ajax=1&whichitem="
              + ItemPool.LOATHING_LEGION_UNIVERSAL_SCREWDRIVER
              + "&action=screw&dowhichitem="
              + ItemPool.BADASS_BELT
              + "&untinkerall=on");
    }
  }

  private void setFakeResponse(String string) {
    fakeClientBuilder.client.addResponse(200, string);
  }
}
