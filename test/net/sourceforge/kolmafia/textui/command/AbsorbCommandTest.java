package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withConcoctionRefresh;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withRange;
import static internal.helpers.Player.withUsedAbsorbs;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AbsorbCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");
  }

  public AbsorbCommandTest() {
    this.command = "absorb";
  }

  @Test
  void mustBeInNoob() {
    var cleanups = withPath(Path.NONE);
    try (cleanups) {
      String output = execute("1 helmet turtle");
      assertThat(output, containsString("not in a Gelatinous Noob"));
      assertErrorState();
    }
  }

  @Test
  void mustSpecifyItem() {
    var cleanups = withPath(Path.GELATINOUS_NOOB);
    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("No items specified"));
      assertErrorState();
    }
  }

  @Test
  void mustHaveAbsorbs() {
    var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB), withUsedAbsorbs(100));
    try (cleanups) {
      String output = execute("1 helmet turtle");

      assertThat(output, containsString("Cannot absorb items"));
      assertErrorState();
    }
  }

  @Test
  void mustSpecifyValidItem() {
    var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB));
    try (cleanups) {
      String output = execute("invalid item");

      assertThat(output, containsString("What item"));
      assertContinueState();
    }
  }

  @Test
  void mustHaveItem() {
    var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB));
    try (cleanups) {
      String output = execute("1 dirty bottlecap");

      assertThat(output, containsString("Item not accessible"));
      assertErrorState();
    }
  }

  @Test
  void canDetectFailedAbsorbItems() {
    var cleanups =
        new Cleanups(
            withPath(Path.GELATINOUS_NOOB),
            withItem("A Light that Never Goes Out", 15),
            withUsedAbsorbs(0));

    try (cleanups) {
      String output = execute("15 A Light that Never Goes Out");

      assertThat("_noobSkillCount", isSetTo(0));
      assertThat(output, containsString("Failed to absorb 15 Lights that Never Go Out"));
      assertErrorState();
    }
  }

  @Test
  void canAbsorbItems() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withPath(Path.GELATINOUS_NOOB),
            withHttpClientBuilder(builder),
            withItem("A Light that Never Goes Out", 15),
            withUsedAbsorbs(0));
    try (cleanups) {
      for (int i = 0; i < 15; i++) {
        client.addResponse(200, "");
      }

      client.addResponse(200, html("request/test_gel_noob_charsheet.html"));

      String output = execute("15 A Light that Never Goes Out");

      assertThat("_noobSkillCount", isSetTo(15));
      assertThat(output, containsString("Absorbed 15 Lights that Never Go Out"));
      assertContinueState();
    }

    var requests = client.getRequests();
    // Under some circumstances loading the charpane will trigger a request for the image
    // gladiatar.gif.  So this request should have 15 responses for the absorb, one for the charpane
    // and perhaps one for the image
    assertTrue(
        requests.size() >= 16,
        "Unexpected number of responses: " + requests.size()); // 15 items + 1 charpane
  }

  @Test
  void canCreateItemsToAbsorb() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withPath(Path.GELATINOUS_NOOB),
            withHttpClientBuilder(builder),
            withItem("pie crust", 1),
            withItem("strawberry", 1),
            withUsedAbsorbs(0),
            withRange(),
            withConcoctionRefresh());
    try (cleanups) {
      execute("Strawberry pie");
    }

    var requests = client.getRequests();
    assertPostRequest(
        requests.get(0), "/craft.php", "action=craft&mode=cook&ajax=1&a=160&b=786&qty=1");
  }
}
