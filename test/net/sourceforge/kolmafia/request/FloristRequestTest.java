package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FloristRequestTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("FloristRequestTest");
    Preferences.reset("FloristRequestTest");
  }

  private final String checkPref = "_floristChecked";
  private final String availPref = "floristAvailable";

  @Test
  public void testFloristShouldNotBeAvailable() {
    var cleanups =
        new Cleanups(
            withLevel(10),
            withProperty(checkPref, false),
            withProperty(availPref, false),
            withNextResponse(200, html("request/test_florist_woods_locked.html")));

    try (cleanups) {
      var request = new FloristRequest();
      request.run();

      assertTrue(Preferences.getBoolean(checkPref));
      assertFalse(Preferences.getBoolean(availPref));
    }
  }
  
  @Test
  public void testFloristShouldBeAvailable() {
    var cleanups =
        new Cleanups(
            withLevel(10),
            withProperty(checkPref, false),
            withProperty(availPref, false),
            withNextResponse(200, html("request/test_florist_woods_unlocked_and_available.html")));

    try (cleanups) {
      var request = new FloristRequest();
      request.run();

      assertTrue(Preferences.getBoolean(checkPref));
      assertTrue(Preferences.getBoolean(availPref));
    }
  }
}
