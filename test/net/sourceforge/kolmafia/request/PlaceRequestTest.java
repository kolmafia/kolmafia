package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

class PlaceRequestTest {

        // These need to be before and after each because leakage has been observed between tests
        // in this class.
        @BeforeEach
        public void initializeCharPrefs() {
                KoLCharacter.reset( "fakePrefUser");
                KoLCharacter.reset(true);
                Preferences.saveSettingsToFile = false;
        }

        @AfterEach
        public void resetCharAndPrefs() {
                KoLCharacter.reset("");
                KoLCharacter.reset(true);
                KoLCharacter.setUserId(0);
                Preferences.saveSettingsToFile = false;
        }
  @Test
  void itShouldSetTheToolbeltAsAFreePullInTTT() {
    // setup environment for test...
    Preferences.setBoolean("timeTowerAvailable", false); // ttt not available.
    Modifiers.getModifiers("Item", "time-twitching toolbelt");
    AdventureResult toolbelt = ItemPool.get(ItemPool.TIME_TWITCHING_TOOLBELT, 1);

    List<AdventureResult> storage = KoLConstants.storage;
    List<AdventureResult> freePulls = KoLConstants.freepulls;
    AdventureResult.addResultToList(KoLConstants.storage, toolbelt);

    // check baseline condition
    assertNotEquals(-1, storage.indexOf(toolbelt), "toolbelt should be in storage before TTT is available");
    assertEquals(-1, freePulls.indexOf(toolbelt), "toolbelt should not be in Free Pulls before TTT is available");

    PlaceRequest.parseResponse("http://server.fakepath/place=twitch", "tower");

    assertTrue(Preferences.getBoolean("timeTowerAvailable"),"TTT is available");

    // time-twitching toolbelt is a free pull if the time tower is available. Verify it is in correct storage list.
    assertEquals(-1, storage.indexOf(toolbelt), "toolbelt should not be in storage when TTT is available");
    assertNotEquals(-1, freePulls.indexOf(toolbelt), "toolbelt should be in Free Pulls when TTT is available");

    // reset the TTT
    PlaceRequest.parseResponse("http://server.fakepath/place=twitch", "temporal ether");

    assertFalse(Preferences.getBoolean("timeTowerAvailable"),"TTT should not be available");

    // check after TTT disappears into the temporal ether
    assertNotEquals(-1, storage.indexOf(toolbelt), "toolbelt should not be in storage after TTT fades");
    assertEquals(-1, freePulls.indexOf(toolbelt), "toolbelt should not be in Free Pulls after TTT fades");

  }
}

// Generated with love by TestMe :) Please report issues and submit feature requests at:
// http://weirddev.com/forum#!/testme
