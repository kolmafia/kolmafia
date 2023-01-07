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
    var maidOpt =
        KoLConstants.campground.stream()
            .filter(x -> x.getItemId() == ItemPool.CLOCKWORK_MAID)
            .findAny();
    assertThat(maidOpt.isPresent(), equalTo(true));
    var maid = maidOpt.get();
    assertThat(maid.getCount(), equalTo(1));
  }
}
