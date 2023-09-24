package net.sourceforge.kolmafia.textui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GrandpaRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("GrandpaRequest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("GrandpaRequest");
  }

  @Test
  void askingAboutHierfalGrantsTwinkleVision() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(withHttpClientBuilder(builder), withProperty("hasTwinkleVision", false));

    try (cleanups) {
      client.addResponse(200, html("request/test_grandpa_hierfal.html"));

      String url = "monkeycastle.php?action=grandpastory&topic=hierfal";
      var request = new GenericRequest(url);
      request.run();
      assertTrue(Preferences.getBoolean("hasTwinkleVision"));
    }
  }
}
