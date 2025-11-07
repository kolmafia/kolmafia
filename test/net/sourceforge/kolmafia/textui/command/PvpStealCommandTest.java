package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAttacksLeft;
import static internal.helpers.Player.withHippyStoneBroken;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClient;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class PvpStealCommandTest extends AbstractCommandTestBase {

  public PvpStealCommandTest() {
    this.command = "pvp";
  }

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("testUser");

    // Figure out this for pvp?
    // new HeistCommandFakeRequest().register("heistFake");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  @Test
  void mustHaveNonEmptyCommand() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("Must specify both mission and stance"));
      assertErrorState();
    }
  }

  @Test
  void mustSpecifyMission() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      String output = execute("1");

      assertThat(output, containsString("Must specify both mission and stance"));
      assertErrorState();
    }
  }

  @Test
  void mustSpecifyMissionDespiteAttackingTougher() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      String output = execute("1 tougher");

      assertThat(output, containsString("Must specify both mission and stance"));
      assertErrorState();
    }
  }

  @Test
  void mustSpecifyStance() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      String output = execute("1 flowers");

      assertThat(output, containsString("Must specify both mission and stance"));
      assertErrorState();
    }
  }

  @Test
  void validatesMission() { // Can't attack for loot if you're in Ronin/HC
    var setup = setupClient();
    var client = setup.client;
    var cleanups = new Cleanups(setup.cleanups, withInteractivity(false));

    try (cleanups) {
      String output = execute("1 loot");

      assertThat(output, containsString("You cannot attack for loot now."));
      assertErrorState();
    }
  }

  @CartesianTest
  void attacksSuccessfully(
      @Values(strings = {"random", "tougher"}) final String tougher,
      @Values(strings = {"flowers", "fame", "lootwhatever"}) final String mission) {

    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      execute("1 " + tougher + " " + mission + " 1");

      assertRequest(client, mission, tougher);
    }
  }

  private record SetupClient(FakeHttpClient client, Cleanups cleanups) {}

  private SetupClient setupClient() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withHippyStoneBroken(),
            withAttacksLeft(1),
            withProperty("defaultFlowerWinMessage", "lucky!"),
            withProperty("defaultFlowerLossMessage", "oops."));

    addResponses(client);
    return new SetupClient(client, cleanups);
  }

  private void addResponses(FakeHttpClient client) {
    client.addResponse(200, html("request/test_pvp_fight_menu.html")); // checkStances
    client.addResponse(200, "");
  }

  private void assertRequest(FakeHttpClient client, String mission, String tougher) {
    var requests = client.getRequests();

    // For the ranked field, 1 = random opponent, 2 = random tougher opponent.
    String tougherNum;
    if (tougher.equals("tougher")) {
      tougherNum = "2";
    } else {
      tougherNum = "1";
    }

    assertPostRequest(
        requests.get(1),
        "/peevpee.php",
        "action=fight&place=fight&attacktype="
            + mission
            + "&winmessage=lucky!&losemessage=oops.&stance=1&ranked="
            + tougherNum);
    assertContinueState();
  }
}
