package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlaceRequestTest {

  // These need to be before and after each because leakage has been observed between tests
  // in this class.
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

    PlaceRequest.parseResponse("http://server.fakepath/place=twitch", "tower");

    assertTrue(Preferences.getBoolean("timeTowerAvailable"), "TTT is available");

    // time-twitching toolbelt is a free pull if the time tower is available. Verify it is in
    // correct storage list.
    assertEquals(
        -1, storage.indexOf(toolbelt), "toolbelt should not be in storage when TTT is available");
    assertNotEquals(
        -1, freePulls.indexOf(toolbelt), "toolbelt should be in Free Pulls when TTT is available");

    // reset the TTT
    PlaceRequest.parseResponse("http://server.fakepath/place=twitch", "temporal ether");

    assertFalse(Preferences.getBoolean("timeTowerAvailable"), "TTT should not be available");

    // check after TTT disappears into the temporal ether
    assertNotEquals(
        -1, storage.indexOf(toolbelt), "toolbelt should not be in storage after TTT fades");
    assertEquals(
        -1, freePulls.indexOf(toolbelt), "toolbelt should not be in Free Pulls after TTT fades");
  }
}

// Generated with love by TestMe :) Please report issues and submit feature requests at:
// http://weirddev.com/forum#!/testme
// Applies to some of the above code

@Nested
class Speakeasy {
  @BeforeEach
  public void initializeCharPrefs() {
    KoLCharacter.reset("PlaceRequestTestFakeSotVisit");
    KoLCharacter.reset(true);
  }

  @AfterEach
  public void resetCharAndPrefs() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
  }

  @Test
  public void itShouldGetParcelLocationFromVisit() {
    String prefName = "_sotParcelLocation";
    assertEquals("", Preferences.getString(prefName), "Preference already set.");
    var req = new GenericRequest("place.php?whichplace=speakeasy&action=olivers_sot");
    req.responseText = html("request/test_first_visit_sot_to_get_location.html");
    PlaceRequest.parseResponse(
        "http://server.fakepath/place.php?whichplace=speakeasy&action=olivers_sot",
        req.responseText);
    assertEquals(
        "The Haunted Storage Room", Preferences.getString(prefName), "Preference not set.");
  }

  @Test
  public void itShouldGetParcelLocationFromSubsequentVisit() {
    String prefName = "_sotParcelLocation";
    assertEquals("", Preferences.getString(prefName), "Preference already set.");
    var req = new GenericRequest("place.php?whichplace=speakeasy&action=olivers_sot");
    req.responseText = html("request/test_next_visit_sot_to_get_location.html");
    PlaceRequest.parseResponse(
            "http://server.fakepath/place.php?whichplace=speakeasy&action=olivers_sot",
            req.responseText);
    assertEquals(
            "The Haunted Storage Room", Preferences.getString(prefName), "Preference not set.");
  }
}
