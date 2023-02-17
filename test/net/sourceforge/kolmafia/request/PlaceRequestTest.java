package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlaceRequestTest {

  @BeforeEach
  public void initializeCharPrefs() {
    KoLCharacter.reset("PlaceRequestTestFakePrefUser");
    KoLCharacter.reset(true);
  }

  @AfterEach
  public void resetCharAndPrefs() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
  }

  @Test
  void itShouldSetTheToolbeltAsAFreePullInTTT() {
    // setup environment for test...
    Preferences.setBoolean("timeTowerAvailable", false); // ttt not available.
    ModifierDatabase.getItemModifiers(ItemPool.TIME_TWITCHING_TOOLBELT);
    AdventureResult toolbelt = ItemPool.get(ItemPool.TIME_TWITCHING_TOOLBELT, 1);

    List<AdventureResult> storage = KoLConstants.storage;
    List<AdventureResult> freePulls = KoLConstants.freepulls;
    AdventureResult.addResultToList(KoLConstants.storage, toolbelt);

    // check baseline condition
    assertNotEquals(
        -1, storage.indexOf(toolbelt), "toolbelt should be in storage before TTT is available");
    assertEquals(
        -1,
        freePulls.indexOf(toolbelt),
        "toolbelt should not be in Free Pulls before TTT is available");

    PlaceRequest.parseResponse("place=twitch", "tower");

    assertTrue(Preferences.getBoolean("timeTowerAvailable"), "TTT is available");

    // time-twitching toolbelt is a free pull if the time tower is available. Verify it is in
    // correct storage list.
    assertEquals(
        -1, storage.indexOf(toolbelt), "toolbelt should not be in storage when TTT is available");
    assertNotEquals(
        -1, freePulls.indexOf(toolbelt), "toolbelt should be in Free Pulls when TTT is available");

    // reset the TTT
    PlaceRequest.parseResponse("place=twitch", "temporal ether");

    assertFalse(Preferences.getBoolean("timeTowerAvailable"), "TTT should not be available");

    // check after TTT disappears into the temporal ether
    assertNotEquals(
        -1, storage.indexOf(toolbelt), "toolbelt should not be in storage after TTT fades");
    assertEquals(
        -1, freePulls.indexOf(toolbelt), "toolbelt should not be in Free Pulls after TTT fades");
  }

  @Nested
  class speakeasy {
    private String sotUrl = "place.php?whichplace=speakeasy&action=olivers_sot";

    @Test
    public void itShouldGetParcelLocationFromFirstVisit() {
      String prefName = "_sotParcelLocation";
      String responseText = html("request/test_first_visit_sot_to_get_location.html");
      var cleanups = new Cleanups(withProperty(prefName, ""));
      try (cleanups) {
        PlaceRequest.parseResponse(sotUrl, responseText);
        assertThat(prefName, isSetTo("The Haunted Storage Room"));
      }
    }

    @Test
    public void itShouldGetParcelLocationFromSubsequentVisit() {
      String prefName = "_sotParcelLocation";
      var cleanups = new Cleanups(withProperty(prefName, ""));
      try (cleanups) {
        String responseText = html("request/test_next_visit_sot_to_get_location.html");
        PlaceRequest.parseResponse(sotUrl, responseText);
        assertThat(prefName, isSetTo("The Haunted Storage Room"));
      }
    }

    @Test
    public void itShouldRemoveParcelWhenTurnedIn() {
      String prefName = "_sotParcelReturned";
      String responseText = html("request/test_visit_sot_to_return.html");
      var cleanups =
          new Cleanups(withProperty(prefName, false), withItem(ItemPool.THE_SOTS_PARCEL, 1));
      try (cleanups) {
        PlaceRequest.parseResponse(sotUrl, responseText);
        assertEquals(0, InventoryManager.getCount(ItemPool.THE_SOTS_PARCEL));
        assertThat(prefName, isSetTo(true));
      }
    }

    @Test
    public void itShouldDetectParcelAlreadyTurnedIn() {
      String prefName = "_sotParcelReturned";
      String responseText = html("request/test_visit_sot_parcel_done.html");
      var cleanups = new Cleanups(withProperty(prefName, false));
      try (cleanups) {
        PlaceRequest.parseResponse(sotUrl, responseText);
        assertThat(prefName, isSetTo(true));
      }
    }
  }
}
