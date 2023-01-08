package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
  void canDetectRockGarden() {
    String html = html("request/test_campground_rock_garden_5.html");
    CampgroundRequest.parseResponse("campground.php", html);
    assertCampgroundItemCount(ItemPool.FRUITY_PEBBLE, 2);
    assertCampgroundItemCount(ItemPool.BOLDER_BOULDER, 2);
    assertCampgroundItemCount(ItemPool.HARD_ROCK, 2);
    assertCampgroundItemCount(ItemPool.ROCK_SEEDS, 1);
  }

  private void assertCampgroundItemCount(int itemId, int count) {
    var resOpt = KoLConstants.campground.stream().filter(x -> x.getItemId() == itemId).findAny();
    assertThat(resOpt.isPresent(), equalTo(true));
    var res = resOpt.get();
    assertThat(res.getCount(), equalTo(count));
  }
}
