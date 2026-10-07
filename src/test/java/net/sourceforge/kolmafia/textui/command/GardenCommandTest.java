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
      try (var _ = withCampgroundItem(ItemPool.CORNUCOPIA, 1)) {
        String output = execute("");
        assertThat(output, containsString("Your thanksgarden garden has 1 cornucopia in it."));
      }
    }

    @Test
    public void inspectsThanksgardenPlural() {
      try (var _ = withCampgroundItem(ItemPool.CORNUCOPIA, 2)) {
        String output = execute("");
        assertThat(output, containsString("Your thanksgarden garden has 2 cornucopias in it."));
      }
    }
  }

  @Nested
  class Grass {
    @Test
    public void inspectsEmptyGrassGarden() {
      try (var _ = withCampgroundItem(CampgroundRequest.NO_TALL_GRASS)) {
        String output = execute("");
        assertThat(output, containsString("Your grass garden has 0 patches of tall grass in it."));
      }
    }

    @Test
    public void inspectsPartialGrassGarden() {
      try (var _ = withCampgroundItem(CampgroundRequest.FOUR_TALL_GRASS)) {
        String output = execute("");
        assertThat(output, containsString("Your grass garden has 4 patches of tall grass in it."));
      }
    }

    @Test
    public void inspectsFullGrassGarden() {
      try (var _ = withCampgroundItem(CampgroundRequest.VERY_TALL_GRASS)) {
        String output = execute("");
        assertThat(
            output, containsString("Your grass garden has 1 patch of very tall grass in it."));
      }
    }

    @Test
    public void picksTallGrass() {
      try (var _ = withCampgroundItem(CampgroundRequest.FOUR_TALL_GRASS)) {
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
      try (var _ = withCampgroundItem(CampgroundRequest.VERY_TALL_GRASS)) {
        execute("pick");

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/campground.php", "action=garden");
      }
    }
  }

  @Nested
  class BlackRose {
    @Test
    public void inspectsNewBlackRoseGarden() {
      var cleanups = withCampgroundItem(new CampgroundRequest.BlackRose(1));

      try (cleanups) {
        String output = execute("");
        assertThat(output, containsString("Your Black Rose Garden has 1 day's growth."));
      }
    }

    @Test
    public void inspectsGrownBlackRoseGarden() {
      var cleanups = withCampgroundItem(new CampgroundRequest.BlackRose(3));

      try (cleanups) {
        String output = execute("");
        assertThat(output, containsString("Your Black Rose Garden has 3 days' growth."));
      }
    }

    @Test
    public void doesNotPickBlackRoseGarden() {
      var cleanups = withCampgroundItem(new CampgroundRequest.BlackRose(3));

      try (cleanups) {
        String output = execute("pick");
        assertThat(
            output,
            containsString(
                "There is nothing to pick in the Black Rose Garden, only a fun maze to explore!"));

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }
  }

  @Nested
  class Rock {
    @Test
    public void inspectsEmptyRockGarden() {
      try (var _ = withCampgroundItem(ItemPool.GROVELING_GRAVEL, 0)) {
        String output = execute("");
        assertThat(output, containsString("Your rock garden has nothing in it."));
      }
    }

    @Test
    public void inspectsPartialRockGarden() {
      try (var _ = withCampgroundItem(ItemPool.GROVELING_GRAVEL, 1)) {
        String output = execute("");
        assertThat(output, containsString("Your rock garden has 1 groveling gravel in it."));
      }
    }

    @Test
    public void inspectsFullRockGarden() {
      try (var _ =
          new Cleanups(
              withCampgroundItem(ItemPool.FRUITY_PEBBLE, 2),
              withCampgroundItem(ItemPool.BOLDER_BOULDER, 2),
              withCampgroundItem(ItemPool.HARD_ROCK, 2))) {
        String output = execute("");
        assertThat(
            output,
            containsString(
                "Your rock garden has 2 fruity pebbles, and 2 bolder boulders, and 2 hard rocks in it."));
      }
    }

    @Test
    public void picksPartialRockGarden() {
      try (var _ = withCampgroundItem(ItemPool.GROVELING_GRAVEL, 1)) {
        execute("pick");

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/campground.php", "action=rgarden1");
      }
    }

    @Test
    public void picksFullRockGarden() {
      try (var _ =
          new Cleanups(
              withCampgroundItem(ItemPool.FRUITY_PEBBLE, 2),
              withCampgroundItem(ItemPool.BOLDER_BOULDER, 2),
              withCampgroundItem(ItemPool.HARD_ROCK, 2))) {
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
      try (var _ = withCampgroundItem(ItemPool.GROVELING_GRAVEL, 1)) {
        var output = execute("pick plot2 plot3");
        assertThat(output, containsString("There is nothing to pick in plot2."));
        assertThat(output, containsString("There is nothing to pick in plot3."));

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void picksSelectPlotsInFullRockGarden() {
      try (var _ =
          new Cleanups(
              withCampgroundItem(ItemPool.FRUITY_PEBBLE, 2),
              withCampgroundItem(ItemPool.BOLDER_BOULDER, 2),
              withCampgroundItem(ItemPool.HARD_ROCK, 2))) {
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
