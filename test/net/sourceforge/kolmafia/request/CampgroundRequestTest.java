package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest.CropType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CampgroundRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("CampgroundRequest");
    Preferences.reset("CampgroundRequest");
  }

  @AfterEach
  public void afterEach() {
    CampgroundRequest.reset();
  }

  private void assertCampgroundItemAbsent(int itemId) {
    var resOpt = KoLConstants.campground.stream().filter(x -> x.getItemId() == itemId).findAny();
    assertThat(resOpt.isPresent(), equalTo(false));
  }

  private void assertCampgroundItemCount(int itemId, int count) {
    var resOpt = KoLConstants.campground.stream().filter(x -> x.getItemId() == itemId).findAny();
    assertThat(resOpt.isPresent(), equalTo(true));
    var res = resOpt.get();
    assertThat(res.getCount(), equalTo(count));
  }

  @Test
  void canDetectExhaustedMedicineCabinet() {
    String html = html("request/test_campground_medicine_cabinet_out_of_consults.html");
    CampgroundRequest.parseResponse("campground.php?action=workshed", html);
    assertEquals(
        CampgroundRequest.getCurrentWorkshedItem().getItemId(), ItemPool.COLD_MEDICINE_CABINET);
  }

  @Test
  void canDetectMeatMaid() {
    String html = html("request/test_campground_inspect_dwelling.html");
    CampgroundRequest.parseResponse("campground.php?action=inspectdwelling", html);
    assertCampgroundItemCount(ItemPool.CLOCKWORK_MAID, 1);
  }

  @Test
  void updatesBookshelfLibramCasts() {
    String url = "campground.php?preaction=summoncandyheart&quantity=12";
    // We don't actually do anything with the response, other than check that it's non-empty.
    String html = "non-empty response";

    var mocked = Mockito.mockStatic(ApiRequest.class, Mockito.CALLS_REAL_METHODS);
    mocked
        .when(() -> ApiRequest.updateStatus())
        .thenAnswer(
            invocation -> {
              // UseSkillRequest just cares that MP is updated.
              KoLCharacter.setMP(500, 1000, 1000);
              return null;
            });

    try (var cleanups =
        new Cleanups(
            withProperty("libramSummons", 0),
            withMP(1000, 1000, 1000),
            new Cleanups(mocked::close))) {
      // Initial state.
      assertEquals(UseSkillRequest.lastSkillUsed, -1);
      assertEquals(UseSkillRequest.lastSkillCount, 0);
      assertEquals(Preferences.getInteger("libramSummons"), 0);

      CampgroundRequest.registerRequest(url);
      assertEquals(UseSkillRequest.lastSkillUsed, SkillPool.CANDY_HEART);
      assertEquals(UseSkillRequest.lastSkillCount, 12);

      CampgroundRequest.parseResponse(url, html);
      assertEquals(Preferences.getInteger("libramSummons"), 12);
      assertEquals(UseSkillRequest.lastSkillUsed, -1);
      assertEquals(UseSkillRequest.lastSkillCount, 0);
    }
  }

  @Nested
  class Pumpkins {
    @Test
    void canDetectGinormousPumpkinHouse() {
      // Campground has a pumpkin house as a dwelling and no garden.
      String html = html("request/test_ginormous_pumpkin_house.html");
      CampgroundRequest.parseResponse("campground.php", html);
      // Correct dwelling
      assertEquals(CampgroundRequest.GINORMOUS_PUMPKIN, CampgroundRequest.getCurrentDwelling());
      // No garden
      assertEquals(null, CampgroundRequest.getCropType());
      // No crop
      assertEquals(null, CampgroundRequest.getCrop());
      // Dwelling does not appear in Campground item list
      assertCampgroundItemAbsent(ItemPool.GINORMOUS_PUMPKIN);
    }

    @Test
    void canDetectGinormousPumpkinCrop() {
      // Campground has a house as a dwelling and a pumpkin garden with
      // a ginormous pumpkin in it.
      String html = html("request/test_ginormous_pumpkin_crop.html");
      CampgroundRequest.parseResponse("campground.php", html);
      // Correct dwelling
      assertEquals(ItemPool.get(ItemPool.HOUSE), CampgroundRequest.getCurrentDwelling());
      // Correct garden
      assertEquals(CropType.PUMPKIN, CampgroundRequest.getCropType());
      // Correct crop
      assertEquals(CampgroundRequest.GINORMOUS_PUMPKIN, CampgroundRequest.getCrop());
      // Correct crop level
      assertCampgroundItemCount(ItemPool.PUMPKIN_SEEDS, 11);
      // Crop appears in Campground item list
      assertCampgroundItemCount(ItemPool.GINORMOUS_PUMPKIN, 1);
    }

    @Test
    void canDetectGinormousPumpkinHouseAndCrop() {
      // Campground has both a pumpkin house as a dwelling and a pumpkin
      // garden with a ginormous pumpkin in it.
      String html = html("request/test_ginormous_pumpkin_house_and_crop.html");
      CampgroundRequest.parseResponse("campground.php", html);
      // Correct dwelling
      assertEquals(CampgroundRequest.GINORMOUS_PUMPKIN, CampgroundRequest.getCurrentDwelling());
      // Correct garden
      assertEquals(CropType.PUMPKIN, CampgroundRequest.getCropType());
      // Correct crop
      assertEquals(CampgroundRequest.GINORMOUS_PUMPKIN, CampgroundRequest.getCrop());
      // Correct crop level
      assertCampgroundItemCount(ItemPool.PUMPKIN_SEEDS, 11);
      // Crop appears in Campground item list
      assertCampgroundItemCount(ItemPool.GINORMOUS_PUMPKIN, 1);
    }
  }

  @Nested
  class RockGarden {
    @Test
    void canDetectNoGarden() {
      String html = html("request/test_campground_no_garden.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertThat(CampgroundRequest.getCropType(), nullValue());
      assertThat(CampgroundRequest.getCrop(), nullValue());
      assertThat(CampgroundRequest.getCrops(), empty());
    }

    @Test
    void canDetectNoRockGarden() {
      String html = html("request/test_campground_rock_garden_0.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertCampgroundItemCount(ItemPool.GROVELING_GRAVEL, 0);
      assertCampgroundItemCount(ItemPool.MILESTONE, 0);
      assertCampgroundItemCount(ItemPool.WHETSTONE, 0);
      assertCampgroundItemCount(ItemPool.ROCK_SEEDS, 0);
    }

    @Test
    void canDetectPartialRockGarden() {
      String html = html("request/test_campground_rock_garden_0_1.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertCampgroundItemCount(ItemPool.GROVELING_GRAVEL, 1);
      assertCampgroundItemCount(ItemPool.MILESTONE, 0);
      assertCampgroundItemCount(ItemPool.WHETSTONE, 0);
      assertCampgroundItemCount(ItemPool.ROCK_SEEDS, 1);
    }

    @Test
    void canDetectRockGarden() {
      String html = html("request/test_campground_rock_garden_5.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertCampgroundItemCount(ItemPool.FRUITY_PEBBLE, 2);
      assertCampgroundItemCount(ItemPool.BOLDER_BOULDER, 2);
      assertCampgroundItemCount(ItemPool.HARD_ROCK, 2);
      assertCampgroundItemCount(ItemPool.ROCK_SEEDS, 1);
    }

    @Test
    void canTrackRockGardenHarvest() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.MILESTONE),
              withCampgroundItem(ItemPool.GROVELING_GRAVEL),
              withCampgroundItem(ItemPool.WHETSTONE),
              withNextResponse(
                  200, html("request/test_campground_tracks_rock_garden_harvest.html")),
              withItem(ItemPool.WHETSTONE, 0));

      try (cleanups) {
        new GenericRequest("campground.php?action=rgarden3&pwd").run();

        assertThat(KoLConstants.inventory, hasItem(ItemPool.get(ItemPool.WHETSTONE)));
        assertCampgroundItemCount(ItemPool.MILESTONE, 1);
        assertCampgroundItemCount(ItemPool.GROVELING_GRAVEL, 1);
        assertCampgroundItemCount(ItemPool.WHETSTONE, 0);
      }
    }

    @Test
    void canStopTrackingRockGarden() {
      CampgroundRequest.parseResponse(
          "campground.php", html("request/test_campground_rock_garden_5.html"));

      assertThat(KoLConstants.campground, hasItem(ItemPool.get(ItemPool.ROCK_SEEDS)));
      assertThat(KoLConstants.campground, hasItem(ItemPool.get(ItemPool.FRUITY_PEBBLE)));

      CampgroundRequest.parseResponse(
          "campground.php", html("request/test_campground_no_garden.html"));

      assertThat(KoLConstants.campground, not(hasItem(ItemPool.get(ItemPool.ROCK_SEEDS))));
      assertThat(KoLConstants.campground, not(hasItem(ItemPool.get(ItemPool.FRUITY_PEBBLE))));
    }
  }

  @Test
  void trackCinchoLoosening() {
    var cleanups =
        new Cleanups(
            withNextResponse(200, html("request/test_rest_cincho_loosens.html")),
            withProperty("_cinchUsed", 75),
            withProperty("_cinchoRests", 2));

    try (cleanups) {
      new GenericRequest("campground.php?action=rest").run();

      assertThat("_cinchUsed", isSetTo(45));
      assertThat("_cinchoRests", isSetTo(3));
    }
  }
}
