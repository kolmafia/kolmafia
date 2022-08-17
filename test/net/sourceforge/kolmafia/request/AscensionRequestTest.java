package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
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
    // Simple test to confirm that it resets our last run breakfast
    String breakfastPref = "lastBreakfast";
    int breakfastBefore = 5;
    int breakfastAfter = 0;
    int breakfastValhalla = -1;
    String ascensionsPref = "ascensionsToday";
    int ascensionBefore = 1;
    int ascensionAfter = 2;

    // Set breakfast and ascension prefs
    Preferences.setInteger(ascensionsPref, ascensionBefore);
    Preferences.setInteger(breakfastPref, breakfastBefore);

    // This does a 302 redirect to afterlife.php
    var jumpGash = new GenericRequest("ascend.php?action=ascend&pwd&confirm=on&confirm2=on");
    jumpGash.execute();

    // Confirm our last run breakfast is now 0
    assertEquals(breakfastAfter, Preferences.getInteger(breakfastPref));
    // Confirm our ascensions today has not changed
    assertEquals(ascensionBefore, Preferences.getInteger(ascensionsPref));

    // Execute the result of the 302 redirect and get the output which puts us in valhalla
    var afterlife = new GenericRequest("afterlife.php");
    afterlife.responseText = html("request/test_ascension_jump_gash.html");
    afterlife.setHasResult(true);
    afterlife.execute();

    // Confirm our last run breakfast is now -1
    assertEquals(breakfastValhalla, Preferences.getInteger(breakfastPref));
    // Confirm our ascensions today has incremented
    assertEquals(ascensionAfter, Preferences.getInteger(ascensionsPref));
  }

  @Test
  void testKarmaUpdates() {
    // Test that our karma is updated correctly when we enter the pearly gates
    String pref = "bankedKarma";
    int karmaBeforehand = 100;
    int karmaAfter = 211;

    // Set our karma beforehand to 100
    Preferences.setInteger(pref, karmaBeforehand);
    // Confirm our karma is 100
    assertEquals(karmaBeforehand, Preferences.getInteger(pref));

    // Execute a request to enter the pearly gates
    var pearlyGates = new GenericRequest("afterlife.php?action=pearlygates");
    pearlyGates.responseText = html("request/test_ascension_enter_valhalla.html");
    pearlyGates.setHasResult(true);
    pearlyGates.processResponse();

    // Confirm our karma is now 211
    assertEquals(karmaAfter, Preferences.getInteger(pref));
  }

  @Test
  void testSelectPathUnconfirmedUnprocessed() {
    // Set last breakfast to -1 to mark that we've ascended as done in testAscensionsTodayTracked
    Preferences.setInteger("lastBreakfast", -1);
    Preferences.setInteger("bankedKarma", 150);
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    try (PrintStream out = new PrintStream(ostream, true)) {
      RequestLogger.openCustom(out);

      // Testing to ensure "Are you sure" is not handled
      var ascendRequest =
          new GenericRequest(
              "afterlife.php?action=ascend&asctype=2&whichclass=1&gender=2&whichpath=44&whichsign=3");
      ascendRequest.setHasResult(true);
      ascendRequest.execute();

      RequestLogger.closeCustom();
    }

    // Confirm unchanged, we did not actually ascend yet
    String logged = ostream.toString().trim();
    assertEquals("", logged);
  }

  @Test
  void testSelectPathConfirmedProcessed() {
    // Set last breakfast to -1 to mark that we've ascended as done in testAscensionsTodayTracked
    Preferences.setInteger("lastBreakfast", -1);
    Preferences.setInteger("bankedKarma", 150);
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    try (PrintStream out = new PrintStream(ostream, true)) {
      RequestLogger.openCustom(out);

      // This does a 302 redirect
      var ascendConfirm =
          new GenericRequest(
              "afterlife.php?action=ascend&confirmascend=1&whichsign=3&gender=2&whichclass=27&whichpath=44&asctype=2&nopetok=1");
      ascendConfirm.setHasResult(true);
      ascendConfirm.execute();

      RequestLogger.closeCustom();
    }

    String logged = ostream.toString().trim();
    assertThat(
        logged,
        containsString(
            "Ascend as a Normal Female Grey Goo under the Vole sign on a Grey You path, banking 150 Karma."));
  }
}
