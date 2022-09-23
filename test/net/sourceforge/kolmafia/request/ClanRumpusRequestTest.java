package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClanFurniture;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
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
            withProperty("_ballpit", false));

    try (cleanups) {
      new ClanRumpusRequest(ClanRumpusRequest.RequestType.BALLS).run();
      assertThat("_ballpit", isSetTo(true));
    }
  }

  @Test
  void canTrackChipMachineFinished() {
    var builder = new FakeHttpClientBuilder();

    builder.client.addResponse(200, html("request/test_clan_rumpus_chips_used.html"));

    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withClanFurniture(),
            withClanFurniture("Snack Machine"),
            withProperty("_chipBags", 0));

    try (cleanups) {
      new ClanRumpusRequest(ClanRumpusRequest.RequestType.CHIPS).run();
      var requests = builder.client.getRequests();

      assertThat(requests, hasSize(1));
      assertPostRequest(requests.get(0), "/clan_rumpus.php", "preaction=buychips&whichbag=0");
      assertThat("_chipBags", isSetTo(3));
    }
  }
}
