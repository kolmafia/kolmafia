package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAdjustmentsRecalculated;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class CampAwayRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("CampAwayRequest");
    Preferences.reset("CampAwayRequest");
  }

  @Test
  void canDetectUnlockedByVisiting() {
    var request = new CampAwayRequest();
    request.responseText = html("request/test_place_campaway_visit.html");
    request.processResults();

    assertThat("getawayCampsiteUnlocked", isSetTo(true));
  }

  @Test
  void doesNotUnlockIfVisitingIsRejected() {
    var request = new CampAwayRequest();
    request.responseText = html("request/test_place_campaway_dont_have_access.html");
    request.processResults();

    assertThat("getawayCampsiteUnlocked", isSetTo(false));
  }

  @Nested
  class FreeRest {
    public Cleanups propertyCleanups() {
      return new Cleanups(
          withProperty("getawayCampsiteUnlocked", true),
          withProperty("timesRested", 0),
          // more than one free rest available
          withItem(ItemPool.MOTHERS_NECKLACE),
          withAdjustmentsRecalculated());
    }

    @ParameterizedTest
    @CsvSource({"request/test_campaway_free.html, 0", "request/test_campaway_non_free.html, 6"})
    public void setsRestsOnVisit(String html, int rests) {
      var cleanups = propertyCleanups();
      try (cleanups) {
        CampAwayRequest request = new CampAwayRequest();
        request.responseText = html(html);
        request.setHasResult(true);
        request.processResponse();
        assertThat("timesRested", isSetTo(rests));
      }
    }

    @ParameterizedTest
    @CsvSource({"request/test_campaway_free.html, 1", "request/test_campaway_non_free.html, 6"})
    public void setsRestsOnRest(String html, int rests) {
      var cleanups = propertyCleanups();
      try (cleanups) {
        CampAwayRequest request = new CampAwayRequest("campaway_tentclick");
        request.responseText = html(html);
        request.setHasResult(true);
        request.processResponse();
        assertThat("timesRested", isSetTo(rests));
      }
    }
  }
}
