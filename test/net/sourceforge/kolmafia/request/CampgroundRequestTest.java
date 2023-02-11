package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
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
  class RockGarden {
    @AfterEach
    public void afterEach() {
      CampgroundRequest.reset();
    }

    @Test
    void canDetectNoGarden() {
      String html = html("request/test_campground_no_garden.html");
      CampgroundRequest.parseResponse("campground.php", html);
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
  }

  private void assertCampgroundItemCount(int itemId, int count) {
    var resOpt = KoLConstants.campground.stream().filter(x -> x.getItemId() == itemId).findAny();
    assertThat(resOpt.isPresent(), equalTo(true));
    var res = resOpt.get();
    assertThat(res.getCount(), equalTo(count));
  }
}
