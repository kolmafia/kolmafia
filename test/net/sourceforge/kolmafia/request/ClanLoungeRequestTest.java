package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClan;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ClanLoungeRequest.Action;
import net.sourceforge.kolmafia.request.ClanLoungeRequest.SpeakeasyDrink;
import net.sourceforge.kolmafia.session.ClanManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        new ClanLoungeRequest(Action.FLOUNDRY).run();
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
        new ClanLoungeRequest(Action.FAX_MACHINE, ClanLoungeRequest.RECEIVE_FAX).run();
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

        new ClanLoungeRequest(Action.SWIMMING_POOL, ClanLoungeRequest.SPRINTS).run();
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
    /*
     * 1) For a speakeasy drink to be visible:
     * -  You need a Clan VIP lounge key
     * -  You must be in a clan
     * -  The clan must have a speakeasy
     * -  The drink must be unlocked
     * -  You must have enough Meat to buy it
     * -  You must have drunk fewer than three speakeasy drinks today
     * 2) This package needs to maintain a list of available drinks
     * -  This will be cleared when you login or change clans
     * -  This will be filled when we parse the Clan VIP lounge on login or clan switch
     *    (This satisfies the first four checks)
     * 3) A speakeasy concoction on the usables list has initial = 0 (never in inventory)
     *    and creatable = 0 if the drink isn't on the available list. If it is on that list,
     *    creatable can be 0, 1, 2, or 3, depending on available Meat and # of speakeasy
     *    drinks consumed today.
     *    (This satisfies the last two checks)
     * 4) We need to track speakeasy drinks consumed - _speakeasyDrinksDrunk - and trigger
     *    concoction.resetCalculations() for all speakeasy drinks.
     */

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

    // This loads ClanLoungeRequest and executes static initialization
    private static final Set<SpeakeasyDrink> ALL_SPEAKEASY =
        ClanLoungeRequest.ALL_SPEAKEASY.stream().collect(Collectors.toSet());

    @Test
    void allSpeakeasyDrinksRemainOnUsablesList() {
      var usables = ConcoctionDatabase.getUsables();

      // Initial state after loading
      for (var drink : ALL_SPEAKEASY) {
        assertTrue(usables.contains(drink.getConcoction()));
        assertFalse(ClanLoungeRequest.availableSpeakeasyDrink(drink));
      }

      // Reset Speakeasy, as if in a clan without one
      ClanLoungeRequest.resetSpeakeasy();

      // Initial state still valid
      for (var drink : ALL_SPEAKEASY) {
        assertTrue(usables.contains(drink.getConcoction()));
        assertFalse(ClanLoungeRequest.availableSpeakeasyDrink(drink));
      }
    }

    @Test
    void canParseSpeakeasyDrinks() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups = new Cleanups(withHttpClientBuilder(builder), withClan());

      try (cleanups) {
        client.addResponse(200, html("request/test_clan_speakeasy.html"));

        var request = new ClanLoungeRequest(Action.SPEAKEASY);
        request.run();

        // All Speakeasy concoctions are available
        // All Speakeasy "items" are on ClanLounge collection
        for (var drink : ClanLoungeRequest.ALL_SPEAKEASY) {
          assertTrue(ClanLoungeRequest.availableSpeakeasyDrink(drink));
          assertTrue(ClanLoungeRequest.hasClanLoungeItem(drink.getItem()));
        }

        var requests = client.getRequests();

        assertThat(requests, hasSize(greaterThanOrEqualTo(1)));
        assertPostRequest(requests.get(0), "/clan_viplounge.php", "action=speakeasy&whichfloor=2");
      }
    }

    @Test
    void speakeasyDrinksCanBeDetectedinClanItemList() {
      var cleanups = new Cleanups(withClan());

      try (cleanups) {
        // Add the Clan Speakeasy itself to the lounge
        AdventureResult speakeasy = ItemPool.get(ItemPool.CLAN_SPEAKEASY);
        ClanManager.addToLounge(speakeasy);
        assertTrue(ClanLoungeRequest.hasClanLoungeItem(speakeasy));

        // Add a Lucky Lindy to the Clan Speakeasy as via parsing the lounge
        SpeakeasyDrink drink = SpeakeasyDrink.findName("Lucky Lindy");
        ClanLoungeRequest.addSpeakeasyDrink(drink.getName());

        // It is available and appears as a lounge item
        assertTrue(ClanLoungeRequest.availableSpeakeasyDrink(drink));
        assertTrue(ClanLoungeRequest.hasClanLoungeItem(drink.getItem()));

        // Reset the Speakeasy in the Clan Lounge
        ClanLoungeRequest.resetSpeakeasy();

        // The Speakeasy still appears as a lounge item
        assertTrue(ClanLoungeRequest.hasClanLoungeItem(speakeasy));

        // As do existing drinks
        assertTrue(ClanLoungeRequest.hasClanLoungeItem(drink.getItem()));

        // However, drinks are not actually available
        assertFalse(ClanLoungeRequest.availableSpeakeasyDrink(drink));

        // Given the item, add drink to available list
        assertTrue(ClanLoungeRequest.maybeAddSpeakeasyDrink(drink.getItem()));
        assertTrue(ClanLoungeRequest.availableSpeakeasyDrink(drink));

        // The lounge itself can't be added as a Speakeasy drink
        assertFalse(ClanLoungeRequest.maybeAddSpeakeasyDrink(speakeasy));
      }
    }

    @Test
    void speakeasyDrinksCanBeUnavailable() {
      var cleanups =
          new Cleanups(withClan(), withMeat(10000), withProperty("_speakeasyDrinksDrunk", 0));

      try (cleanups) {
        Concoction luckyLindy = ConcoctionPool.get(LUCKY_LINDY);
        Concoction beesKnees = ConcoctionPool.get(BEES_KNEES);

        // Initially, no Speakeasy drinks are available
        assertFalse(ClanLoungeRequest.availableSpeakeasyDrink(luckyLindy.getName()));
        assertFalse(ClanLoungeRequest.availableSpeakeasyDrink(beesKnees.getName()));

        // They are on usables list ...
        var usables = ConcoctionDatabase.getUsables();
        assertTrue(usables.contains(luckyLindy));
        assertTrue(usables.contains(beesKnees));

        // But they are not "available" - which means they are not
        // visible in the item manager.
        assertEquals(0, luckyLindy.getAvailable());
        assertEquals(0, beesKnees.getAvailable());

        // Add one of the drinks to the Speakeasy
        ClanLoungeRequest.addSpeakeasyDrink(beesKnees.getName());

        // It is now both available and visible.
        assertTrue(ClanLoungeRequest.availableSpeakeasyDrink(beesKnees.getName()));
        assertEquals(3, beesKnees.getAvailable());

        // Note that visibility depends on both how many speakeasy
        // drinks you have consumed today and how much Meat they cost.
        // We will test those with different tests
      }
    }

    @Test
    void speakeasyDrinksCostMeatToBuy() {
      var cleanups =
          new Cleanups(withClan(), withMeat(1000), withProperty("_speakeasyDrinksDrunk", 0));

      try (cleanups) {
        Concoction luckyLindy = ConcoctionPool.get(LUCKY_LINDY);

        // Add Lucky Lindy to the Speakeasy
        ClanLoungeRequest.addSpeakeasyDrink(luckyLindy.getName());

        // It costs 500 Meat, so two are available
        assertTrue(ClanLoungeRequest.availableSpeakeasyDrink(luckyLindy.getName()));
        assertEquals(2, luckyLindy.getAvailable());
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 3", "1, 2", "2, 1", "3, 0"})
    public void speakeasyDrinksAreLimited(int drunk, int available) {
      var cleanups =
          new Cleanups(withClan(), withMeat(10000), withProperty("_speakeasyDrinksDrunk", drunk));

      try (cleanups) {
        Concoction luckyLindy = ConcoctionPool.get(LUCKY_LINDY);

        // Add Lucky Lindy to the Speakeasy
        ClanLoungeRequest.addSpeakeasyDrink(luckyLindy.getName());

        // It costs 500 Meat, we have plenty of Meat
        // However, you can only drink three a day
        assertTrue(ClanLoungeRequest.availableSpeakeasyDrink(luckyLindy.getName()));
        assertEquals(available, luckyLindy.getAvailable());
      }
    }
  }
}
