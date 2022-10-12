package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ClanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClanLoungeRequestTest {
  private static final Cleanups CLEANUPS = new Cleanups();

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ClanLoungeRequestTest");
    Preferences.reset("ClanLoungeRequestTest");
    // Miss some key visits
    CLEANUPS.add(withProperty("_fireworksShop", true));
    CLEANUPS.add(withProperty("_crimboTree", true));
  }

  @AfterAll
  public static void afterAll() {
    CLEANUPS.close();
  }

  @BeforeEach
  public void init() {
    ClanManager.resetClanId();
  }

  @Test
  void floundryRequestParsesLocations() {
    var cleanups =
        new Cleanups(
            withNextResponse(200, html("request/test_clan_floundry.html")),
            withProperty("_floundryCarpLocation", ""),
            withProperty("_floundryCodLocation", ""),
            withProperty("_floundryTroutLocation", ""),
            withProperty("_floundryBassLocation", ""),
            withProperty("_floundryHatchetfishLocation", ""),
            withProperty("_floundryTunaLocation", ""));

    try (cleanups) {
      new ClanLoungeRequest(ClanLoungeRequest.FLOUNDRY).run();
      assertThat("_floundryCarpLocation", isSetTo("Pirates of the Garbage Barges"));
      assertThat("_floundryCodLocation", isSetTo("Thugnderdome"));
      assertThat("_floundryTroutLocation", isSetTo("The Haunted Conservatory"));
      assertThat("_floundryBassLocation", isSetTo("Guano Junction"));
      assertThat("_floundryHatchetfishLocation", isSetTo("The Skeleton Store"));
      assertThat("_floundryTunaLocation", isSetTo("The Oasis"));
    }
  }

  @Test
  void receivingFaxChecksContents() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, html("request/test_clan_fax_receive.html"));
    builder.client.addResponse(200, ""); // Searching a newly found clan
    builder.client.addResponse(200, html("request/test_desc_item_photocopied_mariachi.html"));
    builder.client.addResponse(200, ""); // api.php

    var cleanups = new Cleanups(withHttpClientBuilder(builder), withProperty("photocopyMonster"));

    try (cleanups) {
      new ClanLoungeRequest(ClanLoungeRequest.FAX_MACHINE, ClanLoungeRequest.RECEIVE_FAX).run();
      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(4));
      assertPostRequest(
          requests.get(0), "/clan_viplounge.php", "preaction=receivefax&whichfloor=2");
      assertPostRequest(requests.get(1), "/clan_rumpus.php", "action=click&spot=7");
      assertPostRequest(requests.get(2), "/desc_item.php", "whichitem=835898159");
      assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");

      assertThat("photocopyMonster", isSetTo("handsome mariachi"));
    }
  }

  @Test
  void canTrackSwimming() {
    var builder = new FakeHttpClientBuilder();
    builder.client.addResponse(200, html("request/test_clan_swim_sprints.html"));

    var cleanups =
        new Cleanups(withHttpClientBuilder(builder), withProperty("_olympicSwimmingPool", false));

    try (cleanups) {
      var outputStream = new ByteArrayOutputStream();
      RequestLogger.openCustom(new PrintStream(outputStream));

      new ClanLoungeRequest(ClanLoungeRequest.SWIMMING_POOL, ClanLoungeRequest.SPRINTS).run();
      var requests = builder.client.getRequests();
      assertThat(requests, hasSize(greaterThanOrEqualTo(1)));
      assertPostRequest(
          requests.get(0),
          "/clan_viplounge.php",
          "preaction=goswimming&subaction=submarine&whichfloor=2");

      RequestLogger.closeCustom();

      assertThat("_olympicSwimmingPool", isSetTo(true));
      assertThat(outputStream, hasToString(containsString("You did 234 submarine sprints")));
    }
  }
}
