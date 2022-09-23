package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClanRumpusRequestTest {
  @BeforeEach
  public void init() {
    Preferences.saveSettingsToFile = false;
    KoLCharacter.reset("ClanRumpusRequestTest");
    Preferences.reset("ClanRumpusRequestTest");
  }

  @Test
  void ballpitRequestParsesAction() {
    var cleanups =
        new Cleanups(
            withNextResponse(200, html("request/test_clan_ballpit.html")),
            withProperty("_ballpit", ""));

    try (cleanups) {
      new ClanRumpusRequest(ClanRumpusRequest.RequestType.BALLS).run();
      assertThat("_ballpit", isSetTo("true"));
    }
  }
}
