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
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.PvpManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class PvpStealCommandTest extends AbstractCommandTestBase {

  public PvpStealCommandTest() {
    this.command = "pvp";
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("testUser");
  }

  @BeforeEach
  protected void beforeEach() {
    Preferences.reset("testUser");
    PvpManager.stancesKnown = false;
    PvpManager.canonicalStances = null;
  }

  @Test
  void mustReturnMissionsOnEmpty() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("7: Installation Wizard"));
      assertContinueState();
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
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withHippyStoneBroken(),
            withAttacksLeft(1),
            withInteractivity(false));

    try (cleanups) {
      addResponses(client);

      String output = execute("1 loot 1");

      assertThat(output, containsString("You cannot attack for loot now."));
      assertContinueState();
    }
  }

  @Test
  void validatesStance() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      String output = execute("1 flowers 14");

      assertThat(output, containsString("14 is not a valid stance"));
      assertErrorState();
    }
  }

  @Test
  void attacksWithAllSuccessfully() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      String output = execute("tougher fame 1");

      assertThat(
          output,
          containsString("Use all remaining PVP attacks to steal fame via Quality Assurance"));
      assertRequest(client, "fame", "tougher", 1);
    }
  }

  @Test
  void fuzzyMatchesStanceSuccessfully() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      client.addResponse(200, "");

      String output = execute("loots bet");

      assertThat(
          output, containsString("Use all remaining PVP attacks to steal loots via Beta Tester"));
      assertRequest(client, "lootwhatever", "random", 11);
    }
  }

  @Test
  void fuzzyMatchingFails() {
    var setup = setupClient();
    var client = setup.client;
    var cleanups = setup.cleanups;

    try (cleanups) {
      String totallyRealStance = "a totally real stance";
      String output = execute("1 flowers " + totallyRealStance);

      assertThat(
          output,
          containsString(
              "\"" + totallyRealStance + "\" does not uniquely match a currently known stance"));
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
      String output = execute("1 " + tougher + " " + mission + " 1");

      assertThat(
          output,
          containsString("Use 1 PVP attacks to steal " + mission + " via Quality Assurance"));
      assertRequest(client, mission, tougher, 1);
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
            withInteractivity(true),
            withProperty("defaultFlowerWinMessage", "lucky!"),
            withProperty("defaultFlowerLossMessage", "oops."));

    addResponses(client);
    return new SetupClient(client, cleanups);
  }

  private void addResponses(FakeHttpClient client) {
    client.addResponse(200, html("request/test_pvp_fight_menu.html")); // checkStances
  }

  private void assertRequest(FakeHttpClient client, String mission, String tougher, int stance) {
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
            + "&ranked="
            + tougherNum
            + "&stance="
            + stance
            + "&who=&winmessage=lucky!&losemessage=oops.");
    assertContinueState();
  }
}
