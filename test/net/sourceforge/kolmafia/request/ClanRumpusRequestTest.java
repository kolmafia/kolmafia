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
import net.sourceforge.kolmafia.request.ClanRumpusRequest.RequestType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClanRumpusRequestTest {
  @BeforeEach
  public void init() {
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
            // This includes additional furniture to match the test fixture and ensure appropriate
            // tidy-up afterwards
            // We only actually care about the snack machine
            withClanFurniture(
                "Girls of Loathing Calendar",
                "Collection of Self-Help Books",
                "Mr. Klaw \"Skill\" Crane Game",
                "Inspirational Desk Calendar",
                "Wrestling Mat",
                "Snack Machine"),
            withProperty("_chipBags", 0));

    try (cleanups) {
      new ClanRumpusRequest(ClanRumpusRequest.RequestType.CHIPS).run();
      var requests = builder.client.getRequests();

      assertThat(requests, hasSize(1));
      assertPostRequest(requests.get(0), "/clan_rumpus.php", "preaction=buychips&whichbag=0");
      assertThat("_chipBags", isSetTo(3));
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
