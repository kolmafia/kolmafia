package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;
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

  @Test
  void klawRequestParsesAction() {
    var cleanups =
        new Cleanups(
            withNextResponse(200, html("request/test_clan_klaw.html")),
            withProperty("_klawSummons", 2));

    try (cleanups) {
      var req = new ClanRumpusRequest(RequestType.VISIT);
      req.visitEquipment(3, 3);
      req.run();
      assertThat("_klawSummons", isSetTo(3));
    }
  }
}
