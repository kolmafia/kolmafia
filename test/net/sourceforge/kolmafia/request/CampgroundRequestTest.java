package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEmptyCampground;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
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
  void canDetectDeactivatedElVibratoPortal() {
    var cleanups = new Cleanups(withProperty("currentPortalEnergy", 20), withEmptyCampground());

    try (cleanups) {
      String html = html("request/test_campground_deactivated_el_vibrato.html");
      CampgroundRequest.parseResponse("campground.php", html);
      assertThat("currentPortalEnergy", isSetTo(0));
    }
  }
}
