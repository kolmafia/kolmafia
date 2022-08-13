package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AscensionRequestTest {
    @BeforeEach
    public void initializeCharPrefs() {
        KoLCharacter.reset("fakePrefUser");
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
    void testAscensionsTodayTracked() {
        String name = "ascensionsToday";
        int beforeAscension = 1;
        int afterAscension = 2;

        // Confirm it was set
        Preferences.setInteger(name, beforeAscension);
        // Set last breakfast to 0 to mark that we've ascended
        Preferences.setInteger("lastBreakfast", 0);

        // Execute a request to enter the pearly gates
        var pearlyGates = new GenericRequest("afterlife.php?action=pearlygates");
        pearlyGates.responseText = html("request/test_ascension_enter_valhalla.html");
        pearlyGates.setHasResult(true);
        pearlyGates.execute();

        // confirm changed
        assertEquals(afterAscension, Preferences.getInteger(name));
    }

    @Test
    void testPreAscension() {
        // This does a 302 redirect to afterlife.php
        var jumpGash = new GenericRequest("ascend.php?action=ascend&pwd&confirm=on&confirm2=on");
        jumpGash.responseText = html("request/test_ascension_jump_gash.html");
        jumpGash.setHasResult(true);
        jumpGash.execute();

        // TODO Test that our user run script executes
    }

    @Test
    void testSessionLogUpdates() {
        var ascendRequest = new GenericRequest("afterlife.php?action=ascend&asctype=2&whichclass=1&gender=2&whichpath=44&whichsign=3");
        ascendRequest.responseText = html("request/test_ascension_confirm_ascend.html");
        ascendRequest.setHasResult(true);
        ascendRequest.execute();
        // TODO Test the session log wasn't updated

        // This does a 302 redirect
        var ascendConfirm =
            new GenericRequest("afterlife.php?action=ascend&confirmascend=1&whichsign=3&gender=2&whichclass=27&whichpath=44&asctype=2&nopetok=1");
        ascendConfirm.responseText = html("request/test_ascension_ascend_grey_you.html");
        ascendConfirm.setHasResult(true);
        ascendConfirm.execute();
        // TODO Test that the session log output is as expected
    }
}
