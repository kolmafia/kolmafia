package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
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
  public void testFloristWoodsLocked() {
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
  public void testFloristNotOwned() {
    var cleanups =
        new Cleanups(
            withLevel(10),
            withProperty(checkPref, false),
            withProperty(availPref, false),
            withNextResponse(200, html("request/test_florist_not_owned.html")));

    try (cleanups) {
      var request = new FloristRequest();
      request.run();

      assertTrue(Preferences.getBoolean(checkPref));
      assertFalse(Preferences.getBoolean(availPref));
    }
  }

  @Test
  public void testFloristAvailable() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withLevel(10),
            withProperty(checkPref, false),
            withProperty(availPref, false));

    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_florist_woods_unlocked_and_available.html"));
      var request = new FloristRequest();
      request.run();

      assertTrue(Preferences.getBoolean(checkPref));
      assertTrue(Preferences.getBoolean(availPref));
    }
  }
}
