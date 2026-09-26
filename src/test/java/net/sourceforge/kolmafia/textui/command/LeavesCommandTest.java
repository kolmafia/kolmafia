package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withEmptyCampground;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class LeavesCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("LeavesCommandTest");
    Preferences.reset("LeavesCommandTest");
  }

  public LeavesCommandTest() {
    this.command = "leaves";
  }

  @Test
  void mustHaveBurningLeaves() {
    var cleanups = withEmptyCampground();
    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("You must have a Pile of Burning Leaves"));
      assertErrorState();
    }
  }

  @Test
  void cannotBurnMoreThanYouHave() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.INFLAMMABLE_LEAF, 2),
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES));
    try (cleanups) {
      execute("3");
      var requests = client.getRequests();
      assertThat(requests.size(), is(0));
      assertErrorState();
    }
  }

  @Test
  void canBurnNumberOfLeaves() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.INFLAMMABLE_LEAF, 3),
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES));
    try (cleanups) {
      execute("3");
      var requests = client.getRequests();
      assertThat(requests.size(), is(2));
      assertGetRequest(requests.get(0), "/campground.php", "preaction=leaves");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1510&option=1&leaves=3");
      assertContinueState();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "leaflet, 11",
    "viathan, 666",
    "super-heated, 11111",
  })
  void canBurnKnownItem(final String param, final int leaves) {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withHandlingChoice(false),
            withHP(11111, 11111, 11111),
            withItem(ItemPool.INFLAMMABLE_LEAF, leaves),
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES));
    try (cleanups) {
      var output = execute(param);
      var requests = client.getRequests();
      assertThat(requests.size(), is(2));
      assertGetRequest(requests.get(0), "/campground.php", "preaction=leaves");
      assertPostRequest(
          requests.get(1), "/choice.php", "whichchoice=1510&option=1&leaves=" + leaves);
      assertContinueState();
    }
  }

  @Test
  void doesNotBurnUnknownSearch() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.INFLAMMABLE_LEAF, 11111),
            withCampgroundItem(ItemPool.A_GUIDE_TO_BURNING_LEAVES));
    try (cleanups) {
      execute("seal tooth");
      var requests = client.getRequests();
      assertThat(requests.size(), is(0));
      assertErrorState();
    }
  }
}
