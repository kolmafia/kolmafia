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

    // This does a 302 redirect to afterlife.php
    var jumpGash = new GenericRequest("ascend.php?action=ascend&pwd&confirm=on&confirm2=on");
    jumpGash.responseText = html("request/test_ascension_jump_gash.html");
    jumpGash.setHasResult(true);
    jumpGash.execute();

    var pearlyGates = new GenericRequest("afterlife.php?action=pearlygates");
    pearlyGates.responseText = html("request/test_ascension_enter_valhalla.html");
    pearlyGates.setHasResult(true);
    pearlyGates.execute();

    var ascendRequest =
        new GenericRequest(
            "afterlife.php?action=ascend&asctype=2&whichclass=1&gender=2&whichpath=44&whichsign=3");
    ascendRequest.responseText = html("request/test_ascension_confirm_ascend.html");
    ascendRequest.setHasResult(true);
    ascendRequest.execute();

    // This does a 302 redirect
    var ascendConfirm =
        new GenericRequest(
            "afterlife.php?action=ascend&confirmascend=1&whichsign=3&gender=2&whichclass=27&whichpath=44&asctype=2&nopetok=1");
    ascendConfirm.responseText = html("request/test_ascension_ascend_grey_you.html");
    ascendConfirm.setHasResult(true);
    ascendConfirm.execute();

    // confirm changed
    assertEquals(afterAscension, Preferences.getInteger(name));
  }
}
