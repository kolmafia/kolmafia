package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withCampgroundItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GardenCommandTest extends AbstractCommandTestBase {

  public GardenCommandTest() {
    this.command = "garden";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  public void noGardenErrors() {
    String output = execute("");
    assertThat(output, containsString("You don't have a garden"));
  }

  @Test
  public void inspectsThanksgarden() {
    var cleanups = withCampgroundItem(ItemPool.CORNUCOPIA, 1);

    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("Your thanksgarden garden has 1 cornucopia in it."));
    }
  }

  @Test
  public void inspectsThanksgardenPlural() {
    var cleanups = withCampgroundItem(ItemPool.CORNUCOPIA, 2);

    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("Your thanksgarden garden has 2 cornucopias in it."));
    }
  }

  @Test
  public void inspectsEmptyRockGarden() {
    var cleanups = withCampgroundItem(ItemPool.GROVELING_GRAVEL, 0);

    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("Your rock garden has nothing in it."));
    }
  }

  @Test
  public void inspectsPartialRockGarden() {
    var cleanups = withCampgroundItem(ItemPool.GROVELING_GRAVEL, 1);

    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("Your rock garden has 1 groveling gravel in it."));
    }
  }

  @Test
  public void inspectsFullRockGarden() {
    var cleanups =
        new Cleanups(
            withCampgroundItem(ItemPool.FRUITY_PEBBLE, 2),
            withCampgroundItem(ItemPool.BOLDER_BOULDER, 2),
            withCampgroundItem(ItemPool.HARD_ROCK, 2));

    try (cleanups) {
      String output = execute("");
      assertThat(
          output,
          containsString(
              "Your rock garden has 2 fruity pebbles, and 2 bolder boulders, and 2 hard rocks in it."));
    }
  }

  @Test
  public void picksPartialRockGarden() {
    var cleanups = withCampgroundItem(ItemPool.GROVELING_GRAVEL, 1);

    try (cleanups) {
      execute("pick");

      var requests = getRequests();
      assertThat(requests, hasSize(1));
      assertPostRequest(requests.get(0), "/campground.php", "action=rgarden1");
    }
  }

  @Test
  public void picksFullRockGarden() {
    var cleanups =
        new Cleanups(
            withCampgroundItem(ItemPool.FRUITY_PEBBLE, 2),
            withCampgroundItem(ItemPool.BOLDER_BOULDER, 2),
            withCampgroundItem(ItemPool.HARD_ROCK, 2));

    try (cleanups) {
      execute("pick");

      var requests = getRequests();
      assertThat(requests, hasSize(3));
      assertPostRequest(requests.get(0), "/campground.php", "action=rgarden1");
      assertPostRequest(requests.get(1), "/campground.php", "action=rgarden2");
      assertPostRequest(requests.get(2), "/campground.php", "action=rgarden3");
    }
  }
}
