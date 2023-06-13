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
import net.sourceforge.kolmafia.request.CampgroundRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

  @Nested
  class Thanksgarden {
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
  }

  @Nested
  class Grass {
    @Test
    public void inspectsEmptyGrassGarden() {
      var cleanups = withCampgroundItem(CampgroundRequest.NO_TALL_GRASS);

      try (cleanups) {
        String output = execute("");
        assertThat(output, containsString("Your grass garden has 0 patches of tall grass in it."));
      }
    }

    @Test
    public void inspectsPartialGrassGarden() {
      var cleanups = withCampgroundItem(CampgroundRequest.FOUR_TALL_GRASS);

      try (cleanups) {
        String output = execute("");
        assertThat(output, containsString("Your grass garden has 4 patches of tall grass in it."));
      }
    }

    @Test
    public void inspectsFullGrassGarden() {
      var cleanups = withCampgroundItem(CampgroundRequest.VERY_TALL_GRASS);

      try (cleanups) {
        String output = execute("");
        assertThat(
            output, containsString("Your grass garden has 1 patch of very tall grass in it."));
      }
    }

    @Test
    public void picksTallGrass() {
      var cleanups = withCampgroundItem(CampgroundRequest.FOUR_TALL_GRASS);

      try (cleanups) {
        execute("pick");

        var requests = getRequests();
        assertThat(requests, hasSize(4));
        assertPostRequest(requests.get(0), "/campground.php", "action=garden");
        assertPostRequest(requests.get(1), "/campground.php", "action=garden");
        assertPostRequest(requests.get(2), "/campground.php", "action=garden");
        assertPostRequest(requests.get(3), "/campground.php", "action=garden");
      }
    }

    @Test
    public void picksVeryTallGrass() {
      var cleanups = withCampgroundItem(CampgroundRequest.VERY_TALL_GRASS);

      try (cleanups) {
        execute("pick");

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/campground.php", "action=garden");
      }
    }
  }

  @Nested
  class Rock {
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

    @Test
    public void skipsEmptySlotsInPartialRockGarden() {
      var cleanups = withCampgroundItem(ItemPool.GROVELING_GRAVEL, 1);

      try (cleanups) {
        var output = execute("pick plot2 plot3");
        assertThat(output, containsString("There is nothing to pick in plot2."));
        assertThat(output, containsString("There is nothing to pick in plot3."));

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void picksSelectPlotsInFullRockGarden() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.FRUITY_PEBBLE, 2),
              withCampgroundItem(ItemPool.BOLDER_BOULDER, 2),
              withCampgroundItem(ItemPool.HARD_ROCK, 2));

      try (cleanups) {
        var output = execute("pick plot1 plot3");
        assertThat(output, containsString("Harvesting plot1: fruity pebble (2)"));
        assertThat(output, containsString("Harvesting plot3: hard rock (2)"));

        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/campground.php", "action=rgarden1");
        assertPostRequest(requests.get(1), "/campground.php", "action=rgarden3");
      }
    }
  }
}
