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
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ClanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

  @BeforeEach
  public void init() {
    ClanManager.resetClanId();
  }

  @AfterAll
  public static void afterAll() {
    CLEANUPS.close();
  }

  @Nested
  class Floundry {
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
  }

  @Nested
  class FaxMachine {
    @Test
    void receivingFaxChecksContents() {
      var builder = new FakeHttpClientBuilder();
      builder.client.addResponse(200, html("request/test_clan_fax_receive.html"));
      builder.client.addResponse(200, html("request/test_desc_item_photocopied_mariachi.html"));
      builder.client.addResponse(200, ""); // api.php

      var cleanups = new Cleanups(withHttpClientBuilder(builder), withProperty("photocopyMonster"));

      try (cleanups) {
        new ClanLoungeRequest(ClanLoungeRequest.FAX_MACHINE, ClanLoungeRequest.RECEIVE_FAX).run();
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(
            requests.get(0), "/clan_viplounge.php", "preaction=receivefax&whichfloor=2");
        assertPostRequest(requests.get(1), "/desc_item.php", "whichitem=835898159");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");

        assertThat("photocopyMonster", isSetTo("handsome mariachi"));
      }
    }
  }

  @Nested
  class SwimmingPool {
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

  @Nested
  class Speakeasy {
    private static final AdventureResult GLASS_OF_MILK = ItemPool.get(ItemPool.GLASS_OF_MILK, 1);
    private static final AdventureResult CUP_OF_TEA = ItemPool.get(ItemPool.CUP_OF_TEA, 1);
    private static final AdventureResult THERMOS_OF_WHISKEY =
        ItemPool.get(ItemPool.THERMOS_OF_WHISKEY, 1);
    private static final AdventureResult LUCKY_LINDY = ItemPool.get(ItemPool.LUCKY_LINDY, 1);
    private static final AdventureResult BEES_KNEES = ItemPool.get(ItemPool.BEES_KNEES, 1);
    private static final AdventureResult SOCKDOLLAGER = ItemPool.get(ItemPool.SOCKDOLLAGER, 1);
    private static final AdventureResult ISH_KABIBBLE = ItemPool.get(ItemPool.ISH_KABIBBLE, 1);
    private static final AdventureResult HOT_SOCKS = ItemPool.get(ItemPool.HOT_SOCKS, 1);
    private static final AdventureResult PHONUS_BALONUS = ItemPool.get(ItemPool.PHONUS_BALONUS, 1);
    private static final AdventureResult FLIVVER = ItemPool.get(ItemPool.FLIVVER, 1);
    private static final AdventureResult SLOPPY_JALOPY = ItemPool.get(ItemPool.SLOPPY_JALOPY, 1);

    private static final List<AdventureResult> ALL_SPEAKEASY_DRINKS =
        List.of(
            GLASS_OF_MILK,
            CUP_OF_TEA,
            THERMOS_OF_WHISKEY,
            LUCKY_LINDY,
            BEES_KNEES,
            SOCKDOLLAGER,
            ISH_KABIBBLE,
            HOT_SOCKS,
            PHONUS_BALONUS,
            FLIVVER,
            SLOPPY_JALOPY);

    @Test
    void canParseSpeakeasyDrinks() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withProperty("_speakeasyDrinksDrunk", 0));

      try (cleanups) {
        client.addResponse(200, html("request/test_clan_speakeasy.html"));

        assertThat(ClanLoungeRequest.availableSpeakeasyDrinks, hasSize(0));
        var request = new ClanLoungeRequest(ClanLoungeRequest.SPEAKEASY);
        request.run();
        assertThat(
            ClanLoungeRequest.availableSpeakeasyDrinks, hasSize(ALL_SPEAKEASY_DRINKS.size()));
        for (var drink : ALL_SPEAKEASY_DRINKS) {
          assertTrue(ClanLoungeRequest.hasClanLoungeItem(drink));
        }

        var requests = client.getRequests();

        assertThat(requests, hasSize(greaterThanOrEqualTo(1)));
        assertPostRequest(requests.get(0), "/clan_viplounge.php", "action=speakeasy&whichfloor=2");
      }
    }
  }
}
