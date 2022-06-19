package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.fakeClientBuilder;
import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.addItem;
import static internal.helpers.Player.isSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.ZodiacSign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UntinkerCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    HttpClientWrapper.setupFakeClient();
  }

  public UntinkerCommandTest() {
    this.command = "untinker";
  }

  @Test
  void completesQuest() {
    var cleanups = new Cleanups(isSign(ZodiacSign.VOLE));
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
    var cleanups = new Cleanups(addItem("badass belt", 1));
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
    var cleanups = new Cleanups(addItem("badass belt", 5));
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
    var cleanups = new Cleanups(addItem("badass belt", 1));
    try (cleanups) {
      execute("0 badass belt");

      assertContinueState();
    }

    var requests = getRequests();
    assertThat(requests, empty());
  }

  private void setFakeResponse(String string) {
    fakeClientBuilder.client.setResponse(200, string);
  }
}
