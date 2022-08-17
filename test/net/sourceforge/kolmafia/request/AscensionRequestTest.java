package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponseRedirectedTo;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
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
    // Fix another test not cleaning up
    FightRequest.currentRound = 0;
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
    var cleanups =
        new Cleanups(
            withProperty(breakfastPref, breakfastBefore),
            withProperty(ascensionsPref, ascensionBefore),
            withNextResponseRedirectedTo(
                200, "afterlife.php", html("request/test_ascension_jump_gash.html")));

    try (cleanups) {
      // This does a 302 redirect to afterlife.php
      var jumpGash = new GenericRequest("ascend.php?action=ascend&pwd&confirm=on&confirm2=on");
      jumpGash.run();

      // Execute the result of the 302 redirect and get the output which puts us in valhalla
      var afterlife = new GenericRequest("afterlife.php");
      afterlife.run();

      // Confirm our last run breakfast is now -1
      assertThat(breakfastPref, isSetTo(breakfastValhalla));
      // Confirm our ascensions today has incremented
      assertThat(ascensionsPref, isSetTo(ascensionAfter));
    }
  }

  @Test
  void testKarmaUpdates() {
    // Test that our karma is updated correctly when we enter the pearly gates
    String pref = "bankedKarma";
    int karmaBeforehand = 100;
    int karmaAfter = 211;

    // Set our karma beforehand to 100
    var cleanups = withProperty(pref, karmaBeforehand);

    try (cleanups) {
      // Confirm our karma is 100
      assertThat(pref, isSetTo(karmaBeforehand));

      // Execute a request to enter the pearly gates
      var pearlyGates = new GenericRequest("afterlife.php?action=pearlygates");
      pearlyGates.responseText = html("request/test_ascension_enter_valhalla.html");
      pearlyGates.setHasResult(true);
      pearlyGates.processResponse();

      // Confirm our karma is now 211
      assertThat(pref, isSetTo(karmaAfter));
    }
  }

  @Test
  void testSelectPathUnconfirmedUnprocessed() {
    // Set last breakfast to -1 to mark that we've ascended as done in testAscensionsTodayTracked
    var cleanups =
        new Cleanups(withProperty("lastBreakfast", -1), withProperty("bankedKarma", 150));
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    try (cleanups;
        PrintStream out = new PrintStream(ostream, true)) {
      RequestLogger.openCustom(out);

      // Testing to ensure "Are you sure" is not handled
      var ascendRequest =
          new GenericRequest(
              "afterlife.php?action=ascend&asctype=2&whichclass=1&gender=2&whichpath=44&whichsign=3");
      ascendRequest.setHasResult(true);
      ascendRequest.run();

      RequestLogger.closeCustom();
    }

    // Confirm unchanged, we did not actually ascend yet
    String logged = ostream.toString().trim();
    assertEquals("", logged);
  }

  @Test
  void testSelectPathConfirmedProcessed() {
    // Set last breakfast to -1 to mark that we've ascended as done in testAscensionsTodayTracked
    var cleanups =
        new Cleanups(
            withProperty("lastBreakfast", -1),
            withProperty("bankedKarma", 150),
            withNextResponseRedirectedTo(200, "main.php", ""));
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    try (cleanups;
        PrintStream out = new PrintStream(ostream, true)) {
      RequestLogger.openCustom(out);

      // This does a 302 redirect
      var ascendConfirm =
          new GenericRequest(
              "afterlife.php?action=ascend&confirmascend=1&whichsign=3&gender=2&whichclass=27&whichpath=44&asctype=2&nopetok=1");
      ascendConfirm.run();

      RequestLogger.closeCustom();
    }

    String logged = ostream.toString().trim();
    assertThat(
        logged,
        containsString(
            "Ascend as a Normal Female Grey Goo under the Vole sign on a Grey You path, banking 150 Karma."));
  }
}
