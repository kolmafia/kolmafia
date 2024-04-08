package net.sourceforge.kolmafia;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEmptyCampground;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withInebriety;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withKingLiberated;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withLimitMode;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNoEffects;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withRange;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSign;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isStarted;
import static internal.matchers.Quest.isStep;
import static internal.matchers.Quest.isUnstarted;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.LimitMode;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class KoLAdventureValidationTest {

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("KoLAdventure");
    Preferences.reset("KoLAdventure");
    KoLConstants.inventory.clear();
    EquipmentManager.resetEquipment();
  }

  @Nested
  class Overdrunk {
    private static final KoLAdventure WARREN =
        AdventureDatabase.getAdventureByName("The Dire Warren");
    private static final KoLAdventure SHADOW_RIFT =
        AdventureDatabase.getAdventureByName("Shadow Rift (Desert Beach)");

    @Test
    void beingSoberPassesPreValidation() {
      var cleanups = new Cleanups(withInebriety(5), withPath(Path.SHADOWS_OVER_LOATHING));

      try (cleanups) {
        assertThat(WARREN.preValidateAdventure(), is(true));
        assertThat(SHADOW_RIFT.preValidateAdventure(), is(true));
      }
    }

    @Test
    void beingTooDrunkFailsPreValidation() {
      var cleanups = new Cleanups(withInebriety(30), withPath(Path.SHADOWS_OVER_LOATHING));

      try (cleanups) {
        assertThat(WARREN.preValidateAdventure(), is(false));
        assertThat(SHADOW_RIFT.preValidateAdventure(), is(false));
      }
    }

    @ParameterizedTest
    @EnumSource(
        value = Slot.class,
        names = {"OFFHAND", "FAMILIAR"})
    void beingTooDrunkWithAWineglassPassesPreValidation(final Slot slot) {
      var cleanups =
          new Cleanups(
              withInebriety(30),
              withPath(Path.SHADOWS_OVER_LOATHING),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquipped(slot, ItemPool.DRUNKULA_WINEGLASS));

      try (cleanups) {
        assertThat(WARREN.preValidateAdventure(), is(true));
        assertThat(SHADOW_RIFT.preValidateAdventure(), is(true));
      }
    }

    @ParameterizedTest
    @EnumSource(
        value = Slot.class,
        names = {"OFFHAND", "FAMILIAR"})
    void beingTooDrunkWithAWineglassInNonSnarfblatFailsPreValidation(final Slot slot) {
      var cleanups =
          new Cleanups(
              withInebriety(30),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquipped(slot, ItemPool.DRUNKULA_WINEGLASS));

      try (cleanups) {
        assertThat(
            AdventureDatabase.getAdventureByName("The Typical Tavern Cellar")
                .preValidateAdventure(),
            is(false));
      }
    }

    @ParameterizedTest
    @EnumSource(
        value = LimitMode.class,
        names = {"SPELUNKY", "BATMAN"})
    void beingTooDrunkInSomeLimitModesPassesPreValidation(final LimitMode limitMode) {
      var cleanups = new Cleanups(withInebriety(30), withLimitMode(limitMode));

      try (cleanups) {
        assertThat(WARREN.preValidateAdventure(), is(true));
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Trick-or-Treating", "Drunken Stupor"})
    void beingTooDrunkInSomeLocationsPassesPreValidation(final String adventureName) {
      var cleanups = new Cleanups(withInebriety(30));

      try (cleanups) {
        assertThat(AdventureDatabase.getAdventure(adventureName).preValidateAdventure(), is(true));
      }
    }
  }

  @Nested
  class PreValidateAdventure {
    private void checkDayPasses(
        KoLAdventure adventure,
        String place,
        String html,
        boolean perm,
        boolean today,
        String alwaysProperty,
        String todayProperty) {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty(alwaysProperty, perm),
              withProperty(todayProperty, today));
      try (cleanups) {
        var url = "place.php?whichplace=" + place;
        client.addResponse(200, html);

        boolean success = adventure.preValidateAdventure();

        var requests = client.getRequests();
        if (perm || today) {
          // If we know that we have permanent or daily access, pre-validation
          // returns true with no requests
          assertTrue(success);
          assertThat(requests, hasSize(0));
        } else if (html.equals("")) {
          // If we have neither permanent nor daily access and none is visible
          // on the map, pre-validation returns false with one request
          assertFalse(success);
          assertThat(requests, hasSize(1));
          assertPostRequest(requests.get(0), "/place.php", "whichplace=" + place);
        } else {
          // If we have neither permanent nor daily access but the map shows
          // access, pre-validation returns true with one request and sets
          // either permanent or daily access
          assertTrue(success);
          if (!todayProperty.equals("none")) {
            assertTrue(Preferences.getBoolean(todayProperty));
          } else {
            assertTrue(Preferences.getBoolean(alwaysProperty));
          }
          assertThat(requests, hasSize(1));
          assertPostRequest(requests.get(0), "/place.php", "whichplace=" + place);
        }
      }
    }

    private void checkDayPasses(String adventureName, String place, String always, String today) {
      KoLAdventure adventure = AdventureDatabase.getAdventureByName(adventureName);
      var html = html("request/test_visit_" + place + ".html");
      // If we have always access, we don't have today access
      checkDayPasses(adventure, place, html, true, false, always, today);
      // If we don't have always access, we might today access
      if (!today.equals("none")) {
        checkDayPasses(adventure, place, html, false, true, always, today);
      }
      // If we don't have always or today access, we might still have today access
      checkDayPasses(adventure, place, html, false, false, always, today);
      // If we don't have always or today access, we might really not have today access
      checkDayPasses(adventure, place, "", false, false, always, today);
    }

    @ParameterizedTest
    @CsvSource({
      "The Neverending Party, neverendingPartyAlways, _neverendingPartyToday",
      "The Tunnel of L.O.V.E., loveTunnelAvailable, _loveTunnelToday",
      "An Unusually Quiet Barroom Brawl, ownsSpeakeasy, none"
    })
    public void checkDayPassesInTownWrong(String adventureName, String always, String today) {
      checkDayPasses(adventureName, "town_wrong", always, today);
    }

    @ParameterizedTest
    @CsvSource({
      "Investigating a Plaintive Telegram, telegraphOfficeAvailable, _telegraphOfficeToday"
    })
    public void checkDayPassesInTownRight(String adventureName, String always, String today) {
      checkDayPasses(adventureName, "town_right", always, today);
    }

    @ParameterizedTest
    @CsvSource({
      "The Bandit Crossroads, frAlways, _frToday",
      "PirateRealm Island, prAlways, _prToday"
    })
    public void checkDayPassesInMonorail(String adventureName, String always, String today) {
      checkDayPasses(adventureName, "monorail", always, today);
    }

    @ParameterizedTest
    @CsvSource({
      "VYKEA, coldAirportAlways, _coldAirportToday",
      "The SMOOCH Army HQ, hotAirportAlways, _hotAirportToday",
      "The Fun-Guy Mansion, sleazeAirportAlways, _sleazeAirportToday",
      "The Deep Dark Jungle, spookyAirportAlways, _spookyAirportToday",
      "Barf Mountain, stenchAirportAlways, _stenchAirportToday"
    })
    public void checkDayPassesInAirport(String adventureName, String always, String today) {
      checkDayPasses(adventureName, "airport", always, today);
    }

    @ParameterizedTest
    @CsvSource({"Gingerbread Civic Center, gingerbreadCityAvailable, _gingerbreadCityToday"})
    public void checkDayPassesInMountains(String adventureName, String always, String today) {
      checkDayPasses(adventureName, "mountains", always, today);
    }

    @Nested
    class Spacegate {
      private static final KoLAdventure SPACEGATE =
          AdventureDatabase.getAdventureByName("Through the Spacegate");
      private static final String always = "spacegateAlways";
      private static final String today = "_spacegateToday";

      @Test
      public void checkAlwaysAccessForSpacegate() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withProperty(always, true),
                withProperty(today, false));
        try (cleanups) {
          // If we have always access, we're good to go.
          boolean success = SPACEGATE.preValidateAdventure();
          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
          assertTrue(success);
        }
      }

      @Test
      public void checkTodayAccessForSpacegate() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withProperty(always, false),
                withProperty(today, true));
        try (cleanups) {
          // If we have daily access, we're good to go
          boolean success = SPACEGATE.preValidateAdventure();
          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
          assertTrue(success);
        }
      }

      @Test
      public void checkPortableAccessForSpacegate() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withProperty(always, false),
                withProperty(today, false),
                withItem(ItemPool.OPEN_PORTABLE_SPACEGATE));
        try (cleanups) {
          // If we have neither access, but we have an open portable
          // Spacegate,  we actually have daily access.
          boolean success = SPACEGATE.preValidateAdventure();
          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
          assertTrue(Preferences.getBoolean(today));
          assertTrue(success);
        }
      }

      @Test
      public void checkMapAccessForSpacegate() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withProperty(always, false),
                withProperty(today, false));
        try (cleanups) {
          // If we have neither access, but the Spacegate is on the map,
          // we actually have permanent access.
          client.addResponse(200, html("request/test_visit_mountains.html"));
          boolean success = SPACEGATE.preValidateAdventure();
          var requests = client.getRequests();
          assertThat(requests, hasSize(1));
          assertPostRequest(requests.get(0), "/place.php", "whichplace=mountains");
          assertTrue(Preferences.getBoolean(always));
          assertTrue(success);
        }
      }

      @Test
      public void checkNoAccessForSpacegate() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withProperty(always, false),
                withProperty(today, false));
        try (cleanups) {
          // If we have neither access, but the Spacegate is not on the map,
          // we really have no access
          client.addResponse(200, "");
          boolean success = SPACEGATE.preValidateAdventure();
          var requests = client.getRequests();
          assertThat(requests, hasSize(1));
          assertPostRequest(requests.get(0), "/place.php", "whichplace=mountains");
          assertFalse(success);
        }
      }
    }

    @Nested
    class Twitch {
      private static final KoLAdventure BOHEMIAN_PARTY =
          AdventureDatabase.getAdventureByName("An Illicit Bohemian Party");
      private static final String today = "timeTowerAvailable";

      @Test
      public void checkTodayAccessForTwitch() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups = new Cleanups(withHttpClientBuilder(builder), withProperty(today, true));
        try (cleanups) {
          // If we have daily access, we're good to go
          boolean success = BOHEMIAN_PARTY.preValidateAdventure();
          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
          assertTrue(success);
        }
      }

      @Test
      public void checkMapAccessForTwitch() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups = new Cleanups(withHttpClientBuilder(builder), withProperty(today, false));
        try (cleanups) {
          // If we have not verified access, but the Time Twitching Tower is on
          // the map, we have access today.
          client.addResponse(200, html("request/test_visit_town_twitch.html"));
          boolean success = BOHEMIAN_PARTY.preValidateAdventure();
          var requests = client.getRequests();
          assertThat(requests, hasSize(1));
          assertPostRequest(requests.get(0), "/place.php", "whichplace=town");
          assertTrue(Preferences.getBoolean(today));
          assertTrue(success);
        }
      }

      @Test
      public void checkMapAccessForNoTwitch() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups = new Cleanups(withHttpClientBuilder(builder), withProperty(today, false));
        try (cleanups) {
          // If we have not verified access, but the Time Twitching Tower is on
          // the map, we have access today.
          client.addResponse(200, html("request/test_visit_town_no_twitch.html"));
          boolean success = BOHEMIAN_PARTY.preValidateAdventure();
          var requests = client.getRequests();
          assertThat(requests, hasSize(1));
          assertPostRequest(requests.get(0), "/place.php", "whichplace=town");
          assertFalse(Preferences.getBoolean(today));
          assertFalse(success);
        }
      }
    }

    @Nested
    class Speakeasy {
      private static final KoLAdventure QUIET_BRAWL =
          AdventureDatabase.getAdventureByName("An Unusually Quiet Barroom Brawl");
      private static final AdventureResult MILK_CAP = ItemPool.get(ItemPool.MILK_CAP);
      private static final String always = "ownsSpeakeasy";

      @Test
      public void canDetectSpeakeasyThroughQuestItem() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder), withProperty(always, false), withItem(MILK_CAP));
        try (cleanups) {
          assertTrue(QUIET_BRAWL.preValidateAdventure());
          assertTrue(Preferences.getBoolean(always));

          var requests = client.getRequests();
          assertThat(requests, hasSize(0));
        }
      }
    }
  }

  @Nested
  class Exploathing {
    public static Cleanups withTempleUnlocked() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.TEMPLE),
              withQuestProgress(Quest.WORSHIP),
              withProperty("lastTempleUnlock"));
      // This depends on ascension count being set
      KoLCharacter.setTempleUnlocked();
      return cleanups;
    }

    public Cleanups withKingdomOfExploathing() {
      // Set up this test to have all quests appropriately started
      return new Cleanups(
          withPath(Path.KINGDOM_OF_EXPLOATHING),
          withAscensions(1),
          // This is not currently true; we don't actually set the property
          // indicating you unlocked it this ascension. Should we?
          // withTempleUnlocked()
          withQuestProgress(Quest.LARVA, QuestDatabase.STARTED),
          withQuestProgress(Quest.RAT, QuestDatabase.STARTED),
          withQuestProgress(Quest.BAT, QuestDatabase.STARTED),
          withQuestProgress(Quest.GOBLIN, "step1"),
          withQuestProgress(Quest.FRIAR, QuestDatabase.STARTED),
          withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
          withProperty("cyrptAlcoveEvilness", 13),
          withProperty("cyrptCrannyEvilness", 13),
          withProperty("cyrptNicheEvilness", 13),
          withProperty("cyrptNookEvilness", 13),
          withProperty("cyrptTotalEvilness", 52),
          withQuestProgress(Quest.TRAPPER, QuestDatabase.STARTED),
          withQuestProgress(Quest.TOPPING, QuestDatabase.STARTED),
          withQuestProgress(Quest.GARBAGE, "step7"),
          withQuestProgress(Quest.BLACK, QuestDatabase.STARTED),
          withQuestProgress(Quest.MACGUFFIN, QuestDatabase.STARTED),
          withQuestProgress(Quest.HIPPY_FRAT, QuestDatabase.STARTED));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "The Sleazy Back Alley",
          "The Haunted Pantry",
          "The Outskirts of Cobb's Knob",
          "The Daily Dungeon",
          "The Spooky Forest",
          "The Hidden Temple",
          "The Typical Tavern Cellar",
          "A Barroom Brawl",
          "The Bat Hole Entrance",
          // Need stench resistance +1
          // "Guano Junction",
          "Cobb's Knob Barracks",
          "Cobb's Knob Kitchens",
          "Cobb's Knob Harem",
          "Cobb's Knob Treasury",
          "The Dark Neck of the Woods",
          "The Dark Heart of the Woods",
          "The Dark Elbow of the Woods",
          "The Defiled Nook",
          "The Defiled Cranny",
          "The Defiled Alcove",
          "The Defiled Niche",
          // Need to talk to Trapper
          // "Itznotyerzitz Mine",
          // "The Goatlet",
          "The Smut Orc Logging Camp",
          "The Castle in the Clouds in the Sky (Basement)",
          "The Hole in the Sky",
          "The Black Forest",
          "The Exploaded Battlefield",
          "The Invader"
        })
    public void testInitiallyOpenAdventures(String adventureName) {
      var cleanups = withKingdomOfExploathing();
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName(adventureName);
        assertTrue(area.canAdventure());
      }
    }

    @Test
    public void thatMarketZonesAvailableWithItem() {
      var cleanups =
          new Cleanups(
              withKingdomOfExploathing(),
              withItem("map to a hidden booze cache"),
              withItem("hypnotic breadcrumbs"),
              withItem("bone with a price tag on it"));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName("The Overgrown Lot");
        assertTrue(area.canAdventure());
        area = AdventureDatabase.getAdventureByName("Madness Bakery");
        assertTrue(area.canAdventure());
        area = AdventureDatabase.getAdventureByName("The Skeleton Store");
        assertTrue(area.canAdventure());
      }
    }

    @Test
    public void thatMarketZonesAvailableIfUnlocked() {
      var cleanups =
          new Cleanups(
              withKingdomOfExploathing(),
              withProperty("overgrownLotAvailable", true),
              withProperty("madnessBakeryAvailable", true),
              withProperty("skeletonStoreAvailable", true));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName("The Overgrown Lot");
        assertTrue(area.canAdventure());
        area = AdventureDatabase.getAdventureByName("Madness Bakery");
        assertTrue(area.canAdventure());
        area = AdventureDatabase.getAdventureByName("The Skeleton Store");
        assertTrue(area.canAdventure());
      }
    }

    @Test
    public void thatMarketZonesNotAvailableViaQuest() {
      var cleanups =
          new Cleanups(
              withKingdomOfExploathing(),
              withProperty("overgrownLotAvailable", false),
              withProperty("madnessBakeryAvailable", false),
              withProperty("skeletonStoreAvailable", false));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName("The Overgrown Lot");
        assertFalse(area.canAdventure());
        area = AdventureDatabase.getAdventureByName("Madness Bakery");
        assertFalse(area.canAdventure());
        area = AdventureDatabase.getAdventureByName("The Skeleton Store");
        assertFalse(area.canAdventure());
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "The Degrassi Knoll Restroom",
          "The Degrassi Knoll Bakery",
          "The Degrassi Knoll Gym",
          "The Degrassi Knoll Garage"
        })
    public void thatHostileKnollUnavailable(String adventureName) {
      var cleanups =
          new Cleanups(
              withKingdomOfExploathing(),
              // Paco will give us the quest, but we cannot fulfill it
              withQuestProgress(Quest.MEATCAR, QuestDatabase.STARTED));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName(adventureName);
        assertFalse(area.canAdventure());
      }
    }

    @Test
    public void cannotVisitMushroomGarden() {
      var cleanups = withKingdomOfExploathing();
      try (cleanups) {
        // For some reason? It's visible in your campground.
        var area = AdventureDatabase.getAdventureByName("Your Mushroom Garden");
        assertFalse(area.canAdventure());
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"South of the Border", "The Shore, Inc. Travel Agency", "Kokomo Resort"})
    public void testUnavailableBeachAdventures(String adventureName) {
      var cleanups =
          new Cleanups(
              withKingdomOfExploathing(),
              // You can use a Kokomo Resort Pass, but no dice
              withEffect("Tropical Contact High", 10));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName(adventureName);
        assertFalse(area.canAdventure());
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "The \"Fun\" House",
          "The Unquiet Garves",
          "The VERY Unquiet Garves",
          "The Penultimate Fantasy Airship"
        })
    public void testUnavailablePlainsAdventures(String adventureName) {
      var cleanups =
          new Cleanups(
              withKingdomOfExploathing(),
              // OCG will give us the quest, but we cannot fulfill it
              withQuestProgress(Quest.EGO, QuestDatabase.STARTED),
              // The VERY Unquiet garves are not available
              withQuestProgress(Quest.CYRPT, QuestDatabase.FINISHED));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName(adventureName);
        assertFalse(area.canAdventure());
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          "Noob Cave",
          "The Dire Warren",
          // Even after finishing TOPPING quest
          "The Valley of Rof L'm Fao",
          // You start out with the Letter for Melvign the Gnome.  Using it
          // redirects to place.php?whichplace=mountains&action=mts_melvin
          // which redirects to place.php?whichplace=exploathing
          "The Thinknerd Warehouse"
        })
    public void testUnavailableMountainAdventures(String adventureName) {
      var cleanups =
          new Cleanups(
              withKingdomOfExploathing(), withQuestProgress(Quest.TOPPING, QuestDatabase.FINISHED));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName(adventureName);
        assertFalse(area.canAdventure());
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
          // You cannot get the quest from Paco, but talking to Mr. Alarm
          // won't give you access either
          "Whitey's Grove"
          // You can't get a Drip harness
          // "The Dripping Trees",
          // "The Dripping Hall"
        })
    public void testUnavailableWoodsAdventures(String adventureName) {
      var cleanups =
          new Cleanups(withKingdomOfExploathing(), withQuestProgress(Quest.PALINDOME, "step3"));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName(adventureName);
        assertFalse(area.canAdventure());
      }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"The Haiku Dungeon", "The Limerick Dungeon", "The Enormous Greater-Than Sign"})
    public void testUnavailableDungeonAdventures(String adventureName) {
      var cleanups = withKingdomOfExploathing();
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName(adventureName);
        assertFalse(area.canAdventure());
      }
    }

    @Test
    public void thatKnollIsUnavailable() {
      var cleanups = new Cleanups(withKingdomOfExploathing(), withSign(ZodiacSign.VOLE));
      try (cleanups) {
        // This applies to adventure zones and other features
        assertFalse(KoLCharacter.knollAvailable());
      }
    }

    @Test
    public void thatCanadiaIsUnavailable() {
      var cleanups = new Cleanups(withKingdomOfExploathing(), withSign(ZodiacSign.OPOSSUM));
      try (cleanups) {
        // This applies to adventure zones and other features
        assertFalse(KoLCharacter.canadiaAvailable());
        var area = AdventureDatabase.getAdventureByName("Outskirts of Camp Logging Camp");
        assertFalse(area.canAdventure());
        area = AdventureDatabase.getAdventureByName("Camp Logging Camp");
        assertFalse(area.canAdventure());
      }
    }

    @Test
    public void thatGnomadsAreUnavailable() {
      var cleanups = new Cleanups(withKingdomOfExploathing(), withSign(ZodiacSign.OPOSSUM));
      try (cleanups) {
        // This applies to adventure zones and other features
        assertFalse(KoLCharacter.gnomadsAvailable());
        var area = AdventureDatabase.getAdventureByName("Thugnderdome");
        assertFalse(area.canAdventure());
      }
    }

    @ParameterizedTest
    @CsvSource({
      "The Fun-Guy Mansion, sleazeAirportAlways, false",
      "The Deep Dark Jungle, spookyAirportAlways, false",
      "The Toxic Teacups, stenchAirportAlways, false",
      "The SMOOCH Army HQ, hotAirportAlways, false",
      "VYKEA, coldAirportAlways, false",
      "Gingerbread Civic Center, gingerbreadCityAvailable, false",
      "Investigating a Plaintive Telegram, telegraphOfficeAvailable, true",
      "The Neverending Party, neverendingPartyAlways, true",
      "The Bandit Crossroads, frAlways, true",
      "Sailing the PirateRealm Seas, prAlways, true",
      "The Tunnel of L.O.V.E., loveTunnelAvailable, true",
      "Through the Spacegate, spacegateAlways, false"
    })
    public void preValidateIOTMZones(String adventureName, String always, boolean check) {
      var cleanups =
          new Cleanups(withPath(Path.KINGDOM_OF_EXPLOATHING), withProperty(always, true));
      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName(adventureName);
        if (check) {
          assertTrue(area.preValidateAdventure());
        } else {
          assertFalse(area.preValidateAdventure());
        }
      }
    }

    @Test
    public void thatExploathingAftercoreWorks() {
      // In Kingdom of Exploathing:
      //
      // - The Hidden Temple was open immediately and you did not need to open
      //   it in one of the usual ways.
      // - You do not have a Meatcar; you might have been given the quest to
      //   build one, but the Knoll zones were not available
      // - You did not plant a beanstalk and go through the Penultimate Fantasy
      //   Airship. In fact, the Airship was not available.
      // - You do not have a S.O.C.K.; you had immediate access to the Giant
      //   Castle without having to go through the Airship.
      // - You may or may not have made a steam-powered rocketship; you had
      //   access to the Hole in the Sky without needing one
      // - You do not have access to the Mysterious Island; you did not need
      //   (and could not get) a dinghy to do the HIPPY_FRAT quest
      //
      // In aftercore, you still have none of those objects, but you retain
      // access to a lot of adventure zones that normally require them.
      //
      // Therefore, we'll test this without said objects but with Quest
      // progress that indicates you had to have been through various
      // unavailable areas - that are now available.

      var cleanups =
          new Cleanups(
              // This is aftercore
              withAscensions(2),
              withKingLiberated(),
              withProperty("lastDesertUnlock"),
              // Assume you took the quest from Paco - even though you could
              // not progress in it.
              withQuestProgress(Quest.MEATCAR, QuestDatabase.STARTED),
              // Starting the Cyrpt normally gives to access to the Unquiet Garves/
              // Finishing it gives you the VERY Unquiet Garves
              withQuestProgress(Quest.CYRPT, QuestDatabase.FINISHED),
              // Finishing the Garbage quest normally requires a beanstalk and
              // the Penultimate Fantasy Airship
              withQuestProgress(Quest.GARBAGE, QuestDatabase.FINISHED),
              // Finishing with the Highland Lord opens the The Valley of Rof
              // L'm Fao
              withQuestProgress(Quest.TOPPING, QuestDatabase.FINISHED),
              // You need to adventure through the Hidden Temple
              withQuestProgress(Quest.WORSHIP, QuestDatabase.FINISHED),
              // We made wet stunt nut stew the hard way
              withQuestProgress(Quest.PALINDOME, QuestDatabase.FINISHED),
              // In order to get to the desert, you needed beach access
              withQuestProgress(Quest.DESERT, QuestDatabase.FINISHED),
              // You fought hippies and fratboys - but not on the Island
              withQuestProgress(Quest.HIPPY_FRAT, QuestDatabase.FINISHED),
              // This is a Canadia sign and therefore has a hostile Knoll.
              withSign(ZodiacSign.OPOSSUM));
      try (cleanups) {
        // Paco gave us the quest and opened the Knoll.
        var area = AdventureDatabase.getAdventureByName("The Degrassi Knoll Garage");
        assertTrue(area.canAdventure());

        // Everything in the Misspelled Cemetary is available
        area = AdventureDatabase.getAdventureByName("The Unquiet Garves");
        assertTrue(area.canAdventure());
        area = AdventureDatabase.getAdventureByName("The VERY Unquiet Garves");
        assertTrue(area.canAdventure());

        // The Airship is available - although we did not plant a beanstalk.
        // (Fun fact: in aftercore, you can see a beanstalk in the Plains.)
        area = AdventureDatabase.getAdventureByName("The Penultimate Fantasy Airship");
        assertTrue(area.canAdventure());

        // The Giant Castle is available - although we did not get a S.O.C.K.
        area =
            AdventureDatabase.getAdventureByName("The Castle in the Clouds in the Sky (Top Floor)");
        assertTrue(area.canAdventure());

        // The Hole in the Sky is NOT available without a steam-powered model rocketship
        area = AdventureDatabase.getAdventureByName("The Hole in the Sky");
        assertFalse(area.canAdventure());

        // Bad Spelling Land is available
        area = AdventureDatabase.getAdventureByName("The Valley of Rof L'm Fao");
        assertTrue(area.canAdventure());

        // We can make wet stew more easily now.
        area = AdventureDatabase.getAdventureByName("Whitey's Grove");
        assertTrue(area.canAdventure());

        // The Hidden Temple is still available
        area = AdventureDatabase.getAdventureByName("The Hidden Temple");
        assertTrue(area.canAdventure());

        // Beach zones are available
        area = AdventureDatabase.getAdventureByName("The Shore, Inc. Travel Agency");
        assertTrue(area.canAdventure());

        // The Island remains unavailable
        area = AdventureDatabase.getAdventureByName("The Obligatory Pirate's Cove");
        assertFalse(area.canAdventure());
      }
    }
  }

  @Nested
  class Standard {
    @Test
    public void restrictedItemZonesNotAllowedUnderStandard() {
      var cleanups = new Cleanups(withRestricted(true));
      try (cleanups) {
        // From the tiny bottle of absinthe - a very old item
        KoLAdventure area = AdventureDatabase.getAdventureByName("The Stately Pleasure Dome");
        assertFalse(area.canAdventure());
      }
    }

    @Test
    public void nonItemZonesAllowedUnderStandard() {
      var cleanups = new Cleanups(withRestricted(true));
      try (cleanups) {
        KoLAdventure area = AdventureDatabase.getAdventureByName("The Outskirts of Cobb's Knob");
        assertTrue(area.canAdventure());
      }
    }
  }

  @Nested
  class BugbearInvasion {
    private static final KoLAdventure MEDBAY = AdventureDatabase.getAdventureByName("Medbay");
    private static final KoLAdventure WASTE_PROCESSING =
        AdventureDatabase.getAdventureByName("Waste Processing");
    private static final KoLAdventure SONAR = AdventureDatabase.getAdventureByName("Sonar");
    private static final KoLAdventure SCIENCE_LAB =
        AdventureDatabase.getAdventureByName("Science Lab");
    private static final KoLAdventure MORGUE = AdventureDatabase.getAdventureByName("Morgue");
    private static final KoLAdventure SPECIAL_OPS =
        AdventureDatabase.getAdventureByName("Special Ops");
    private static final KoLAdventure ENGINEERING =
        AdventureDatabase.getAdventureByName("Engineering");
    private static final KoLAdventure NAVIGATION =
        AdventureDatabase.getAdventureByName("Navigation");
    private static final KoLAdventure GALLEY = AdventureDatabase.getAdventureByName("Galley");

    @Test
    public void cannotVisitMothershipUnlessBugbearInvasion() {
      var cleanups = new Cleanups(withPath(Path.NONE));
      try (cleanups) {
        assertFalse(MEDBAY.canAdventure());
        assertFalse(WASTE_PROCESSING.canAdventure());
        assertFalse(SONAR.canAdventure());
        assertFalse(SCIENCE_LAB.canAdventure());
        assertFalse(MORGUE.canAdventure());
        assertFalse(SPECIAL_OPS.canAdventure());
        assertFalse(ENGINEERING.canAdventure());
        assertFalse(NAVIGATION.canAdventure());
        assertFalse(GALLEY.canAdventure());
      }
    }

    @Test
    public void canVisitMothershipZonesOnlyIfOpen() {
      var cleanups =
          new Cleanups(
              withPath(Path.BUGBEAR_INVASION),
              withProperty("statusMedbay", "cleared"),
              withProperty("statusWasteProcessing", "open"),
              withProperty("statusSonar", "2"),
              withProperty("statusScienceLab", "unlocked"),
              withProperty("statusMorgue", "unlocked"),
              withProperty("statusSpecialOps", "0"),
              withProperty("statusEngineering", "7"),
              withProperty("statusNavigation", "unlocked"),
              withProperty("statusGalley", "0"));
      try (cleanups) {
        assertFalse(MEDBAY.canAdventure());
        assertTrue(WASTE_PROCESSING.canAdventure());
        assertFalse(SONAR.canAdventure());
        assertFalse(SCIENCE_LAB.canAdventure());
        assertFalse(MORGUE.canAdventure());
        assertFalse(SPECIAL_OPS.canAdventure());
        assertFalse(ENGINEERING.canAdventure());
        assertFalse(NAVIGATION.canAdventure());
        assertFalse(GALLEY.canAdventure());
      }
    }
  }

  @Nested
  class AirportCharters {

    // Simplify looking up the KoLAdventure based on adventure ID.
    private static final Map<Integer, KoLAdventure> sleazeZones = new HashMap<>();
    private static final Map<Integer, KoLAdventure> spookyZones = new HashMap<>();
    private static final Map<Integer, KoLAdventure> stenchZones = new HashMap<>();
    private static final Map<Integer, KoLAdventure> hotZones = new HashMap<>();
    private static final Map<Integer, KoLAdventure> coldZones = new HashMap<>();

    static {
      sleazeZones.put(
          AdventurePool.FUN_GUY_MANSION,
          AdventureDatabase.getAdventureByName("The Fun-Guy Mansion"));
      sleazeZones.put(
          AdventurePool.SLOPPY_SECONDS_DINER,
          AdventureDatabase.getAdventureByName("Sloppy Seconds Diner"));
      sleazeZones.put(
          AdventurePool.YACHT, AdventureDatabase.getAdventureByName("The Sunken Party Yacht"));

      spookyZones.put(
          AdventurePool.DR_WEIRDEAUX,
          AdventureDatabase.getAdventureByName("The Mansion of Dr. Weirdeaux"));
      spookyZones.put(
          AdventurePool.SECRET_GOVERNMENT_LAB,
          AdventureDatabase.getAdventureByName("The Secret Government Laboratory"));
      spookyZones.put(
          AdventurePool.DEEP_DARK_JUNGLE,
          AdventureDatabase.getAdventureByName("The Deep Dark Jungle"));

      stenchZones.put(
          AdventurePool.BARF_MOUNTAIN, AdventureDatabase.getAdventureByName("Barf Mountain"));
      stenchZones.put(
          AdventurePool.GARBAGE_BARGES,
          AdventureDatabase.getAdventureByName("Pirates of the Garbage Barges"));
      stenchZones.put(
          AdventurePool.TOXIC_TEACUPS, AdventureDatabase.getAdventureByName("The Toxic Teacups"));
      stenchZones.put(
          AdventurePool.LIQUID_WASTE_SLUICE,
          AdventureDatabase.getAdventureByName(
              "Uncle Gator's Country Fun-Time Liquid Waste Sluice"));

      hotZones.put(
          AdventurePool.SMOOCH_ARMY_HQ, AdventureDatabase.getAdventureByName("The SMOOCH Army HQ"));
      hotZones.put(
          AdventurePool.VELVET_GOLD_MINE,
          AdventureDatabase.getAdventureByName("The Velvet / Gold Mine"));
      hotZones.put(
          AdventurePool.LAVACO_LAMP_FACTORY,
          AdventureDatabase.getAdventureByName("LavaCo&trade; Lamp Factory"));
      hotZones.put(
          AdventurePool.BUBBLIN_CALDERA,
          AdventureDatabase.getAdventureByName("The Bubblin' Caldera"));

      coldZones.put(AdventurePool.ICE_HOTEL, AdventureDatabase.getAdventureByName("The Ice Hotel"));
      coldZones.put(AdventurePool.VYKEA, AdventureDatabase.getAdventureByName("VYKEA"));
      coldZones.put(AdventurePool.ICE_HOLE, AdventureDatabase.getAdventureByName("The Ice Hole"));
    }

    @AfterAll
    public static void afterAll() {
      sleazeZones.clear();
      spookyZones.clear();
      stenchZones.clear();
      hotZones.clear();
      coldZones.clear();
    }

    private void testElementalZoneNoAccess(Map<Integer, KoLAdventure> zones) {
      for (Integer key : zones.keySet()) {
        assertFalse(zones.get(key).canAdventure());
      }
    }

    private void testElementalZoneWithAccess(Map<Integer, KoLAdventure> zones, String property) {
      var cleanups = new Cleanups(withProperty(property, true));
      try (cleanups) {
        for (Integer key : zones.keySet()) {
          assertTrue(zones.get(key).canAdventure());
        }
      }
    }

    @Test
    public void cannotVisitElementalZonesWithoutAccess() {
      testElementalZoneNoAccess(sleazeZones);
      testElementalZoneNoAccess(spookyZones);
      testElementalZoneNoAccess(stenchZones);
      testElementalZoneNoAccess(hotZones);
      testElementalZoneNoAccess(coldZones);
    }

    @Test
    public void canVisitElementalZonesWithAllAccess() {
      testElementalZoneWithAccess(sleazeZones, "sleazeAirportAlways");
      testElementalZoneWithAccess(spookyZones, "spookyAirportAlways");
      testElementalZoneWithAccess(stenchZones, "stenchAirportAlways");
      testElementalZoneWithAccess(hotZones, "hotAirportAlways");
      testElementalZoneWithAccess(coldZones, "coldAirportAlways");
    }

    @Test
    public void canVisitElementalZonesWithDailyAccess() {
      testElementalZoneWithAccess(sleazeZones, "_sleazeAirportToday");
      testElementalZoneWithAccess(spookyZones, "_spookyAirportToday");
      testElementalZoneWithAccess(stenchZones, "_stenchAirportToday");
      testElementalZoneWithAccess(hotZones, "_hotAirportToday");
      testElementalZoneWithAccess(coldZones, "_coldAirportToday");
    }
  }

  @Nested
  class Grimstone {
    @Nested
    class Wolf {
      private static final KoLAdventure GYM =
          AdventureDatabase.getAdventureByName("The Inner Wolf Gym");
      private static final KoLAdventure UNLEASH =
          AdventureDatabase.getAdventureByName("Unleash Your Inner Wolf");

      @Test
      public void mustBeInWolfTale() {
        var cleanups = new Cleanups(withProperty("grimstoneMaskPath", "none"));
        try (cleanups) {
          assertFalse(GYM.canAdventure());
          assertFalse(UNLEASH.canAdventure());
        }
      }

      @Test
      public void mustHaveTimeLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "wolf"), withProperty("wolfTurnsUsed", 30));
        try (cleanups) {
          assertFalse(GYM.canAdventure());
          assertFalse(UNLEASH.canAdventure());
        }
      }

      @Test
      public void canTrainOrUnleashEarly() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "wolf"), withProperty("wolfTurnsUsed", 24));
        try (cleanups) {
          assertTrue(GYM.canAdventure());
          assertTrue(UNLEASH.canAdventure());
        }
      }

      @ParameterizedTest
      @ValueSource(ints = {25, 26})
      public void mustTrainNearEnd(int turns) {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "wolf"), withProperty("wolfTurnsUsed", turns));
        try (cleanups) {
          assertTrue(GYM.canAdventure());
          assertFalse(UNLEASH.canAdventure());
        }
      }

      @Test
      public void mustUnleashAtEnd() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "wolf"), withProperty("wolfTurnsUsed", 27));
        try (cleanups) {
          assertFalse(GYM.canAdventure());
          assertTrue(UNLEASH.canAdventure());
        }
      }
    }

    @Nested
    class Hare {
      private static final KoLAdventure I911 =
          AdventureDatabase.getAdventureByName("A Deserted Stretch of I-911");

      @Test
      public void mustBeInHareTale() {
        var cleanups = new Cleanups(withProperty("grimstoneMaskPath", "none"));
        try (cleanups) {
          assertFalse(I911.canAdventure());
        }
      }

      @Test
      public void mustHaveTurnsLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "hare"), withProperty("hareTurnsUsed", 30));
        try (cleanups) {
          assertFalse(I911.canAdventure());
        }
      }

      @Test
      public void canAdventureWithTurnsLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "hare"), withProperty("hareTurnsUsed", 29));
        try (cleanups) {
          assertTrue(I911.canAdventure());
        }
      }

      @Test
      public void gainingHareBrainedSetsTurnsUsed() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "hare"),
                withProperty("hareTurnsUsed", 0),
                withNoEffects());
        try (cleanups) {
          assertTrue(I911.canAdventure());
          assertThat("hareTurnsUsed", isSetTo(0));
          ResultProcessor.processResult(true, EffectPool.get(EffectPool.HARE_BRAINED, 10));
          assertThat("hareTurnsUsed", isSetTo(20));
        }
      }
    }

    @Nested
    class Stepmother {
      private static final KoLAdventure BALLROOM =
          AdventureDatabase.getAdventureByName("The Prince's Dance Floor");

      @Test
      public void mustBeInStepmotherTale() {
        var cleanups = new Cleanups(withProperty("grimstoneMaskPath", "none"));
        try (cleanups) {
          assertFalse(BALLROOM.canAdventure());
        }
      }

      @Test
      public void mustHaveTimeLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "stepmother"),
                withProperty("cinderellaMinutesToMidnight", 0));
        try (cleanups) {
          assertFalse(BALLROOM.canAdventure());
        }
      }

      @Test
      public void canAdventureWithTimeLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "stepmother"),
                withProperty("cinderellaMinutesToMidnight", 1));
        try (cleanups) {
          assertTrue(BALLROOM.canAdventure());
        }
      }
    }

    @Nested
    class Gnome {
      private static final KoLAdventure VILLAGE =
          AdventureDatabase.getAdventureByName("Ye Olde Medievale Villagee");

      @Test
      public void mustBeInGnomeTale() {
        var cleanups = new Cleanups(withProperty("grimstoneMaskPath", "none"));
        try (cleanups) {
          assertFalse(VILLAGE.canAdventure());
        }
      }

      @Test
      public void mustHaveTimeLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "gnome"),
                withProperty("rumpelstiltskinTurnsUsed", 30));
        try (cleanups) {
          assertFalse(VILLAGE.canAdventure());
        }
      }

      @Test
      public void canAdventureWithTimeLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "gnome"),
                withProperty("rumpelstiltskinTurnsUsed", 5));
        try (cleanups) {
          assertTrue(VILLAGE.canAdventure());
        }
      }
    }

    @Nested
    class CandyWitch {
      private static final KoLAdventure GUMDROP_FOREST =
          AdventureDatabase.getAdventureByName("Gumdrop Forest");

      @Test
      public void mustBeInCandyWitchTale() {
        var cleanups = new Cleanups(withProperty("grimstoneMaskPath", "none"));
        try (cleanups) {
          assertFalse(GUMDROP_FOREST.canAdventure());
        }
      }

      @Test
      public void mustHaveTimeLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "witch"),
                withProperty("candyWitchTurnsUsed", 30));
        try (cleanups) {
          assertFalse(GUMDROP_FOREST.canAdventure());
        }
      }

      @Test
      public void canAdventureWithTimeLeft() {
        var cleanups =
            new Cleanups(
                withProperty("grimstoneMaskPath", "witch"), withProperty("candyWitchTurnsUsed", 5));
        try (cleanups) {
          assertTrue(GUMDROP_FOREST.canAdventure());
        }
      }
    }
  }

  @Nested
  class Memories {
    private static final KoLAdventure PRIMORDIAL_SOUP =
        AdventureDatabase.getAdventureByName("The Primordial Soup");
    private static AdventureResult EMPTY_AGUA_DE_VIDA_BOTTLE =
        ItemPool.get(ItemPool.EMPTY_AGUA_DE_VIDA_BOTTLE);

    @Test
    public void mustHaveEmptyAguaDeVidaBottle() {
      var cleanups = new Cleanups();
      try (cleanups) {
        assertFalse(PRIMORDIAL_SOUP.canAdventure());
      }
    }

    @Test
    public void canAdventureWithBottleInInventory() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withItem(EMPTY_AGUA_DE_VIDA_BOTTLE));
      try (cleanups) {
        assertTrue(PRIMORDIAL_SOUP.canAdventure());
        assertTrue(PRIMORDIAL_SOUP.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canRetrieveBottleAndAdventure() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItemInCloset(EMPTY_AGUA_DE_VIDA_BOTTLE),
              withProperty("autoSatisfyWithCloset", true));
      try (cleanups) {
        client.addResponse(200, html("request/test_uncloset_empty_agua_bottle.html"));

        assertTrue(PRIMORDIAL_SOUP.canAdventure());
        assertTrue(PRIMORDIAL_SOUP.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertGetRequest(
            requests.get(0), "/inventory.php", "action=closetpull&ajax=1&whichitem=4130&qty=1");
      }
    }
  }

  @Nested
  class PirateRealm {
    private static final KoLAdventure RED_ROGERS_FORTRESS =
        AdventureDatabase.getAdventureByName("Red Roger's Fortress");
    private static final KoLAdventure BATTLE_ISLAND =
        AdventureDatabase.getAdventureByName("Battle Island");
    private static final KoLAdventure PIRATEREALM_ISLAND =
        AdventureDatabase.getAdventureByName("PirateRealm Island");
    private static final KoLAdventure SAILING =
        AdventureDatabase.getAdventureByName("Sailing the PirateRealm Seas");

    @Test
    public void properlyChecksLastIsland() {
      var cleanups =
          new Cleanups(
              withProperty("_lastPirateRealmIsland", "Battle Island"),
              withProperty("prAlways", true));
      try (cleanups) {
        assertFalse(RED_ROGERS_FORTRESS.canAdventure());
        assertTrue(BATTLE_ISLAND.canAdventure());
        assertTrue(PIRATEREALM_ISLAND.canAdventure());
      }
    }

    @Test
    public void canAlwaysSail() {
      var cleanups =
          new Cleanups(
              withProperty("_lastPirateRealmIsland", "Glass Island"),
              withProperty("prAlways", true));
      try (cleanups) {
        assertTrue(SAILING.canAdventure());
      }
    }
  }

  @Nested
  class Astral {
    private static final KoLAdventure BAD_TRIP =
        AdventureDatabase.getAdventureByName("An Incredibly Strange Place (Bad Trip)");
    private static final KoLAdventure MEDIOCRE_TRIP =
        AdventureDatabase.getAdventureByName("An Incredibly Strange Place (Mediocre Trip)");
    private static final KoLAdventure GREAT_TRIP =
        AdventureDatabase.getAdventureByName("An Incredibly Strange Place (Great Trip)");
    private static AdventureResult ASTRAL_MUSHROOM = ItemPool.get(ItemPool.ASTRAL_MUSHROOM, 1);
    private static AdventureResult HALF_ASTRAL = EffectPool.get(EffectPool.HALF_ASTRAL);

    @Test
    public void mustHaveAstralMushroomOrHalfAstral() {
      var cleanups = new Cleanups(withLimitMode(LimitMode.NONE));
      try (cleanups) {
        assertFalse(BAD_TRIP.canAdventure());
        assertFalse(MEDIOCRE_TRIP.canAdventure());
        assertFalse(GREAT_TRIP.canAdventure());
      }
    }

    @Test
    public void canAdventureIfHalfAstralWithTripSelected() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withEffect("Half-Astral", 5),
              withProperty("currentAstralTrip", "Great Trip"),
              withLimitMode(LimitMode.ASTRAL));
      try (cleanups) {
        assertFalse(BAD_TRIP.canAdventure());
        assertFalse(MEDIOCRE_TRIP.canAdventure());
        assertTrue(GREAT_TRIP.canAdventure());
        assertTrue(GREAT_TRIP.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureIfHalfAstralWithTripUnSelected() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withEffect("Half-Astral", 5),
              withProperty("currentAstralTrip", ""),
              withLimitMode(LimitMode.ASTRAL),
              withPasswordHash("astral"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_visit_astral_travel_agent.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_choose_great_trip.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(BAD_TRIP.canAdventure());
        assertTrue(MEDIOCRE_TRIP.canAdventure());
        assertTrue(GREAT_TRIP.canAdventure());
        assertTrue(GREAT_TRIP.prepareForAdventure());
        assertThat(Preferences.getString("currentAstralTrip"), is("Great Trip"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));

        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=97&pwd=astral");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=71&option=3&pwd=astral");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canAdventureWithAstralMushroom() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ASTRAL_MUSHROOM),
              withNoEffects(),
              withProperty("currentAstralTrip", ""),
              withLimitMode(LimitMode.NONE),
              withPasswordHash("astral"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_astral_mushroom.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_visit_astral_travel_agent.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_choose_great_trip.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(GREAT_TRIP.canAdventure());
        assertTrue(GREAT_TRIP.prepareForAdventure());

        assertEquals(5, HALF_ASTRAL.getCount(KoLConstants.activeEffects));
        assertThat(Preferences.getString("currentAstralTrip"), is("Great Trip"));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.ASTRAL);

        var requests = client.getRequests();
        assertThat(requests, hasSize(7));

        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.ASTRAL_MUSHROOM + "&ajax=1&pwd=astral");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(2), "/adventure.php", "snarfblat=97&pwd=astral");
        assertGetRequest(requests.get(3), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(5), "/choice.php", "whichchoice=71&option=3&pwd=astral");
        assertPostRequest(requests.get(6), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Mole {
    private static final KoLAdventure MT_MOLEHILL =
        AdventureDatabase.getAdventureByName("Mt. Molehill");
    private static AdventureResult GONG = ItemPool.get(ItemPool.GONG, 1);
    private static AdventureResult SHAPE_OF_MOLE = EffectPool.get(EffectPool.SHAPE_OF_MOLE);

    @Test
    public void mustHaveGongOrShapeOfMole() {
      var cleanups = new Cleanups(withLimitMode(LimitMode.NONE));
      try (cleanups) {
        assertFalse(MT_MOLEHILL.canAdventure());
      }
    }

    @Test
    public void canAdventureIfInShapeOfMole() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withEffect("Shape of...Mole!", 12),
              withProperty("currentLlamaForm", "Mole"),
              withLimitMode(LimitMode.MOLE));
      try (cleanups) {
        assertTrue(MT_MOLEHILL.canAdventure());
        assertTrue(MT_MOLEHILL.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithGong() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(GONG),
              withNoEffects(),
              withProperty("currentLlamaForm", ""),
              withLimitMode(LimitMode.NONE),
              withPasswordHash("mole"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE));
      try (cleanups) {
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_use_llama_lama_gong.html"));
        client.addResponse(200, html("request/test_choose_mole_form.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(MT_MOLEHILL.canAdventure());
        assertTrue(MT_MOLEHILL.prepareForAdventure());

        assertEquals(12, SHAPE_OF_MOLE.getCount(KoLConstants.activeEffects));
        assertThat(Preferences.getString("currentLlamaForm"), is("Mole"));
        assertEquals(KoLCharacter.getLimitMode(), LimitMode.MOLE);

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));

        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.GONG + "&ajax=1&pwd=mole");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/choice.php", "whichchoice=276&option=2&pwd=mole");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Cola {
    private static final KoLAdventure COLA_NONE =
        AdventureDatabase.getAdventureByName("Battlefield (No Uniform)");
    private static final KoLAdventure COLA_CLOACA =
        AdventureDatabase.getAdventureByName("Battlefield (Cloaca Uniform)");
    private static final KoLAdventure COLA_DYSPEPSI =
        AdventureDatabase.getAdventureByName("Battlefield (Dyspepsi Uniform)");

    @Test
    public void mustMeetZonePrerequesites() {
      var cleanups =
          new Cleanups(withAscensions(1), withLevel(4), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertFalse(COLA_CLOACA.canAdventure());
        assertFalse(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void mustHaveAscended() {
      var cleanups =
          new Cleanups(withAscensions(0), withLevel(4), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertFalse(COLA_NONE.canAdventure());
      }
    }

    @Test
    public void mustBeAtLeastLevel4() {
      var cleanups =
          new Cleanups(withAscensions(1), withLevel(3), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertFalse(COLA_NONE.canAdventure());
      }
    }

    @Test
    public void mustBeNoMoreThanLevel5() {
      var cleanups =
          new Cleanups(withAscensions(1), withLevel(6), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertFalse(COLA_NONE.canAdventure());
      }
    }

    @Test
    public void mustHaveRecoveredKey() {
      var cleanups =
          new Cleanups(
              withAscensions(1), withLevel(4), withQuestProgress(Quest.EGO, QuestDatabase.STARTED));
      try (cleanups) {
        assertFalse(COLA_NONE.canAdventure());
      }
    }

    @Test
    public void canAdventureWithCloacaUniformEquipped() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(Slot.HAT, ItemPool.CLOACA_HELMET),
              withEquipped(Slot.OFFHAND, ItemPool.CLOACA_SHIELD),
              withEquipped(Slot.PANTS, ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_CLOACA.canAdventure());
        assertFalse(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureCloacaEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(Slot.HAT, ItemPool.CLOACA_HELMET),
              withEquipped(Slot.OFFHAND, ItemPool.CLOACA_SHIELD),
              withEquipped(Slot.PANTS, ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_CLOACA.canAdventure());
        assertTrue(COLA_CLOACA.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithCloacaUniformAvailable() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquippableItem(ItemPool.CLOACA_HELMET),
              withEquippableItem(ItemPool.CLOACA_SHIELD),
              withEquippableItem(ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_CLOACA.canAdventure());
        assertFalse(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureCloacaAvailable() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquippableItem(ItemPool.CLOACA_HELMET),
              withEquippableItem(ItemPool.CLOACA_SHIELD),
              withEquippableItem(ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_CLOACA.canAdventure());
        assertTrue(COLA_CLOACA.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.CLOACA_UNIFORM + "&ajax=1");
      }
    }

    @Test
    public void canAdventureWithDyspepsiUniformEquipped() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(Slot.HAT, ItemPool.DYSPEPSI_HELMET),
              withEquipped(Slot.OFFHAND, ItemPool.DYSPEPSI_SHIELD),
              withEquipped(Slot.PANTS, ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertFalse(COLA_CLOACA.canAdventure());
        assertTrue(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureDyspepsiEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(Slot.HAT, ItemPool.DYSPEPSI_HELMET),
              withEquipped(Slot.OFFHAND, ItemPool.DYSPEPSI_SHIELD),
              withEquipped(Slot.PANTS, ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_DYSPEPSI.canAdventure());
        assertTrue(COLA_DYSPEPSI.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithDyspepsiUniformAvailable() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquippableItem(ItemPool.DYSPEPSI_HELMET),
              withEquippableItem(ItemPool.DYSPEPSI_SHIELD),
              withEquippableItem(ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertFalse(COLA_CLOACA.canAdventure());
        assertTrue(COLA_DYSPEPSI.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureDyspepsiAvailable() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquippableItem(ItemPool.DYSPEPSI_HELMET),
              withEquippableItem(ItemPool.DYSPEPSI_SHIELD),
              withEquippableItem(ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_DYSPEPSI.canAdventure());
        assertTrue(COLA_DYSPEPSI.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.DYSPEPSI_UNIFORM + "&ajax=1");
      }
    }

    @Test
    public void canPrepareForAdventureWithNoUniform() {
      setupFakeClient();

      var cleanups =
          new Cleanups(withAscensions(1), withLevel(4), withQuestProgress(Quest.EGO, "step1"));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_NONE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureAndRemoveCloacaUniform() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(Slot.HAT, ItemPool.CLOACA_HELMET),
              withEquipped(Slot.OFFHAND, ItemPool.CLOACA_SHIELD),
              withEquipped(Slot.PANTS, ItemPool.CLOACA_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_NONE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=offhand");
      }
    }

    @Test
    public void canPrepareForAdventureAndRemoveDyspepsiUniform() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withAscensions(1),
              withLevel(4),
              withQuestProgress(Quest.EGO, "step1"),
              withEquipped(Slot.HAT, ItemPool.DYSPEPSI_HELMET),
              withEquipped(Slot.OFFHAND, ItemPool.DYSPEPSI_SHIELD),
              withEquipped(Slot.PANTS, ItemPool.DYSPEPSI_FATIGUES));
      try (cleanups) {
        assertTrue(COLA_NONE.canAdventure());
        assertTrue(COLA_NONE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&ajax=1&action=unequip&type=offhand");
      }
    }
  }

  @Nested
  class Gingerbread {
    private static final KoLAdventure CIVIC_CENTER =
        AdventureDatabase.getAdventureByName("Gingerbread Civic Center");
    private static final KoLAdventure TRAIN_STATION =
        AdventureDatabase.getAdventureByName("Gingerbread Train Station");
    private static final KoLAdventure INDUSTRIAL_ZONE =
        AdventureDatabase.getAdventureByName("Gingerbread Industrial Zone");
    private static final KoLAdventure RETAIL_DISTRICT =
        AdventureDatabase.getAdventureByName("Gingerbread Upscale Retail District");
    private static final KoLAdventure SEWERS =
        AdventureDatabase.getAdventureByName("Gingerbread Sewers");

    @Test
    public void mustHaveAccessToTheCity() {
      var cleanups =
          new Cleanups(
              withProperty("gingerbreadCityAvailable", false),
              withProperty("_gingerbreadCityTurns", 0));
      try (cleanups) {
        assertFalse(CIVIC_CENTER.canAdventure());
        assertFalse(TRAIN_STATION.canAdventure());
        assertFalse(INDUSTRIAL_ZONE.canAdventure());
        assertFalse(RETAIL_DISTRICT.canAdventure());
        assertFalse(SEWERS.canAdventure());
      }
    }

    @Test
    public void someZonesRequireUnlocking() {
      var cleanups =
          new Cleanups(
              withProperty("gingerbreadCityAvailable", true),
              withProperty("_gingerbreadCityTurns", 0));
      try (cleanups) {
        assertTrue(CIVIC_CENTER.canAdventure());
        assertTrue(TRAIN_STATION.canAdventure());
        assertTrue(INDUSTRIAL_ZONE.canAdventure());
        assertFalse(RETAIL_DISTRICT.canAdventure());
        assertFalse(SEWERS.canAdventure());
      }
    }

    @Test
    public void sewersCanBeUnlocked() {
      var cleanups =
          new Cleanups(
              withProperty("gingerbreadCityAvailable", true),
              withProperty("gingerSewersUnlocked", true),
              withProperty("_gingerbreadCityTurns", 0));
      try (cleanups) {
        assertTrue(SEWERS.canAdventure());
      }
    }

    @Test
    public void retailDistrictCanBeUnlocked() {
      var cleanups =
          new Cleanups(
              withProperty("gingerbreadCityAvailable", true),
              withProperty("gingerRetailUnlocked", true),
              withProperty("_gingerbreadCityTurns", 0));
      try (cleanups) {
        assertTrue(RETAIL_DISTRICT.canAdventure());
      }
    }

    private void testTurnsAvailableVsUsed(int turnsAvailable, int turnsUsed) {
      var cleanups = new Cleanups(withProperty("_gingerbreadCityTurns", turnsUsed));
      try (cleanups) {
        assertThat(CIVIC_CENTER.canAdventure(), is(turnsUsed < turnsAvailable));
      }
    }

    @CartesianTest
    public void canAdventureWithTurnsLeft(
        @Values(booleans = {false, true}) final boolean extraTurns,
        @Values(booleans = {false, true}) final boolean clockAdvanced) {
      var cleanups =
          new Cleanups(
              withProperty("gingerbreadCityAvailable", true),
              withProperty("gingerExtraAdventures", extraTurns),
              withProperty("_gingerbreadClockAdvanced", clockAdvanced));
      try (cleanups) {
        int available = 20;
        if (extraTurns) available += 10;
        if (clockAdvanced) available -= 5;
        testTurnsAvailableVsUsed(available, available);
        testTurnsAvailableVsUsed(available, available - 5);
      }
    }
  }

  @Nested
  class Spookyraven {
    private static final KoLAdventure HAUNTED_PANTRY =
        AdventureDatabase.getAdventureByName("The Haunted Pantry");
    private static final KoLAdventure HAUNTED_CONSERVATORY =
        AdventureDatabase.getAdventureByName("The Haunted Conservatory");
    private static final KoLAdventure HAUNTED_KITCHEN =
        AdventureDatabase.getAdventureByName("The Haunted Kitchen");
    private static final KoLAdventure HAUNTED_BILLIARDS_ROOM =
        AdventureDatabase.getAdventureByName("The Haunted Billiards Room");
    private static final KoLAdventure HAUNTED_LIBRARY =
        AdventureDatabase.getAdventureByName("The Haunted Library");
    private static final KoLAdventure HAUNTED_GALLERY =
        AdventureDatabase.getAdventureByName("The Haunted Gallery");
    private static final KoLAdventure HAUNTED_BATHROOM =
        AdventureDatabase.getAdventureByName("The Haunted Bathroom");
    private static final KoLAdventure HAUNTED_BEDROOM =
        AdventureDatabase.getAdventureByName("The Haunted Bedroom");
    private static final KoLAdventure HAUNTED_BALLROOM =
        AdventureDatabase.getAdventureByName("The Haunted Ballroom");
    private static final KoLAdventure HAUNTED_STORAGE_ROOM =
        AdventureDatabase.getAdventureByName("The Haunted Storage Room");
    private static final KoLAdventure HAUNTED_NURSERY =
        AdventureDatabase.getAdventureByName("The Haunted Nursery");
    private static final KoLAdventure HAUNTED_LABORATORY =
        AdventureDatabase.getAdventureByName("The Haunted Laboratory");
    private static final KoLAdventure HAUNTED_WINE_CELLAR =
        AdventureDatabase.getAdventureByName("The Haunted Wine Cellar");
    private static final KoLAdventure HAUNTED_LAUNDRY_ROOM =
        AdventureDatabase.getAdventureByName("The Haunted Laundry Room");
    private static final KoLAdventure HAUNTED_BOILER_ROOM =
        AdventureDatabase.getAdventureByName("The Haunted Boiler Room");
    private static final KoLAdventure SUMMONING_CHAMBER =
        AdventureDatabase.getAdventureByName("Summoning Chamber");

    @Test
    public void hauntedPantryAvailable() {
      var cleanups = new Cleanups(withAscensions(0), withLevel(1));
      try (cleanups) {
        assertTrue(HAUNTED_PANTRY.canAdventure());
      }
    }

    @Test
    public void hauntedFirstFloorAvailableWithQuest() {
      var cleanups =
          new Cleanups(withQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(HAUNTED_KITCHEN.canAdventure());
        assertTrue(HAUNTED_CONSERVATORY.canAdventure());
      }
    }

    @Test
    public void hauntedFirstFloorAvailableWithTelegram() {
      var cleanups = new Cleanups(withItem(ItemPool.SPOOKYRAVEN_TELEGRAM));
      try (cleanups) {
        assertTrue(HAUNTED_KITCHEN.canAdventure());
        assertTrue(HAUNTED_CONSERVATORY.canAdventure());
      }
    }

    @Test
    public void canReadTelegramToStartQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withItem(ItemPool.SPOOKYRAVEN_TELEGRAM));
      try (cleanups) {
        client.addResponse(200, html("request/test_spookyraven_telegram.html"));
        client.addResponse(200, ""); // api.php
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_NECKLACE), QuestDatabase.UNSTARTED);
        assertTrue(HAUNTED_KITCHEN.canAdventure());
        assertTrue(HAUNTED_KITCHEN.prepareForAdventure());
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_NECKLACE), QuestDatabase.STARTED);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.SPOOKYRAVEN_TELEGRAM + "&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void hauntedFirstFloorNotAvailableWithoutLevel() {
      var cleanups = new Cleanups(withAscensions(0), withLevel(4));
      try (cleanups) {
        assertFalse(HAUNTED_KITCHEN.canAdventure());
        assertFalse(HAUNTED_CONSERVATORY.canAdventure());
      }
    }

    @Test
    public void hauntedFirstFloorAvailableWithLevel() {
      var cleanups = new Cleanups(withAscensions(0), withLevel(5));
      try (cleanups) {
        assertTrue(HAUNTED_KITCHEN.canAdventure());
        assertTrue(HAUNTED_CONSERVATORY.canAdventure());
      }
    }

    @Test
    public void canFetchAndReadTelegramToStartQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups = new Cleanups(withHttpClientBuilder(builder), withLevel(5));
      try (cleanups) {
        client.addResponse(200, html("request/test_spookyraven_telegram.json"));
        client.addResponse(200, "");
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_NECKLACE), QuestDatabase.UNSTARTED);
        assertTrue(HAUNTED_KITCHEN.canAdventure());
        assertTrue(HAUNTED_KITCHEN.prepareForAdventure());
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_NECKLACE), QuestDatabase.STARTED);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/api.php", "what=inventory&for=KoLmafia");
        assertPostRequest(
            requests.get(1),
            "/inv_use.php",
            "whichitem=" + ItemPool.SPOOKYRAVEN_TELEGRAM + "&ajax=1");
      }
    }

    @Test
    public void hauntedBilliardsRoomAvailableWithKey() {
      var cleanups = new Cleanups(withItem(ItemPool.BILLIARDS_KEY));
      try (cleanups) {
        assertTrue(HAUNTED_BILLIARDS_ROOM.canAdventure());
      }
    }

    @Test
    public void hauntedBilliardsRoomNotAvailableWithoutKey() {
      var cleanups = new Cleanups();
      try (cleanups) {
        assertFalse(HAUNTED_BILLIARDS_ROOM.canAdventure());
      }
    }

    @Test
    public void hauntedLibraryAvailableWithKey() {
      var cleanups = new Cleanups(withItem(ItemPool.LIBRARY_KEY));
      try (cleanups) {
        assertTrue(HAUNTED_LIBRARY.canAdventure());
      }
    }

    @Test
    public void hauntedLibraryNotAvailableWithoutKey() {
      var cleanups = new Cleanups();
      try (cleanups) {
        assertFalse(HAUNTED_LIBRARY.canAdventure());
      }
    }

    @Test
    public void hauntedSecondFloorAvailableWithQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.SPOOKYRAVEN_DANCE, "step1"));
      try (cleanups) {
        assertTrue(HAUNTED_GALLERY.canAdventure());
        assertTrue(HAUNTED_BATHROOM.canAdventure());
        assertTrue(HAUNTED_BEDROOM.canAdventure());
      }
    }

    @Test
    public void hauntedSecondFloorAvailableWithNecklaceAndAscension() {
      var cleanups = new Cleanups(withItem(ItemPool.SPOOKYRAVEN_NECKLACE), withAscensions(1));
      try (cleanups) {
        assertTrue(HAUNTED_GALLERY.canAdventure());
        assertTrue(HAUNTED_BATHROOM.canAdventure());
        assertTrue(HAUNTED_BEDROOM.canAdventure());
      }
    }

    @Test
    public void hauntedSecondFloorAvailableWithNecklaceAndLevel() {
      var cleanups = new Cleanups(withItem(ItemPool.SPOOKYRAVEN_NECKLACE), withLevel(7));
      try (cleanups) {
        assertTrue(HAUNTED_GALLERY.canAdventure());
        assertTrue(HAUNTED_BATHROOM.canAdventure());
        assertTrue(HAUNTED_BEDROOM.canAdventure());
      }
    }

    @Test
    public void canTalkToLadySpookyravenToStartQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withAscensions(1),
              withItem(ItemPool.SPOOKYRAVEN_NECKLACE));
      try (cleanups) {
        client.addResponse(200, html("request/test_lady_spookyraven_2.html")); // Hand in necklace
        client.addResponse(
            200, html("request/test_lady_spookyraven_2A.html")); // Unlock second floor
        client.addResponse(200, ""); // api.php
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_NECKLACE), QuestDatabase.UNSTARTED);
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_DANCE), QuestDatabase.UNSTARTED);
        assertTrue(HAUNTED_GALLERY.canAdventure());
        assertTrue(HAUNTED_GALLERY.prepareForAdventure());
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_NECKLACE), QuestDatabase.FINISHED);
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_DANCE), "step1");

        var requests = client.getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(requests.get(0), "/place.php", "whichplace=manor1&action=manor1_ladys");
        assertPostRequest(requests.get(1), "/place.php", "whichplace=manor2&action=manor2_ladys");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void hauntedSecondFloorNotAvailableWithNecklaceWithoutLevel() {
      var cleanups = new Cleanups(withItem(ItemPool.SPOOKYRAVEN_NECKLACE), withLevel(6));
      try (cleanups) {
        assertFalse(HAUNTED_GALLERY.canAdventure());
        assertFalse(HAUNTED_BATHROOM.canAdventure());
        assertFalse(HAUNTED_BEDROOM.canAdventure());
      }
    }

    @Test
    public void hauntedSecondFloorAvailableWithSpookyravenNecklaceQuestFinishedAndAscension() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED),
              withAscensions(1));
      try (cleanups) {
        assertTrue(HAUNTED_GALLERY.canAdventure());
        assertTrue(HAUNTED_BATHROOM.canAdventure());
        assertTrue(HAUNTED_BEDROOM.canAdventure());
      }
    }

    @Test
    public void hauntedSecondFloorAvailableWithSpookyravenNecklaceQuestFinishedAndLevel() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED), withLevel(7));
      try (cleanups) {
        assertTrue(HAUNTED_GALLERY.canAdventure());
        assertTrue(HAUNTED_BATHROOM.canAdventure());
        assertTrue(HAUNTED_BEDROOM.canAdventure());
      }
    }

    @Test
    public void hauntedSecondFloorNotAvailableWithSpookyravenNecklaceQuestFinishedWithoutLevel() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED), withLevel(6));
      try (cleanups) {
        assertFalse(HAUNTED_GALLERY.canAdventure());
        assertFalse(HAUNTED_BATHROOM.canAdventure());
        assertFalse(HAUNTED_BEDROOM.canAdventure());
      }
    }

    @Test
    public void canTalkToLadySpookyravenTwiceToStartQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withAscensions(1),
              withItem(ItemPool.SPOOKYRAVEN_NECKLACE));
      try (cleanups) {
        client.addResponse(200, html("request/test_lady_spookyraven_1.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_lady_spookyraven_2A.html"));
        client.addResponse(200, ""); // api.php
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_NECKLACE), QuestDatabase.UNSTARTED);
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_DANCE), QuestDatabase.UNSTARTED);
        assertTrue(HAUNTED_GALLERY.canAdventure());
        assertTrue(HAUNTED_GALLERY.prepareForAdventure());
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_NECKLACE), QuestDatabase.FINISHED);
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_DANCE), "step1");

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));
        assertPostRequest(requests.get(0), "/place.php", "whichplace=manor1&action=manor1_ladys");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(2), "/place.php", "whichplace=manor2&action=manor2_ladys");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void hauntedBallroomAvailableWithQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.SPOOKYRAVEN_DANCE, "step3"));
      try (cleanups) {
        assertTrue(HAUNTED_BALLROOM.canAdventure());
      }
    }

    @Test
    public void hauntedBallroomAvailableWithItems() {
      // ResultProcessor sets quest progress to step 2 when you get the third item.
      var cleanups =
          new Cleanups(
              withItem(ItemPool.POWDER_PUFF),
              withItem(ItemPool.FINEST_GOWN),
              withItem(ItemPool.DANCING_SHOES));
      try (cleanups) {
        assertTrue(HAUNTED_BALLROOM.canAdventure());
      }
    }

    @Test
    public void canTalkToLadySpookyravenToOpenBallroom() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.POWDER_PUFF),
              withItem(ItemPool.FINEST_GOWN),
              withItem(ItemPool.DANCING_SHOES));
      try (cleanups) {
        client.addResponse(200, html("request/test_lady_spookyraven_2B.html"));
        client.addResponse(200, ""); // api.php
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_DANCE), QuestDatabase.UNSTARTED);
        assertTrue(HAUNTED_BALLROOM.canAdventure());
        assertTrue(HAUNTED_BALLROOM.prepareForAdventure());
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_DANCE), "step3");

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/place.php", "whichplace=manor2&action=manor2_ladys");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canBallroomDanceToOpenThirdFloor() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withAscensions(1),
              withQuestProgress(Quest.SPOOKYRAVEN_DANCE, "step3"));
      try (cleanups) {
        client.addResponse(200, html("request/test_spookraven_dance.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_spookyraven_after_dance.html"));
        var request = new GenericRequest("adventure.php?snarfblat=395");
        request.run();
        assertEquals(QuestDatabase.getQuest(Quest.SPOOKYRAVEN_DANCE), QuestDatabase.FINISHED);
        assertTrue(HAUNTED_LABORATORY.canAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=395");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(2), "/place.php", "whichplace=manor2");
      }
    }

    @Test
    public void hauntedBallroomNotAvailableWithoutQuestOrItems() {
      var cleanups = new Cleanups(withQuestProgress(Quest.SPOOKYRAVEN_DANCE, "step1"));
      try (cleanups) {
        assertFalse(HAUNTED_BALLROOM.canAdventure());
      }
    }

    @Test
    public void hauntedThirdFloorAvailableWithQuestAndAscension() {
      var cleanups =
          new Cleanups(
              withAscensions(1),
              withQuestProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(HAUNTED_STORAGE_ROOM.canAdventure());
        assertTrue(HAUNTED_NURSERY.canAdventure());
        assertTrue(HAUNTED_LABORATORY.canAdventure());
      }
    }

    @Test
    public void hauntedThirdFloorAvailableWithQuestAndLevel() {
      var cleanups =
          new Cleanups(
              withLevel(9), withQuestProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(HAUNTED_STORAGE_ROOM.canAdventure());
        assertTrue(HAUNTED_NURSERY.canAdventure());
        assertTrue(HAUNTED_LABORATORY.canAdventure());
      }
    }

    @Test
    public void hauntedThirdFloorNotAvailableWithQuestAndNotLevel() {
      var cleanups =
          new Cleanups(
              withLevel(8), withQuestProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertFalse(HAUNTED_STORAGE_ROOM.canAdventure());
        assertFalse(HAUNTED_NURSERY.canAdventure());
        assertFalse(HAUNTED_LABORATORY.canAdventure());
      }
    }

    @Test
    public void hauntedCellarAvailableWithQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.MANOR, "step1"));
      try (cleanups) {
        assertTrue(HAUNTED_WINE_CELLAR.canAdventure());
        assertTrue(HAUNTED_LAUNDRY_ROOM.canAdventure());
        assertTrue(HAUNTED_BOILER_ROOM.canAdventure());
      }
    }

    @Test
    public void hauntedCellarNotAvailableWithOutQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.MANOR, QuestDatabase.STARTED));
      try (cleanups) {
        assertFalse(HAUNTED_WINE_CELLAR.canAdventure());
        assertFalse(HAUNTED_LAUNDRY_ROOM.canAdventure());
        assertFalse(HAUNTED_BOILER_ROOM.canAdventure());
      }
    }

    @Test
    public void summoningChamberNotAvailableIfNotOpened() {
      var cleanups = new Cleanups(withQuestProgress(Quest.MANOR, "step1"));
      try (cleanups) {
        assertFalse(SUMMONING_CHAMBER.canAdventure());
      }
    }

    @Test
    public void summoningChamberAvailableIfHaveWineBomb() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.MANOR, "step2"),
              withItem(ItemPool.WINE_BOMB));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_wine_bomb.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(SUMMONING_CHAMBER.canAdventure());
        assertTrue(SUMMONING_CHAMBER.prepareForAdventure());
        assertThat(Quest.MANOR, isStep("step3"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=manor4&action=manor4_chamberwall");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void summoningChamberAvailableIfCanDissolveMortar() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.MANOR, "step2"),
              withItem(ItemPool.LOOSENING_POWDER),
              withItem(ItemPool.POWDERED_CASTOREUM),
              withItem(ItemPool.DRAIN_DISSOLVER),
              withItem(ItemPool.TRIPLE_DISTILLED_TURPENTINE),
              withItem(ItemPool.DETARTRATED_ANHYDROUS_SUBLICALC),
              withItem(ItemPool.TRIATOMACEOUS_DUST));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_mortar_dissolving_solution.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(SUMMONING_CHAMBER.canAdventure());
        assertTrue(SUMMONING_CHAMBER.prepareForAdventure());
        assertThat(Quest.MANOR, isStep("step3"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=manor4&action=manor4_chamberwall");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void summoningChamberAvailableIfOpened() {
      var cleanups = new Cleanups(withQuestProgress(Quest.MANOR, "step3"));
      try (cleanups) {
        assertTrue(SUMMONING_CHAMBER.canAdventure());
      }
    }

    @Test
    public void summoningChamberNotAvailableIfLordSpookyravenDefeated() {
      var cleanups = new Cleanups(withQuestProgress(Quest.MANOR, QuestDatabase.FINISHED));
      try (cleanups) {
        assertFalse(SUMMONING_CHAMBER.canAdventure());
      }
    }
  }

  @Nested
  class DegrassiKnoll {
    private static final KoLAdventure GARAGE =
        AdventureDatabase.getAdventureByName("The Degrassi Knoll Garage");

    @Test
    public void hostileKnollNotAvailableInMuscleSign() {
      var cleanups =
          new Cleanups(
              withSign(ZodiacSign.VOLE), withQuestProgress(Quest.UNTINKER, QuestDatabase.STARTED));
      try (cleanups) {
        assertFalse(GARAGE.canAdventure());
      }
    }

    @Test
    public void hostileKnollNotAvailableIfNotUnlocked() {
      var cleanups =
          new Cleanups(
              withSign(ZodiacSign.PACKRAT),
              withQuestProgress(Quest.UNTINKER, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.MEATCAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.LARVA, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(GARAGE.canAdventure());
      }
    }

    @Test
    public void hostileKnollAvailableIfUntinkerUnlocked() {
      var cleanups =
          new Cleanups(
              withSign(ZodiacSign.PACKRAT),
              withQuestProgress(Quest.UNTINKER, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(GARAGE.canAdventure());
      }
    }

    @Test
    public void hostileKnollAvailableIfPacoUnlocked() {
      var cleanups =
          new Cleanups(
              withSign(ZodiacSign.PACKRAT),
              withQuestProgress(Quest.MEATCAR, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(GARAGE.canAdventure());
      }
    }

    @Test
    public void willAcceptUntinkerQuestToUnlockHostileKnoll() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withSign(ZodiacSign.PACKRAT),
              withQuestProgress(Quest.UNTINKER, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_untinker_accept_quest.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(GARAGE.canAdventure());
        assertTrue(GARAGE.prepareForAdventure());
        assertThat(Quest.UNTINKER, isStarted());

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/place.php",
            "whichplace=forestvillage&action=fv_untinker_quest&preaction=screwquest");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Portal {
    private static final KoLAdventure EL_VIBRATO =
        AdventureDatabase.getAdventureByName("El Vibrato Island");

    @Test
    public void elVibratoNotAvailableWithoutPortal() {
      var cleanups = new Cleanups(withProperty("currentPortalEnergy", 0));
      try (cleanups) {
        assertFalse(EL_VIBRATO.canAdventure());
      }
    }

    @Test
    public void elVibratoAvailableWithChargedPortal() {
      var cleanups = new Cleanups(withProperty("currentPortalEnergy", 10));
      try (cleanups) {
        assertTrue(EL_VIBRATO.canAdventure());
      }
    }

    @Test
    public void failureToAdventureSetsPortalEnergyToZero() {
      var cleanups = new Cleanups(withProperty("currentPortalEnergy", 10));
      try (cleanups) {
        var failure =
            KoLAdventure.findAdventureFailure(
                html("request/test_adventure_fail_due_to_el_vibrato_power.html"));
        assertThat(failure, greaterThan(0));
        assertThat("currentPortalEnergy", isSetTo(0));
      }
    }

    @Test
    public void elVibratoAvailableWithTrapezoid() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.TRAPEZOID),
              withEmptyCampground(),
              withProperty("currentPortalEnergy", 0));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_el_vibrato_trapezoid.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(EL_VIBRATO.canAdventure());
        assertTrue(EL_VIBRATO.prepareForAdventure());
        assertFalse(InventoryManager.hasItem(ItemPool.TRAPEZOID));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=3198&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Tavern {
    private static final KoLAdventure TAVERN_CELLAR =
        AdventureDatabase.getAdventureByName("The Typical Tavern Cellar");

    @Test
    void thatCellarIsOpenIfAlreadyTalkedToBart() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withQuestProgress(Quest.RAT, "step1"));
      try (cleanups) {
        assertTrue(TAVERN_CELLAR.canAdventure());
        assertTrue(TAVERN_CELLAR.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void canOpenCellarByTalkingToBart() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder), withQuestProgress(Quest.RAT, QuestDatabase.STARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_barkeep_accept.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(TAVERN_CELLAR.canAdventure());
        assertTrue(TAVERN_CELLAR.prepareForAdventure());
        assertThat(Quest.RAT, isStep("step1"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/tavern.php", "place=barkeep");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Pixels {
    private static final KoLAdventure FUNGUS_PLAINS =
        AdventureDatabase.getAdventureByName("The Fungus Plains");
    private static final KoLAdventure VANYA =
        AdventureDatabase.getAdventureByName("Vanya's Castle Foyer");

    @Test
    public void pixelRealmNotAvailableWithoutWoods() {
      var cleanups = new Cleanups(withQuestProgress(Quest.LARVA, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(FUNGUS_PLAINS.canAdventure());
      }
    }

    @Test
    public void canAdventureWithTransfunctionerEquipped() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED),
              withEquipped(Slot.ACCESSORY1, ItemPool.TRANSFUNCTIONER));
      try (cleanups) {
        assertTrue(FUNGUS_PLAINS.canAdventure());
        assertTrue(FUNGUS_PLAINS.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithTransfunctionerInInventory() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED),
              withEquippableItem(ItemPool.TRANSFUNCTIONER));
      try (cleanups) {
        client.addResponse(200, html("request/test_equip_transfunctioner.html"));
        client.addResponse(200, ""); // charpane.php
        assertTrue(FUNGUS_PLAINS.canAdventure());
        assertTrue(FUNGUS_PLAINS.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&slot=1&action=equip&whichitem=" + ItemPool.TRANSFUNCTIONER);
        assertGetRequest(requests.get(1), "/charpane.php", null);
      }
    }

    private void acquireAndEquipTransfunctioner(FakeHttpClientBuilder builder) {
      var client = builder.client;
      // place.php?whichplace=forestvillage&action=fv_mystic
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_mystic_1.html"));
      // choice.php?whichchoice=664&option=1&pwd
      client.addResponse(200, html("request/test_mystic_2.html"));
      // choice.php?whichchoice=664&option=1&pwd
      client.addResponse(200, html("request/test_mystic_3.html"));
      // choice.php?whichchoice=664&option=1&pwd
      client.addResponse(200, html("request/test_mystic_4.html"));
      client.addResponse(200, ""); // api.php
      // inv_equip.php?which=2&ajax=1&slot=1&action=equip&whichitem=458
      client.addResponse(200, html("request/test_equip_transfunctioner.html"));
      client.addResponse(200, ""); // charpane.php

      assertTrue(FUNGUS_PLAINS.canAdventure());
      assertTrue(FUNGUS_PLAINS.prepareForAdventure());

      var requests = client.getRequests();
      assertThat(requests, hasSize(8));
      assertPostRequest(requests.get(0), "/place.php", "whichplace=forestvillage&action=fv_mystic");
      assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(2), "/choice.php", "whichchoice=664&option=1");
      assertPostRequest(requests.get(3), "/choice.php", "whichchoice=664&option=1");
      assertPostRequest(requests.get(4), "/choice.php", "whichchoice=664&option=1");
      assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(
          requests.get(6),
          "/inv_equip.php",
          "which=2&ajax=1&slot=1&action=equip&whichitem=" + ItemPool.TRANSFUNCTIONER);
      assertGetRequest(requests.get(7), "/charpane.php", null);
    }

    @Test
    public void canAcquireAndEquipTransfunctionerAutomated() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED),
              withProperty("choiceAdventure664", 1));
      try (cleanups) {
        acquireAndEquipTransfunctioner(builder);
      }
    }

    @Test
    public void canAcquireAndEquipTransfunctionerManually() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED),
              withProperty("choiceAdventure664", 0));
      try (cleanups) {
        acquireAndEquipTransfunctioner(builder);
      }
    }
  }

  @Nested
  class DwarfFactory {
    private static final KoLAdventure WAREHOUSE =
        AdventureDatabase.getAdventureByName("Dwarven Factory Warehouse");
    private static final KoLAdventure OFFICE =
        AdventureDatabase.getAdventureByName("The Mine Foremens' Office");

    @Test
    public void mustHaveStartedQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.FACTORY, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(WAREHOUSE.canAdventure());
        assertFalse(OFFICE.canAdventure());
      }
    }

    @Test
    public void mustHaveOutfit() {
      var cleanups = new Cleanups(withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED));
      try (cleanups) {
        assertFalse(WAREHOUSE.canAdventure());
        assertFalse(OFFICE.canAdventure());
      }
    }

    @Test
    public void canAdventureWithMiningOutfitEquipped() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquipped(Slot.HAT, "miner's helmet"),
              withEquipped(Slot.WEAPON, "7-Foot Dwarven mattock"),
              withEquipped(Slot.PANTS, "miner's pants"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(OFFICE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureWithMiningOutfitEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquipped(Slot.HAT, "miner's helmet"),
              withEquipped(Slot.WEAPON, "7-Foot Dwarven mattock"),
              withEquipped(Slot.PANTS, "miner's pants"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(WAREHOUSE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithMiningOutfitInInventory() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquippableItem("miner's helmet"),
              withEquippableItem("7-Foot Dwarven mattock"),
              withEquippableItem("miner's pants"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(OFFICE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureWithMiningOutfitInInventory() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquippableItem("miner's helmet"),
              withEquippableItem("7-Foot Dwarven mattock"),
              withEquippableItem("miner's pants"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(WAREHOUSE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.MINING_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canAdventureWithDwarvishUniformEquipped() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquipped(Slot.HAT, "dwarvish war helmet"),
              withEquipped(Slot.WEAPON, "dwarvish war mattock"),
              withEquipped(Slot.PANTS, "dwarvish war kilt"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(OFFICE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureWithDwarvishUniformEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquipped(Slot.HAT, "dwarvish war helmet"),
              withEquipped(Slot.WEAPON, "dwarvish war mattock"),
              withEquipped(Slot.PANTS, "dwarvish war kilt"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(WAREHOUSE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canAdventureWithDwarvishUniformInInventory() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquippableItem("dwarvish war helmet"),
              withEquippableItem("dwarvish war mattock"),
              withEquippableItem("dwarvish war kilt"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(OFFICE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureWithDwarvishUniformInInventory() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.FACTORY, QuestDatabase.STARTED),
              withEquippableItem("dwarvish war helmet"),
              withEquippableItem("dwarvish war mattock"),
              withEquippableItem("dwarvish war kilt"));
      try (cleanups) {
        assertTrue(WAREHOUSE.canAdventure());
        assertTrue(WAREHOUSE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.DWARVISH_UNIFORM + "&ajax=1");
      }
    }
  }

  @Nested
  class BatHole {

    private static final KoLAdventure BAT_HOLE_ENTRYWAY =
        AdventureDatabase.getAdventureByName("The Bat Hole Entrance");
    private static final KoLAdventure GUANO_JUNCTION =
        AdventureDatabase.getAdventureByName("Guano Junction");
    private static final KoLAdventure BATRAT =
        AdventureDatabase.getAdventureByName("The Batrat and Ratbat Burrow");
    private static final KoLAdventure BEANBAT =
        AdventureDatabase.getAdventureByName("The Beanbat Chamber");
    private static final KoLAdventure BOSSBAT =
        AdventureDatabase.getAdventureByName("The Boss Bat's Lair");

    @Test
    public void cannotVisitBatHoleWithQuestUnstarted() {
      var cleanups = new Cleanups(withQuestProgress(Quest.BAT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(BAT_HOLE_ENTRYWAY.canAdventure());
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertFalse(BATRAT.canAdventure());
        assertFalse(BEANBAT.canAdventure());
        assertFalse(BOSSBAT.canAdventure());
      }
    }

    @Test
    public void canVisitBatHoleWithQuestStarted() {
      var cleanups = new Cleanups(withQuestProgress(Quest.BAT, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(BAT_HOLE_ENTRYWAY.canAdventure());
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertFalse(BATRAT.canAdventure());
        assertFalse(BEANBAT.canAdventure());
        assertFalse(BOSSBAT.canAdventure());
      }
    }

    @Test
    public void cannotVisitGuanoJunctionWithoutStenchProtection() {
      var cleanups = new Cleanups(withQuestProgress(Quest.BAT, QuestDatabase.STARTED));
      try (cleanups) {
        // We do not currently allow betweenBattle script to fix
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertFalse(GUANO_JUNCTION.prepareForAdventure());
      }
    }

    @Test
    public void canVisitGuanoJunctionWithStenchProtection() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.BAT, QuestDatabase.STARTED),
              withEquipped(Slot.HAT, "Knob Goblin harem veil"));
      try (cleanups) {
        assertTrue(GUANO_JUNCTION.canAdventure());
        assertTrue(GUANO_JUNCTION.prepareForAdventure());
      }
    }

    @Test
    public void canVisitBatHoleWithOneSonarUsed() {
      var cleanups = new Cleanups(withQuestProgress(Quest.BAT, "step1"));
      try (cleanups) {
        assertTrue(BAT_HOLE_ENTRYWAY.canAdventure());
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertTrue(BATRAT.canAdventure());
        assertFalse(BEANBAT.canAdventure());
        assertFalse(BOSSBAT.canAdventure());
      }
    }

    @Test
    public void canVisitBatHoleWithOneSonarInInventory() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.BAT, QuestDatabase.STARTED), withItem(ItemPool.SONAR, 1));
      try (cleanups) {
        assertTrue(BAT_HOLE_ENTRYWAY.canAdventure());
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertTrue(BATRAT.canAdventure());
        assertFalse(BEANBAT.canAdventure());
        assertFalse(BOSSBAT.canAdventure());
      }
    }

    @Test
    public void canVisitBatHoleWithTwoSonarUsed() {
      var cleanups = new Cleanups(withQuestProgress(Quest.BAT, "step2"));
      try (cleanups) {
        assertTrue(BAT_HOLE_ENTRYWAY.canAdventure());
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertTrue(BATRAT.canAdventure());
        assertTrue(BEANBAT.canAdventure());
        assertFalse(BOSSBAT.canAdventure());
      }
    }

    @Test
    public void canVisitBatHoleWithTwoSonarInInventory() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.BAT, QuestDatabase.STARTED), withItem(ItemPool.SONAR, 2));
      try (cleanups) {
        assertTrue(BAT_HOLE_ENTRYWAY.canAdventure());
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertTrue(BATRAT.canAdventure());
        assertTrue(BEANBAT.canAdventure());
        assertFalse(BOSSBAT.canAdventure());
      }
    }

    @Test
    public void canVisitBatHoleWithThreeSonarUsed() {
      var cleanups = new Cleanups(withQuestProgress(Quest.BAT, "step3"));
      try (cleanups) {
        assertTrue(BAT_HOLE_ENTRYWAY.canAdventure());
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertTrue(BATRAT.canAdventure());
        assertTrue(BEANBAT.canAdventure());
        assertTrue(BOSSBAT.canAdventure());
      }
    }

    @Test
    public void canVisitBatHoleWithThreeSonarInInventory() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.BAT, QuestDatabase.STARTED), withItem(ItemPool.SONAR, 3));
      try (cleanups) {
        assertTrue(BAT_HOLE_ENTRYWAY.canAdventure());
        assertFalse(GUANO_JUNCTION.canAdventure());
        assertTrue(BATRAT.canAdventure());
        assertTrue(BEANBAT.canAdventure());
        assertTrue(BOSSBAT.canAdventure());
      }
    }

    @Test
    public void canPrepareToAdventureInBatHoleUsingZeroSonar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(withQuestProgress(Quest.BAT, "step1"), withItem(ItemPool.SONAR, 3));
      try (cleanups) {
        assertTrue(BATRAT.canAdventure());
        assertTrue(BATRAT.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareToAdventureInBatHoleUsingOneSonar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.BAT, QuestDatabase.STARTED), withItem(ItemPool.SONAR, 3));
      try (cleanups) {
        assertTrue(BATRAT.canAdventure());
        assertTrue(BATRAT.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.SONAR + "&ajax=1");
      }
    }

    @Test
    public void canPrepareToAdventureInBatHoleUsingTwoSonar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.BAT, QuestDatabase.STARTED), withItem(ItemPool.SONAR, 3));
      try (cleanups) {
        assertTrue(BEANBAT.canAdventure());
        assertTrue(BEANBAT.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.SONAR + "&ajax=1");
        assertPostRequest(
            requests.get(1), "/inv_use.php", "whichitem=" + ItemPool.SONAR + "&ajax=1");
      }
    }

    @Test
    public void canPrepareToAdventureInBatHoleUsingThreeSonar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.BAT, QuestDatabase.STARTED), withItem(ItemPool.SONAR, 3));
      try (cleanups) {
        assertTrue(BOSSBAT.canAdventure());
        assertTrue(BOSSBAT.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.SONAR + "&ajax=1");
        assertPostRequest(
            requests.get(1), "/inv_use.php", "whichitem=" + ItemPool.SONAR + "&ajax=1");
        assertPostRequest(
            requests.get(2), "/inv_use.php", "whichitem=" + ItemPool.SONAR + "&ajax=1");
      }
    }
  }

  @Nested
  class CobbsKnob {

    private static final KoLAdventure OUTSKIRTS_OF_THE_KNOB =
        AdventureDatabase.getAdventureByName("The Outskirts of Cobb's Knob");
    private static final KoLAdventure COBB_BARRACKS =
        AdventureDatabase.getAdventureByName("Cobb's Knob Barracks");
    private static final KoLAdventure COBB_KITCHEN =
        AdventureDatabase.getAdventureByName("Cobb's Knob Kitchens");
    private static final KoLAdventure COBB_HAREM =
        AdventureDatabase.getAdventureByName("Cobb's Knob Harem");
    private static final KoLAdventure COBB_TREASURY =
        AdventureDatabase.getAdventureByName("Cobb's Knob Treasury");
    private static final KoLAdventure COBB_LABORATORY =
        AdventureDatabase.getAdventureByName("Cobb's Knob Laboratory");
    private static final KoLAdventure KNOB_SHAFT =
        AdventureDatabase.getAdventureByName("The Knob Shaft");
    private static final KoLAdventure MENAGERIE_LEVEL_1 =
        AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 1");
    private static final KoLAdventure MENAGERIE_LEVEL_2 =
        AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 2");
    private static final KoLAdventure MENAGERIE_LEVEL_3 =
        AdventureDatabase.getAdventureByName("Cobb's Knob Menagerie, Level 3");
    private static final KoLAdventure THRONE_ROOM =
        AdventureDatabase.getAdventureByName("Throne Room");

    @Test
    public void canVisitCobbsKnobBeforeQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(OUTSKIRTS_OF_THE_KNOB.canAdventure());
        assertFalse(COBB_BARRACKS.canAdventure());
        assertFalse(COBB_KITCHEN.canAdventure());
        assertFalse(COBB_HAREM.canAdventure());
        assertFalse(COBB_TREASURY.canAdventure());
        assertFalse(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobBeforeDecrypting() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(OUTSKIRTS_OF_THE_KNOB.canAdventure());
        assertFalse(COBB_BARRACKS.canAdventure());
        assertFalse(COBB_KITCHEN.canAdventure());
        assertFalse(COBB_HAREM.canAdventure());
        assertFalse(COBB_TREASURY.canAdventure());
        assertFalse(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobAfterDecrypting() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(OUTSKIRTS_OF_THE_KNOB.canAdventure());
        assertTrue(COBB_BARRACKS.canAdventure());
        assertTrue(COBB_KITCHEN.canAdventure());
        assertTrue(COBB_HAREM.canAdventure());
        assertTrue(COBB_TREASURY.canAdventure());
        assertFalse(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void canDecryptMapToOpenCobbsKnob() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.ENCRYPTION_KEY),
              withItem(ItemPool.COBBS_KNOB_MAP),
              withQuestProgress(Quest.GOBLIN, QuestDatabase.STARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_encryption_key.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(COBB_BARRACKS.canAdventure());
        assertTrue(COBB_BARRACKS.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.COBBS_KNOB_MAP + "&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canVisitCobbsKnobAfterDefeatingKing() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(OUTSKIRTS_OF_THE_KNOB.canAdventure());
        assertTrue(COBB_BARRACKS.canAdventure());
        assertTrue(COBB_KITCHEN.canAdventure());
        assertTrue(COBB_HAREM.canAdventure());
        assertTrue(COBB_TREASURY.canAdventure());
        assertFalse(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void cannotVisitCobbsKnobLaboratoryWithoutKey() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertFalse(COBB_LABORATORY.canAdventure());
        assertFalse(KNOB_SHAFT.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobLaboratoryWithKey() {
      var cleanups =
          new Cleanups(withItem("Cobb's Knob lab key"), withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(COBB_LABORATORY.canAdventure());
        assertTrue(KNOB_SHAFT.canAdventure());
      }
    }

    @Test
    public void cannotVisitCobbsKnobMenagerieWithoutKey() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertFalse(MENAGERIE_LEVEL_1.canAdventure());
        assertFalse(MENAGERIE_LEVEL_2.canAdventure());
        assertFalse(MENAGERIE_LEVEL_3.canAdventure());
      }
    }

    @Test
    public void cannotVisitCobbsKnobMenagerieWithoutQuest() {
      var cleanups = new Cleanups(withItem("Cobb's Knob Menagerie key"));
      try (cleanups) {
        assertFalse(MENAGERIE_LEVEL_1.canAdventure());
        assertFalse(MENAGERIE_LEVEL_2.canAdventure());
        assertFalse(MENAGERIE_LEVEL_3.canAdventure());
      }
    }

    @Test
    public void canVisitCobbsKnobMenagerieWithKey() {
      var cleanups =
          new Cleanups(
              withItem("Cobb's Knob Menagerie key"), withQuestProgress(Quest.GOBLIN, "step1"));
      try (cleanups) {
        assertTrue(MENAGERIE_LEVEL_1.canAdventure());
        assertTrue(MENAGERIE_LEVEL_2.canAdventure());
        assertTrue(MENAGERIE_LEVEL_3.canAdventure());
      }
    }

    // Tests for fighting the King.  We've already confirmed that the
    // inside of the Knob must be open and the King not yet slain.

    // Can fight King as harem girl wearing outfit with effect

    @Test
    public void canFightKingGearedUpAsHaremGirl() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquipped(Slot.HAT, "Knob Goblin harem veil"),
              withEquipped(Slot.PANTS, "Knob Goblin harem pants"),
              withEffect(EffectPool.KNOB_GOBLIN_PERFUME));
      try (cleanups) {
        assertTrue(THRONE_ROOM.canAdventure());
        assertTrue(THRONE_ROOM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canFightKingUnEquippedAsHaremGirl() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquippableItem("Knob Goblin harem veil"),
              withEquippableItem("Knob Goblin harem pants"),
              withEffect(EffectPool.KNOB_GOBLIN_PERFUME));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HAREM_OUTFIT));
        assertTrue(THRONE_ROOM.canAdventure());
        assertTrue(THRONE_ROOM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.HAREM_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canFightKingUnPerfumedAsHaremGirl() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquipped(Slot.HAT, "Knob Goblin harem veil"),
              withEquipped(Slot.PANTS, "Knob Goblin harem pants"),
              withItem(ItemPool.KNOB_GOBLIN_PERFUME));
      try (cleanups) {
        assertTrue(THRONE_ROOM.canAdventure());
        assertTrue(THRONE_ROOM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.KNOB_GOBLIN_PERFUME + "&ajax=1");
      }
    }

    @Test
    public void cannotFightKingUnPerfumedAsHaremGirlInBeecore() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquipped(Slot.HAT, "Knob Goblin harem veil"),
              withEquipped(Slot.PANTS, "Knob Goblin harem pants"),
              withPath(Path.BEES_HATE_YOU));
      try (cleanups) {
        assertFalse(THRONE_ROOM.canAdventure());
      }
    }

    @Test
    public void canFightKingUnPerfumedUnGearedAsHaremGirl() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquippableItem("Knob Goblin harem veil"),
              withEquippableItem("Knob Goblin harem pants"),
              withItem(ItemPool.KNOB_GOBLIN_PERFUME));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HAREM_OUTFIT));
        assertTrue(THRONE_ROOM.canAdventure());
        assertTrue(THRONE_ROOM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.HAREM_OUTFIT + "&ajax=1");
        assertPostRequest(
            requests.get(1),
            "/inv_use.php",
            "whichitem=" + ItemPool.KNOB_GOBLIN_PERFUME + "&ajax=1");
      }
    }

    @Test
    public void canFightKingGearedUpAsGuard() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquipped(Slot.HAT, "Knob Goblin elite helm"),
              withEquipped(Slot.WEAPON, "Knob Goblin elite polearm"),
              withEquipped(Slot.PANTS, "Knob Goblin elite pants"),
              withItem(ItemPool.KNOB_CAKE));
      try (cleanups) {
        assertTrue(THRONE_ROOM.canAdventure());
        assertTrue(THRONE_ROOM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canFightKingUnGearedUpAsGuard() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquippableItem("Knob Goblin elite helm"),
              withEquippableItem("Knob Goblin elite polearm"),
              withEquippableItem("Knob Goblin elite pants"),
              withItem(ItemPool.KNOB_CAKE));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.KNOB_ELITE_OUTFIT));
        assertTrue(THRONE_ROOM.canAdventure());
        assertTrue(THRONE_ROOM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.KNOB_ELITE_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canFightKingUnCakedAsGuard() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquipped(Slot.HAT, "Knob Goblin elite helm"),
              withEquipped(Slot.WEAPON, "Knob Goblin elite polearm"),
              withEquipped(Slot.PANTS, "Knob Goblin elite pants"),
              withItem("unfrosted Knob cake"),
              withItem("Knob frosting"),
              withProperty("hasChef", true),
              withRange());
      try (cleanups) {
        assertTrue(THRONE_ROOM.canAdventure());
        assertTrue(THRONE_ROOM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/craft.php", "action=craft&mode=cook&ajax=1&a=4946&b=4945&qty=1");
      }
    }

    @Test
    public void canFightKingUnGearedUnCakedAsGuard() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GOBLIN, "step1"),
              withEquippableItem("Knob Goblin elite helm"),
              withEquippableItem("Knob Goblin elite polearm"),
              withEquippableItem("Knob Goblin elite pants"),
              withItem("unfrosted Knob cake"),
              withItem("Knob frosting"),
              withProperty("hasChef", true),
              withRange());
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.KNOB_ELITE_OUTFIT));
        assertTrue(THRONE_ROOM.canAdventure());
        assertTrue(THRONE_ROOM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.KNOB_ELITE_OUTFIT + "&ajax=1");
        assertPostRequest(
            requests.get(1), "/craft.php", "action=craft&mode=cook&ajax=1&a=4946&b=4945&qty=1");
      }
    }
  }

  @Nested
  class Pandammonium {
    private static final KoLAdventure PANDAMONIUM_SLUMS =
        AdventureDatabase.getAdventureByName("Pandamonium Slums");
    private static final KoLAdventure LAUGH_FLOOR =
        AdventureDatabase.getAdventureByName("The Laugh Floor");
    private static final KoLAdventure INFERNAL_RACKETS =
        AdventureDatabase.getAdventureByName("Infernal Rackets Backstage");

    // Quest.FRIAR progression
    //
    // "started" - given by Council
    // "step1" - spoke to friars without all three ritual items
    // "step2" - spoke to friars with all three ritual items
    // "finished - performed ritual
    //
    // I don't think you actually have to talk to the friars.
    //
    // Quest.AZAZEL
    //
    // "started" when you first visit Pandamonium.
    // Until then, the zones are not open.

    @Test
    public void canVisitPandamoniumWhenAzazelStarted() {
      var cleanups = new Cleanups(withQuestProgress(Quest.AZAZEL, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(PANDAMONIUM_SLUMS.canAdventure());
        assertTrue(LAUGH_FLOOR.canAdventure());
        assertTrue(INFERNAL_RACKETS.canAdventure());
      }
    }

    @Test
    public void canOpenPandamoniumWhenFriarsFinished() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.FRIAR, QuestDatabase.FINISHED),
              withQuestProgress(Quest.AZAZEL, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_pandamonium.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(PANDAMONIUM_SLUMS.canAdventure());
        assertTrue(PANDAMONIUM_SLUMS.prepareForAdventure());
        assertEquals(QuestDatabase.getQuest(Quest.AZAZEL), QuestDatabase.STARTED);

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertGetRequest(requests.get(0), "/pandamonium.php", null);
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canFinishFriarsToOpenPandamonium() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withPasswordHash("friars"),
              // If you have a password hash, KoL looks at your vinyl boots
              withGender(Gender.FEMALE),
              withQuestProgress(Quest.FRIAR, QuestDatabase.STARTED),
              withItem(ItemPool.DODECAGRAM),
              withItem(ItemPool.CANDLES),
              withItem(ItemPool.BUTTERKNIFE),
              withQuestProgress(Quest.AZAZEL, QuestDatabase.UNSTARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_friars_ritual.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_visit_pandamonium.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(PANDAMONIUM_SLUMS.canAdventure());
        assertTrue(PANDAMONIUM_SLUMS.prepareForAdventure());
        assertEquals(QuestDatabase.getQuest(Quest.FRIAR), QuestDatabase.FINISHED);
        assertEquals(0, InventoryManager.getCount(ItemPool.DODECAGRAM));
        assertEquals(0, InventoryManager.getCount(ItemPool.CANDLES));
        assertEquals(0, InventoryManager.getCount(ItemPool.BUTTERKNIFE));
        assertEquals(QuestDatabase.getQuest(Quest.AZAZEL), QuestDatabase.STARTED);

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));
        assertPostRequest(requests.get(0), "/friars.php", "action=ritual&pwd=friars");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertGetRequest(requests.get(2), "/pandamonium.php", null);
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Cyrpt {

    private static final KoLAdventure DEFILED_ALCOVE =
        AdventureDatabase.getAdventureByName("The Defiled Alcove");
    private static final KoLAdventure DEFILED_CRANNY =
        AdventureDatabase.getAdventureByName("The Defiled Cranny");
    private static final KoLAdventure DEFILED_NICHE =
        AdventureDatabase.getAdventureByName("The Defiled Niche");
    private static final KoLAdventure DEFILED_NOOK =
        AdventureDatabase.getAdventureByName("The Defiled Nook");
    private static final KoLAdventure HAERT =
        AdventureDatabase.getAdventureByName("Haert of the Cyrpt");

    @Test
    public void cannotVisitCyrptBeforeQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.CYRPT, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenQuestStarted() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptTotalEvilness", 200));
      try (cleanups) {
        assertTrue(DEFILED_ALCOVE.canAdventure());
        assertTrue(DEFILED_CRANNY.canAdventure());
        assertTrue(DEFILED_NICHE.canAdventure());
        assertTrue(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenAlcoveClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptTotalEvilness", 150));
      try (cleanups) {
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertTrue(DEFILED_CRANNY.canAdventure());
        assertTrue(DEFILED_NICHE.canAdventure());
        assertTrue(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenCrannyClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 0),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptTotalEvilness", 150));
      try (cleanups) {
        assertTrue(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertTrue(DEFILED_NICHE.canAdventure());
        assertTrue(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenNicheClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptNookEvilness", 50),
              withProperty("cyrptTotalEvilness", 150));
      try (cleanups) {
        assertTrue(DEFILED_ALCOVE.canAdventure());
        assertTrue(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertTrue(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
      }
    }

    @Test
    public void canVisitCyrptWhenNookClear() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 50),
              withProperty("cyrptCrannyEvilness", 50),
              withProperty("cyrptNicheEvilness", 50),
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptTotalEvilness", 150));
      try (cleanups) {
        assertTrue(DEFILED_ALCOVE.canAdventure());
        assertTrue(DEFILED_CRANNY.canAdventure());
        assertTrue(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
      }
    }

    @Test
    public void canVisitHaertWhenCyrptEvilness0() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptCrannyEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptTotalEvilness", 0));
      try (cleanups) {
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertTrue(HAERT.canAdventure());
      }
    }

    @Test
    public void canVisitHaertWhenCyrptEvilness999() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED),
              withProperty("cyrptAlcoveEvilness", 0),
              withProperty("cyrptCrannyEvilness", 0),
              withProperty("cyrptNicheEvilness", 0),
              withProperty("cyrptNookEvilness", 0),
              withProperty("cyrptTotalEvilness", 999));
      try (cleanups) {
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertTrue(HAERT.canAdventure());
      }
    }

    @Test
    public void cannotVisitCyrptWhenQuestFinished() {
      var cleanups = new Cleanups(withQuestProgress(Quest.CYRPT, QuestDatabase.FINISHED));
      try (cleanups) {
        assertFalse(DEFILED_ALCOVE.canAdventure());
        assertFalse(DEFILED_CRANNY.canAdventure());
        assertFalse(DEFILED_NICHE.canAdventure());
        assertFalse(DEFILED_NOOK.canAdventure());
        assertFalse(HAERT.canAdventure());
      }
    }
  }

  @Nested
  class McLargeHuge {

    private static final KoLAdventure ITZNOTYERZITZ_MINE =
        AdventureDatabase.getAdventureByName("Itznotyerzitz Mine");
    private static final KoLAdventure GOATLET = AdventureDatabase.getAdventureByName("The Goatlet");
    private static final KoLAdventure NINJA_SNOWMEN =
        AdventureDatabase.getAdventureByName("Lair of the Ninja Snowmen");
    private static final KoLAdventure EXTREME_SLOPE =
        AdventureDatabase.getAdventureByName("The eXtreme Slope");
    private static final KoLAdventure SHROUDED_PEAK =
        AdventureDatabase.getAdventureByName("Mist-Shrouded Peak");
    private static final KoLAdventure ICY_PEAK =
        AdventureDatabase.getAdventureByName("The Icy Peak");

    @Test
    public void cannotVisitMcLargeHugePreQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.TRAPPER, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(ITZNOTYERZITZ_MINE.canAdventure());
        assertFalse(GOATLET.canAdventure());
        assertFalse(NINJA_SNOWMEN.canAdventure());
        assertFalse(EXTREME_SLOPE.canAdventure());
        assertFalse(SHROUDED_PEAK.canAdventure());
        assertFalse(ICY_PEAK.canAdventure());
      }
    }

    @Test
    public void canTalkToTrapperToOpenZones() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.TRAPPER, QuestDatabase.STARTED));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_trapper_talk.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(GOATLET.canAdventure());
        assertTrue(GOATLET.prepareForAdventure());
        assertEquals(QuestDatabase.getQuest(Quest.TRAPPER), "step1");

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=mclargehuge&action=trappercabin");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canVisitMcLargeHugeOnceQuestStarted() {
      var cleanups = new Cleanups(withQuestProgress(Quest.TRAPPER, "step1"));
      try (cleanups) {
        assertTrue(ITZNOTYERZITZ_MINE.canAdventure());
        assertTrue(GOATLET.canAdventure());
        assertFalse(NINJA_SNOWMEN.canAdventure());
        assertFalse(EXTREME_SLOPE.canAdventure());
        assertFalse(SHROUDED_PEAK.canAdventure());
        assertFalse(ICY_PEAK.canAdventure());
      }
    }

    @Test
    public void canVisitMcLargeHugeAfterGivingTrapperItems() {
      var cleanups = new Cleanups(withQuestProgress(Quest.TRAPPER, "step2"));
      try (cleanups) {
        assertTrue(ITZNOTYERZITZ_MINE.canAdventure());
        assertTrue(GOATLET.canAdventure());
        assertTrue(NINJA_SNOWMEN.canAdventure());
        assertTrue(EXTREME_SLOPE.canAdventure());
        assertFalse(SHROUDED_PEAK.canAdventure());
        assertFalse(ICY_PEAK.canAdventure());
      }
    }

    @Test
    public void cannotVisitShroudedPeakWithoutColdResistance() {
      var cleanups = new Cleanups(withQuestProgress(Quest.TRAPPER, "step3"));
      try (cleanups) {
        // We do not currently allow betweenBattle script to fix
        assertFalse(SHROUDED_PEAK.canAdventure());
      }
    }

    @Test
    public void canVisitShroudedPeakWithColdResistance() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.TRAPPER, "step3"),
              withEquipped(Slot.ACCESSORY1, "cozy scarf"));
      try (cleanups) {
        // We do not currently allow betweenBattle script to fix
        assertTrue(SHROUDED_PEAK.canAdventure());
        assertTrue(SHROUDED_PEAK.prepareForAdventure());
      }
    }

    @Test
    public void cannotVisitShroudedPeakAfterGroar() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.TRAPPER, "step5"),
              withEquipped(Slot.ACCESSORY1, "cozy scarf"));
      try (cleanups) {
        assertFalse(SHROUDED_PEAK.canAdventure());
      }
    }

    @Test
    public void cannotVisitIcyPeakWithoutColdResistance() {
      var cleanups = new Cleanups(withQuestProgress(Quest.TRAPPER, "step5"));
      try (cleanups) {
        // We do not currently allow betweenBattle script to fix
        assertFalse(ICY_PEAK.canAdventure());
      }
    }

    @Test
    public void canVisitIcyPeakWithColdResistance() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.TRAPPER, "step5"),
              withEquipped(Slot.ACCESSORY1, "ghost of a necklace"));
      try (cleanups) {
        // We do not currently allow betweenBattle script to fix
        assertTrue(ICY_PEAK.canAdventure());
        assertTrue(ICY_PEAK.prepareForAdventure());
      }
    }

    @Test
    public void cannotVisitIcyPeakBeforeGroar() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.TRAPPER, "step4"),
              withEquipped(Slot.ACCESSORY1, "ghost of a necklace"));
      try (cleanups) {
        assertFalse(SHROUDED_PEAK.canAdventure());
      }
    }
  }

  @Nested
  class BeanStalk {

    private static final KoLAdventure AIRSHIP =
        AdventureDatabase.getAdventureByName("The Penultimate Fantasy Airship");
    private static final KoLAdventure CASTLE_BASEMENT =
        AdventureDatabase.getAdventureByName("The Castle in the Clouds in the Sky (Basement)");
    private static final KoLAdventure CASTLE_GROUND =
        AdventureDatabase.getAdventureByName("The Castle in the Clouds in the Sky (Ground Floor)");
    private static final KoLAdventure CASTLE_TOP =
        AdventureDatabase.getAdventureByName("The Castle in the Clouds in the Sky (Top Floor)");
    private static final KoLAdventure HOLE_IN_THE_SKY =
        AdventureDatabase.getAdventureByName("The Hole in the Sky");

    @Test
    public void cannotVisitBeanStalkPreQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GARBAGE, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(AIRSHIP.canAdventure());
        assertFalse(CASTLE_BASEMENT.canAdventure());
        assertFalse(CASTLE_GROUND.canAdventure());
        assertFalse(CASTLE_TOP.canAdventure());
        assertFalse(HOLE_IN_THE_SKY.canAdventure());
      }
    }

    @Test
    public void cannotVisitBeanStalkWithNoBean() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GARBAGE, QuestDatabase.STARTED));
      try (cleanups) {
        assertFalse(AIRSHIP.canAdventure());
        assertFalse(CASTLE_BASEMENT.canAdventure());
        assertFalse(CASTLE_GROUND.canAdventure());
        assertFalse(CASTLE_TOP.canAdventure());
        assertFalse(HOLE_IN_THE_SKY.canAdventure());
      }
    }

    @Test
    public void canPlantBeanIfNecessary() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GARBAGE, QuestDatabase.STARTED), withItem("enchanted bean"));
      try (cleanups) {
        assertTrue(AIRSHIP.canAdventure());
        assertTrue(AIRSHIP.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=plains&action=garbage_grounds");
      }
    }

    @Test
    public void canVisitAirshipWithBeanStalkWithBeanPlanted() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GARBAGE, "step1"));
      try (cleanups) {
        assertTrue(AIRSHIP.canAdventure());
        assertFalse(CASTLE_BASEMENT.canAdventure());
        assertFalse(CASTLE_GROUND.canAdventure());
        assertFalse(CASTLE_TOP.canAdventure());
        assertFalse(HOLE_IN_THE_SKY.canAdventure());
      }
    }

    @Test
    public void canVisitSomeBeanstalkZonesInExploathing() {
      var cleanups = new Cleanups(withPath(Path.KINGDOM_OF_EXPLOATHING));
      try (cleanups) {
        assertFalse(AIRSHIP.canAdventure());
        assertTrue(CASTLE_BASEMENT.canAdventure());
        assertTrue(HOLE_IN_THE_SKY.canAdventure());
      }
    }

    @Test
    public void canVisitCastleWithSOCK() {
      var cleanups = new Cleanups(withQuestProgress(Quest.GARBAGE, "step1"), withItem("S.O.C.K."));
      try (cleanups) {
        assertTrue(CASTLE_BASEMENT.canAdventure());
      }
    }

    @Test
    public void canVisitCastleWithRowboat() {
      var cleanups = new Cleanups(withItem("intragalactic rowboat"));
      try (cleanups) {
        assertTrue(CASTLE_BASEMENT.canAdventure());
      }
    }

    @Test
    public void canVisitCastleGroundFloorIfUnlocked() {
      var cleanups = new Cleanups(withAscensions(13), withProperty("lastCastleGroundUnlock", 13));
      try (cleanups) {
        assertTrue(CASTLE_GROUND.canAdventure());
      }
    }

    @Test
    public void canVisitCastleTopFloorIfUnlocked() {
      var cleanups = new Cleanups(withAscensions(13), withProperty("lastCastleTopUnlock", 13));
      try (cleanups) {
        assertTrue(CASTLE_TOP.canAdventure());
      }
    }

    @Test
    public void canVisitHoleInTheSkyWithRocketship() {
      var cleanups = new Cleanups(withItem("steam-powered model rocketship"));
      try (cleanups) {
        assertTrue(HOLE_IN_THE_SKY.canAdventure());
      }
    }

    @Test
    public void canVisitHoleInTheSkyWithRowboat() {
      var cleanups = new Cleanups(withItem("intragalactic rowboat"));
      try (cleanups) {
        assertTrue(HOLE_IN_THE_SKY.canAdventure());
      }
    }
  }

  @Nested
  class HiddenCity {

    private static final KoLAdventure HIDDEN_PARK =
        AdventureDatabase.getAdventureByName("The Hidden Park");
    private static final KoLAdventure NW_SHRINE =
        AdventureDatabase.getAdventureByName("An Overgrown Shrine (Northwest)");
    private static final KoLAdventure SW_SHRINE =
        AdventureDatabase.getAdventureByName("An Overgrown Shrine (Southwest)");
    private static final KoLAdventure NE_SHRINE =
        AdventureDatabase.getAdventureByName("An Overgrown Shrine (Northeast)");
    private static final KoLAdventure SE_SHRINE =
        AdventureDatabase.getAdventureByName("An Overgrown Shrine (Southeast)");
    private static final KoLAdventure ZIGGURAT =
        AdventureDatabase.getAdventureByName("A Massive Ziggurat");
    private static final KoLAdventure HIDDEN_APARTMENT =
        AdventureDatabase.getAdventureByName("The Hidden Apartment Building");
    private static final KoLAdventure HIDDEN_HOSPITAL =
        AdventureDatabase.getAdventureByName("The Hidden Hospital");
    private static final KoLAdventure HIDDEN_OFFICE =
        AdventureDatabase.getAdventureByName("The Hidden Office Building");
    private static final KoLAdventure HIDDEN_BOWLING_ALLEY =
        AdventureDatabase.getAdventureByName("The Hidden Bowling Alley");

    @Test
    public void cannotVisitHiddenCityPreQuest() {
      var cleanups = new Cleanups(withQuestProgress(Quest.WORSHIP, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(HIDDEN_PARK.canAdventure());
        assertFalse(NW_SHRINE.canAdventure());
        assertFalse(HIDDEN_APARTMENT.canAdventure());
        assertFalse(NE_SHRINE.canAdventure());
        assertFalse(HIDDEN_OFFICE.canAdventure());
        assertFalse(SW_SHRINE.canAdventure());
        assertFalse(HIDDEN_HOSPITAL.canAdventure());
        assertFalse(SE_SHRINE.canAdventure());
        assertFalse(HIDDEN_BOWLING_ALLEY.canAdventure());
        assertFalse(ZIGGURAT.canAdventure());
      }
    }

    @Test
    public void canVisitHiddenCityOnceOpened() {
      var cleanups = new Cleanups(withQuestProgress(Quest.WORSHIP, "step3"));
      try (cleanups) {
        assertTrue(HIDDEN_PARK.canAdventure());
        assertTrue(NW_SHRINE.canAdventure());
        assertTrue(NE_SHRINE.canAdventure());
        assertTrue(SW_SHRINE.canAdventure());
        assertTrue(SE_SHRINE.canAdventure());
        assertTrue(ZIGGURAT.canAdventure());
        assertFalse(HIDDEN_APARTMENT.canAdventure());
        assertFalse(HIDDEN_OFFICE.canAdventure());
        assertFalse(HIDDEN_HOSPITAL.canAdventure());
        assertFalse(HIDDEN_BOWLING_ALLEY.canAdventure());
      }
    }

    @Test
    public void canVisitHiddenApartmentBuildingOnceOpened() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.WORSHIP, "step3"),
              withQuestProgress(Quest.CURSES, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(HIDDEN_APARTMENT.canAdventure());
      }
    }

    @Test
    public void canVisitHiddenOfficeBuildingOnceOpened() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.WORSHIP, "step3"),
              withQuestProgress(Quest.BUSINESS, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(HIDDEN_OFFICE.canAdventure());
      }
    }

    @Test
    public void canVisitHiddenHospitalOnceOpened() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.WORSHIP, "step3"),
              withQuestProgress(Quest.DOCTOR, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(HIDDEN_HOSPITAL.canAdventure());
      }
    }

    @Test
    public void canVisitHiddenBowlingAlleyOnceOpened() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.WORSHIP, "step3"),
              withQuestProgress(Quest.SPARE, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(HIDDEN_BOWLING_ALLEY.canAdventure());
      }
    }
  }

  @Nested
  class Palindome {
    private static final KoLAdventure PALINDOME =
        AdventureDatabase.getAdventureByName("Inside the Palindome");

    @Test
    public void cannotVisitPalindomeWithoutTalisman() {
      var cleanups = new Cleanups();
      try (cleanups) {
        assertFalse(PALINDOME.canAdventure());
      }
    }

    @Test
    public void canVisitPalindomeWithTalismanEquipped() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder), withEquipped(Slot.ACCESSORY1, ItemPool.TALISMAN));
      try (cleanups) {
        assertTrue(PALINDOME.canAdventure());
        assertTrue(PALINDOME.prepareForAdventure());
        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canEquipTalismanFromInventory() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withEquippableItem(ItemPool.TALISMAN));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_palindome_equip_talisman.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(PALINDOME.canAdventure());
        assertTrue(PALINDOME.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&slot=1&action=equip&whichitem=" + ItemPool.TALISMAN);
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void cannotVisitPalindomeWithTalismanComponentsAndNoMeat() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.COPPERHEAD_CHARM),
              withItem(ItemPool.COPPERHEAD_CHARM_RAMPANT),
              withMeat(0));
      try (cleanups) {
        assertFalse(PALINDOME.canAdventure());
      }
    }

    @Test
    public void canCreateTalismanAndEquipWithMeat() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.COPPERHEAD_CHARM),
              withItem(ItemPool.COPPERHEAD_CHARM_RAMPANT),
              withMeat(10));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_palindome_make_paste.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_visit_palindome_make_talisman.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_visit_palindome_equip_talisman.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(PALINDOME.canAdventure());
        assertTrue(PALINDOME.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(6));
        assertPostRequest(
            requests.get(0), "/craft.php", "action=makepaste&whichitem=25&ajax=1&qty=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(
            requests.get(2), "/craft.php", "action=craft&mode=combine&ajax=1&a=7178&b=7186&qty=1");
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(
            requests.get(4),
            "/inv_equip.php",
            "which=2&ajax=1&slot=1&action=equip&whichitem=" + ItemPool.TALISMAN);
        assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canCreateTalismanAndEquipWithThePlunger() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.COPPERHEAD_CHARM),
              withItem(ItemPool.COPPERHEAD_CHARM_RAMPANT),
              withSign(ZodiacSign.VOLE));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_palindome_make_talisman.html"));
        client.addResponse(200, ""); // api.php
        client.addResponse(200, html("request/test_visit_palindome_equip_talisman.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(PALINDOME.canAdventure());
        assertTrue(PALINDOME.prepareForAdventure());

        var requests = client.getRequests();
        assertThat(requests, hasSize(4));
        assertPostRequest(
            requests.get(0), "/craft.php", "action=craft&mode=combine&ajax=1&a=7178&b=7186&qty=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(
            requests.get(2),
            "/inv_equip.php",
            "which=2&ajax=1&slot=1&action=equip&whichitem=" + ItemPool.TALISMAN);
        assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Pirate {

    private static final KoLAdventure PIRATE_COVE =
        AdventureDatabase.getAdventureByName("The Obligatory Pirate's Cove");
    private static final KoLAdventure BARRRNEYS_BARRR =
        AdventureDatabase.getAdventureByName("Barrrney's Barrr");
    private static final KoLAdventure FCLE = AdventureDatabase.getAdventureByName("The F'c'le");
    private static final KoLAdventure POOP_DECK =
        AdventureDatabase.getAdventureByName("The Poop Deck");
    private static final KoLAdventure BELOWDECKS =
        AdventureDatabase.getAdventureByName("Belowdecks");

    @Test
    public void cannotVisitPiratesWithoutIslandAccess() {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(PIRATE_COVE.canAdventure());
        assertFalse(BARRRNEYS_BARRR.canAdventure());
        assertFalse(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPiratesUndisguised() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(PIRATE_COVE.canAdventure());
        assertFalse(BARRRNEYS_BARRR.canAdventure());
        assertFalse(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void cannotVisitPiratesDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(PIRATE_COVE.canAdventure());
        assertFalse(BARRRNEYS_BARRR.canAdventure());
        assertFalse(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipDisguised() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertTrue(POOP_DECK.canAdventure());
        assertTrue(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canPrepareToAdventureWearingPirateOutfit() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquipped(Slot.HAT, "eyepatch"),
              withEquipped(Slot.PANTS, "swashbuckling pants"),
              withEquipped(Slot.ACCESSORY1, "stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(BARRRNEYS_BARRR.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareToAdventureNotWearingPirateOutfit() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(BARRRNEYS_BARRR.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.SWASHBUCKLING_GETUP + "&ajax=1");
      }
    }

    @Test
    public void canVisitPirateShipFledged() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("pirate fledges"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertTrue(POOP_DECK.canAdventure());
        assertTrue(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canPrepareToAdventureWearingFledges() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquipped(Slot.ACCESSORY1, "pirate fledges"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(BARRRNEYS_BARRR.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareToAdventureNotWearingFledges() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("pirate fledges"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(BARRRNEYS_BARRR.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&slot=1&action=equip&whichitem=" + ItemPool.PIRATE_FLEDGES);
      }
    }

    @Test
    public void canVisitPirateShipBeforeQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertFalse(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipFcleDuringQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, "step5"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertFalse(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipPoopDeckDuringQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, "step6"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertTrue(POOP_DECK.canAdventure());
        assertFalse(BELOWDECKS.canAdventure());
      }
    }

    @Test
    public void canVisitPirateShipBelowdecksAfterQuest() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("eyepatch"),
              withEquippableItem("swashbuckling pants"),
              withEquippableItem("stuffed shoulder parrot"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withQuestProgress(Quest.PIRATE, QuestDatabase.FINISHED));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP));
        assertTrue(BARRRNEYS_BARRR.canAdventure());
        assertTrue(FCLE.canAdventure());
        assertTrue(POOP_DECK.canAdventure());
        assertTrue(BELOWDECKS.canAdventure());
      }
    }
  }

  @Nested
  class HippyCamp {

    // Adventures available in The Hippy Camp on the Mysterious Island depend
    // on whether the island is peaceful, on the verge of war, or at
    // war. Post-war, the camp may revert to its prewar peace - or may be
    // bombed back to the stone age, if the hippies lost the war.
    //
    // Wearing a hippy or fratboy disguise also changes available encounters.
    //
    // Externally (the image in the browser), the adventure URL always goes to
    // AdventurePool.HIPPY_CAMP, regardless of quest state or disguise. But
    // after adventuring, the "Adventure Again" link in the response and the
    // "Last Adventure" link in the charpane may refer to a different
    // URL. These URLs can also be used, regardless of whether the conditions
    // still hold.
    //
    // AdventurePool.HIPPY_CAMP
    // AdventurePool.HIPPY_CAMP_DISGUISED
    // AdventurePool.WARTIME_HIPPY_CAMP
    // AdventurePool.WARTIME_HIPPY_CAMP_DISGUISED
    //
    // If the hippies lost the war:
    //
    // AdventurePool.BOMBED_HIPPY_CAMP

    private static final KoLAdventure HIPPY_CAMP =
        AdventureDatabase.getAdventureByName("Hippy Camp");
    private static final KoLAdventure HIPPY_CAMP_DISGUISED =
        AdventureDatabase.getAdventureByName("Hippy Camp (Hippy Disguise)");
    private static final KoLAdventure WARTIME_HIPPY_CAMP =
        AdventureDatabase.getAdventureByName("Wartime Hippy Camp");
    private static final KoLAdventure WARTIME_HIPPY_CAMP_DISGUISED =
        AdventureDatabase.getAdventureByName("Wartime Hippy Camp (Frat Disguise)");
    private static final KoLAdventure BOMBED_HIPPY_CAMP =
        AdventureDatabase.getAdventureByName("The Hippy Camp (Bombed Back to the Stone Age)");

    @Test
    public void cannotVisitHippyCampWithoutIslandAccess() {
      assertFalse(HIPPY_CAMP.canAdventure());
      assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
      assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
      assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
      assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
    }

    @Test
    public void canVisitHippyCampBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampInDisguiseBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquipped(Slot.HAT, "filthy knitted dread sack"),
              withEquipped(Slot.PANTS, "filthy corduroys"));
      try (cleanups) {
        assertTrue(HIPPY_CAMP.canAdventure());
        // We check only quest status, not available equipment
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureDisguisedEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquipped(Slot.HAT, "filthy knitted dread sack"),
              withEquipped(Slot.PANTS, "filthy corduroys"));
      try (cleanups) {
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureDisguisedUnEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickHippyOutfitToAdventureBeforeWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              // Have War Hippy Fatigues
              withItem("reinforced beaded headband"),
              withItem("bullet-proof corduroys"),
              withItem("round purple sunglasses"),
              // Filthy Hippy Disguise
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertFalse(EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT));
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickWarHippyOutfitToAdventureBeforeWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              // Have War Hippy Fatigues
              withEquippableItem("reinforced beaded headband"),
              withEquippableItem("bullet-proof corduroys"),
              withEquippableItem("round purple sunglasses"),
              // Filthy Hippy Disguise
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT));
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.WAR_HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canVisitHippyCampOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        // KoL does not require going directly to verge-of-war zones
        assertTrue(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampInDisguiseOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              withEquipped(Slot.HAT, "Orcish baseball cap"),
              withEquipped(Slot.PANTS, "Orcish cargo shorts"),
              withEquipped(Slot.WEAPON, "Orcish frat-paddle"));
      try (cleanups) {
        // KoL does not require going directly to verge-of-war zones
        assertTrue(HIPPY_CAMP.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        // ... but it allows it.
        assertTrue(WARTIME_HIPPY_CAMP.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canPickFratOutfitToAdventureOnVergeOfWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              // War Frat Fatigues
              withItem("beer helmet"),
              withItem("distressed denim pants"),
              withItem("bejeweled pledge pin"),
              // Frat Boy Ensemble
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertFalse(EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT));
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickWarFratOutfitToAdventureOnVergeOfWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              // War Frat Fatigues
              withEquippableItem("beer helmet"),
              withEquippableItem("distressed denim pants"),
              withEquippableItem("bejeweled pledge pin"),
              // Frat Boy Ensemble
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT));
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.WAR_FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void cannotVisitHippyCampDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampAfterWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "fratboys"));
      try (cleanups) {
        assertTrue(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canVisitHippyCampInDisguiseAfterWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "fratboys"),
              withEquipped(Slot.HAT, "filthy knitted dread sack"),
              withEquipped(Slot.PANTS, "filthy corduroys"));
      try (cleanups) {
        assertTrue(HIPPY_CAMP.canAdventure());
        assertTrue(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canVisitBombedHippyCampAfterLostWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "hippies"));
      try (cleanups) {
        assertFalse(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void canVisitBombedHippyCampAfterWossname() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "both"));
      try (cleanups) {
        assertFalse(HIPPY_CAMP.canAdventure());
        assertFalse(HIPPY_CAMP_DISGUISED.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP.canAdventure());
        assertFalse(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(BOMBED_HIPPY_CAMP.canAdventure());
      }
    }

    @Test
    public void changesIntoWarFratOutfitIfNecessary() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withEquippableItem("beer helmet"),
              withEquippableItem("distressed denim pants"),
              withEquippableItem("bejeweled pledge pin"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));

      try (cleanups) {
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.canAdventure());
        assertTrue(WARTIME_HIPPY_CAMP_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_equip.php", "which=2&action=outfit&whichoutfit=33&ajax=1");
      }
    }
  }

  @Nested
  class FratHouse {

    // Adventures available in The Frat House on the Mysterious Island depend
    // on whether the island is peaceful, on the verge of war, or at
    // war. Post-war, the camp may revert to its prewar peace - or may be
    // bombed back to the stone age, if the hippies lost the war.
    //
    // Wearing a hippy or fratboy disguise also changes available encounters.
    //
    // Externally (the image in the browser), the adventure URL always goes to
    // AdventurePool.FRAT_HOUSE, regardless of quest state or disguise. But
    // after adventuring, the "Adventure Again" link in the response and the
    // "Last Adventure" link in the charpane may refer to a different
    // URL. These URLs can also be used, regardless of whether the conditions
    // still hold.
    //
    // AdventurePool.FRAT_HOUSE
    // AdventurePool.FRAT_HOUSE_DISGUISED
    // AdventurePool.WARTIME_FRAT_HOUSE
    // AdventurePool.WARTIME_FRAT_HOUSE_DISGUISED
    //
    // If the hippies lost the war:
    //
    // AdventurePool.BOMBED_FRAT_HOUSE

    private static final KoLAdventure FRAT_HOUSE =
        AdventureDatabase.getAdventureByName("Frat House");
    private static final KoLAdventure FRAT_HOUSE_DISGUISED =
        AdventureDatabase.getAdventureByName("Frat House (Frat Disguise)");
    private static final KoLAdventure WARTIME_FRAT_HOUSE =
        AdventureDatabase.getAdventureByName("Wartime Frat House");
    private static final KoLAdventure WARTIME_FRAT_HOUSE_DISGUISED =
        AdventureDatabase.getAdventureByName("Wartime Frat House (Hippy Disguise)");
    private static final KoLAdventure BOMBED_FRAT_HOUSE =
        AdventureDatabase.getAdventureByName(
            "The Orcish Frat House (Bombed Back to the Stone Age)");

    @Test
    public void cannotVisitFratHouseWithoutIslandAccess() {
      assertFalse(FRAT_HOUSE.canAdventure());
      assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
      assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
      assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
      assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
    }

    @Test
    public void canVisitFratHouseBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertTrue(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseInDisguiseBeforeWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquipped(Slot.HAT, "Orcish baseball cap"),
              withEquipped(Slot.PANTS, "Orcish cargo shorts"),
              withEquipped(Slot.WEAPON, "Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(FRAT_HOUSE.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canPrepareForAdventureDisguisedEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquipped(Slot.HAT, "Orcish baseball cap"),
              withEquipped(Slot.PANTS, "Orcish cargo shorts"),
              withEquipped(Slot.WEAPON, "Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureDisguisedUnEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickFratOutfitToAdventureBeforeWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              // War Frat Fatigues
              withItem("beer helmet"),
              withItem("distressed denim pants"),
              withItem("bejeweled pledge pin"),
              // Frat Boy Ensemble
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertFalse(EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT));
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickWarFratOutfitToAdventureBeforeWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.UNSTARTED),
              // War Frat Fatigues
              withEquippableItem("beer helmet"),
              withEquippableItem("distressed denim pants"),
              withEquippableItem("bejeweled pledge pin"),
              // Frat Boy Ensemble
              withEquippableItem("Orcish baseball cap"),
              withEquippableItem("Orcish cargo shorts"),
              withEquippableItem("Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT));
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.WAR_FRAT_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canVisitFratHouseOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        // KoL does not require going directly to verge-of-war zones
        assertTrue(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        // ... but it allows it.
        assertTrue(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseInDisguiseOnVergeOfWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              withEquipped(Slot.HAT, "filthy knitted dread sack"),
              withEquipped(Slot.PANTS, "filthy corduroys"));
      try (cleanups) {
        // KoL does not require going directly to verge-of-war zones
        assertTrue(FRAT_HOUSE.canAdventure());
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        // ... but it allows it.
        assertTrue(WARTIME_FRAT_HOUSE.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canPickHippyOutfitToAdventureOnVergeOfWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              // Have War Hippy Fatigues
              withItem("reinforced beaded headband"),
              withItem("bullet-proof corduroys"),
              withItem("round purple sunglasses"),
              // Filthy Hippy Disguise
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertFalse(EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT));
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void canPickWarFratOutfitToAdventureOnVergeOfWar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED),
              // Have War Hippy Fatigues
              withEquippableItem("reinforced beaded headband"),
              withEquippableItem("bullet-proof corduroys"),
              withEquippableItem("round purple sunglasses"),
              // Filthy Hippy Disguise
              withEquippableItem("filthy knitted dread sack"),
              withEquippableItem("filthy corduroys"));
      try (cleanups) {
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT));
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT));
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(WARTIME_FRAT_HOUSE_DISGUISED.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&action=outfit&whichoutfit=" + OutfitPool.WAR_HIPPY_OUTFIT + "&ajax=1");
      }
    }

    @Test
    public void cannotVisitFratHouseDuringWar() {
      var cleanups =
          new Cleanups(withItem("dingy dinghy"), withQuestProgress(Quest.ISLAND_WAR, "step1"));
      try (cleanups) {
        assertFalse(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseAfterWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "hippies"));
      try (cleanups) {
        assertTrue(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canVisitFratHouseInDisguiseAfterWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "hippies"),
              withEquipped(Slot.HAT, "Orcish baseball cap"),
              withEquipped(Slot.PANTS, "Orcish cargo shorts"),
              withEquipped(Slot.WEAPON, "Orcish frat-paddle"));
      try (cleanups) {
        assertTrue(FRAT_HOUSE.canAdventure());
        assertTrue(EquipmentManager.hasOutfit(OutfitPool.FRAT_OUTFIT));
        assertTrue(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canVisitBombedFratHouseAfterLostWar() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "fratboys"));
      try (cleanups) {
        assertFalse(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }

    @Test
    public void canVisitBombedFratHouseAfterWossname() {
      var cleanups =
          new Cleanups(
              withItem("dingy dinghy"),
              withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.FINISHED),
              withProperty("sideDefeated", "both"));
      try (cleanups) {
        assertFalse(FRAT_HOUSE.canAdventure());
        assertFalse(FRAT_HOUSE_DISGUISED.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE.canAdventure());
        assertFalse(WARTIME_FRAT_HOUSE_DISGUISED.canAdventure());
        assertTrue(BOMBED_FRAT_HOUSE.canAdventure());
      }
    }
  }

  @Nested
  class Farm {
    private static final KoLAdventure THE_BARN =
        AdventureDatabase.getAdventureByName("McMillicancuddy's Barn");
    private static final KoLAdventure THE_POND =
        AdventureDatabase.getAdventureByName("McMillicancuddy's Pond");
    private static final KoLAdventure THE_BACK_40 =
        AdventureDatabase.getAdventureByName("McMillicancuddy's Back 40");
    private static final KoLAdventure THE_OTHER_BACK_40 =
        AdventureDatabase.getAdventureByName("McMillicancuddy's Other Back 40");
    private static final KoLAdventure THE_GRANARY =
        AdventureDatabase.getAdventureByName("McMillicancuddy's Granary");
    private static final KoLAdventure THE_BOG =
        AdventureDatabase.getAdventureByName("McMillicancuddy's Bog");
    private static final KoLAdventure THE_FAMILY_PLOT =
        AdventureDatabase.getAdventureByName("McMillicancuddy's Family Plot");
    private static final KoLAdventure THE_SHADY_THICKET =
        AdventureDatabase.getAdventureByName("McMillicancuddy's Shady Thicket");

    @Test
    public void cannotAdventureUnlessAtWar() {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, QuestDatabase.STARTED));
      try (cleanups) {
        assertFalse(THE_BARN.canAdventure());
        assertFalse(THE_POND.canAdventure());
        assertFalse(THE_BACK_40.canAdventure());
        assertFalse(THE_OTHER_BACK_40.canAdventure());
        assertFalse(THE_GRANARY.canAdventure());
        assertFalse(THE_BOG.canAdventure());
        assertFalse(THE_FAMILY_PLOT.canAdventure());
        assertFalse(THE_SHADY_THICKET.canAdventure());
      }
    }

    @Test
    public void cannotAdventureWhenSidequestCompleted() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "start1"),
              withProperty("sidequestFarmCompleted", "hippies"));
      try (cleanups) {
        assertFalse(THE_BARN.canAdventure());
        assertFalse(THE_POND.canAdventure());
        assertFalse(THE_BACK_40.canAdventure());
        assertFalse(THE_OTHER_BACK_40.canAdventure());
        assertFalse(THE_GRANARY.canAdventure());
        assertFalse(THE_BOG.canAdventure());
        assertFalse(THE_FAMILY_PLOT.canAdventure());
        assertFalse(THE_SHADY_THICKET.canAdventure());
      }
    }

    @Test
    public void cannotAdventureAfterLocationIsCleared() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withProperty(
                  "duckAreasSelected",
                  THE_POND.getAdventureId()
                      + ","
                      + THE_GRANARY.getAdventureId()
                      + ","
                      + THE_SHADY_THICKET.getAdventureId()),
              withProperty("duckAreasCleared", ""),
              withLastLocation(THE_GRANARY));
      try (cleanups) {
        assertTrue(THE_GRANARY.canAdventure());
        var request = new GenericRequest("adventure.php?snarfblat=" + AdventurePool.THE_GRANARY);
        request.responseText = html("request/test_no_more_ducks.html");
        QuestManager.handleQuestChange(request);
        assertEquals(Preferences.getString("duckAreasCleared"), THE_GRANARY.getAdventureId());
        assertFalse(THE_GRANARY.canAdventure());
      }
    }

    @Test
    public void canAdventureInBarnWithZeroSelected() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"), withProperty("duckAreasSelected", ""));
      try (cleanups) {
        assertTrue(THE_BARN.canAdventure());
        assertFalse(THE_POND.canAdventure());
        assertFalse(THE_BACK_40.canAdventure());
        assertFalse(THE_OTHER_BACK_40.canAdventure());
        assertFalse(THE_GRANARY.canAdventure());
        assertFalse(THE_BOG.canAdventure());
        assertFalse(THE_FAMILY_PLOT.canAdventure());
        assertFalse(THE_SHADY_THICKET.canAdventure());
      }
    }

    @Test
    public void canAdventureInBarnWithOneSelected() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withProperty("duckAreasSelected", THE_POND.getAdventureId()));
      try (cleanups) {
        assertTrue(THE_BARN.canAdventure());
        assertFalse(THE_POND.canAdventure());
        assertFalse(THE_BACK_40.canAdventure());
        assertFalse(THE_OTHER_BACK_40.canAdventure());
        assertFalse(THE_GRANARY.canAdventure());
        assertFalse(THE_BOG.canAdventure());
        assertFalse(THE_FAMILY_PLOT.canAdventure());
        assertFalse(THE_SHADY_THICKET.canAdventure());
      }
    }

    @Test
    public void canAdventureInBarnWithTwoSelected() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withProperty(
                  "duckAreasSelected",
                  THE_POND.getAdventureId() + "," + THE_GRANARY.getAdventureId()));
      try (cleanups) {
        assertTrue(THE_BARN.canAdventure());
        assertFalse(THE_POND.canAdventure());
        assertFalse(THE_BACK_40.canAdventure());
        assertFalse(THE_OTHER_BACK_40.canAdventure());
        assertFalse(THE_GRANARY.canAdventure());
        assertFalse(THE_BOG.canAdventure());
        assertFalse(THE_FAMILY_PLOT.canAdventure());
        assertFalse(THE_SHADY_THICKET.canAdventure());
      }
    }

    @Test
    public void canAdventureWithSelected() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withProperty(
                  "duckAreasSelected",
                  THE_POND.getAdventureId()
                      + ","
                      + THE_GRANARY.getAdventureId()
                      + ","
                      + THE_SHADY_THICKET.getAdventureId()));
      try (cleanups) {
        assertFalse(THE_BARN.canAdventure());
        assertTrue(THE_POND.canAdventure());
        assertFalse(THE_BACK_40.canAdventure());
        assertFalse(THE_OTHER_BACK_40.canAdventure());
        assertTrue(THE_GRANARY.canAdventure());
        assertFalse(THE_BOG.canAdventure());
        assertFalse(THE_FAMILY_PLOT.canAdventure());
        assertTrue(THE_SHADY_THICKET.canAdventure());
      }
    }

    @Test
    public void cannotAdventureIfCleared() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withProperty(
                  "duckAreasSelected",
                  THE_POND.getAdventureId()
                      + ","
                      + THE_GRANARY.getAdventureId()
                      + ","
                      + THE_SHADY_THICKET.getAdventureId()),
              withProperty("duckAreasCleared", THE_GRANARY.getAdventureId()));
      try (cleanups) {
        assertFalse(THE_BARN.canAdventure());
        assertTrue(THE_POND.canAdventure());
        assertFalse(THE_BACK_40.canAdventure());
        assertFalse(THE_OTHER_BACK_40.canAdventure());
        assertFalse(THE_GRANARY.canAdventure());
        assertFalse(THE_BOG.canAdventure());
        assertFalse(THE_FAMILY_PLOT.canAdventure());
        assertTrue(THE_SHADY_THICKET.canAdventure());
      }
    }
  }

  @Nested
  class RabbitHole {
    private static final KoLAdventure RABBIT_HOLE =
        AdventureDatabase.getAdventureByName("The Red Queen's Garden");

    @Test
    public void cannotAdventureWithoutEffectOrItem() {
      assertThat(RABBIT_HOLE.canAdventure(), is(false));
    }

    @Test
    public void canAdventureWithEffectActive() {
      var cleanups = new Cleanups(withEffect(EffectPool.DOWN_THE_RABBIT_HOLE));
      try (cleanups) {
        assertThat(RABBIT_HOLE.canAdventure(), is(true));
      }
    }

    @Test
    public void canAdventureWithItemInInventory() {
      var cleanups = new Cleanups(withItem(ItemPool.DRINK_ME_POTION));
      try (cleanups) {
        assertThat(RABBIT_HOLE.canAdventure(), is(true));
      }
    }

    @Test
    public void cannotPrepareForAdventureWithoutItemAndEffect() {
      assertThat(RABBIT_HOLE.prepareForAdventure(), is(false));
    }

    @Test
    public void canPrepareForAdventureWithEffect() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withEffect(EffectPool.DOWN_THE_RABBIT_HOLE), withItem(ItemPool.DRINK_ME_POTION));
      try (cleanups) {
        assertTrue(RABBIT_HOLE.canAdventure());
        assertTrue(RABBIT_HOLE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureWithItem() {
      setupFakeClient();

      var cleanups = new Cleanups(withItem(ItemPool.DRINK_ME_POTION));
      try (cleanups) {
        assertTrue(RABBIT_HOLE.canAdventure());
        assertTrue(RABBIT_HOLE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.DRINK_ME_POTION + "&ajax=1");
      }
    }
  }

  @Nested
  class Suburbs {
    private static final KoLAdventure GROVE =
        AdventureDatabase.getAdventureByName("The Clumsiness Grove");
    private static final KoLAdventure MAELSTROM =
        AdventureDatabase.getAdventureByName("The Maelstrom of Lovers");
    private static final KoLAdventure GLACIER =
        AdventureDatabase.getAdventureByName("The Glacier of Jerks");

    @Test
    public void cannotAdventureWithoutEffectOrItem() {
      assertThat(GROVE.canAdventure(), is(false));
      assertThat(MAELSTROM.canAdventure(), is(false));
      assertThat(GLACIER.canAdventure(), is(false));
    }

    @Test
    public void canAdventureWithEffectActive() {
      var cleanups = new Cleanups(withEffect(EffectPool.DIS_ABLED));
      try (cleanups) {
        assertThat(GROVE.canAdventure(), is(true));
        assertThat(MAELSTROM.canAdventure(), is(true));
        assertThat(GLACIER.canAdventure(), is(true));
      }
    }

    @Test
    public void canAdventureWithItemInInventory() {
      var cleanups = new Cleanups(withItem(ItemPool.DEVILISH_FOLIO));
      try (cleanups) {
        assertThat(GROVE.canAdventure(), is(true));
        assertThat(MAELSTROM.canAdventure(), is(true));
        assertThat(GLACIER.canAdventure(), is(true));
      }
    }

    @Test
    public void cannotPrepareForAdventureWithoutItemAndEffect() {
      assertThat(GROVE.prepareForAdventure(), is(false));
      assertThat(MAELSTROM.prepareForAdventure(), is(false));
      assertThat(GLACIER.prepareForAdventure(), is(false));
    }

    @Test
    public void canPrepareForAdventureWithEffect() {
      setupFakeClient();

      var cleanups =
          new Cleanups(withEffect(EffectPool.DIS_ABLED), withItem(ItemPool.DEVILISH_FOLIO));
      try (cleanups) {
        assertTrue(GROVE.canAdventure());
        assertTrue(GROVE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureWithItem() {
      setupFakeClient();

      var cleanups = new Cleanups(withItem(ItemPool.DEVILISH_FOLIO));
      try (cleanups) {
        assertTrue(GROVE.canAdventure());
        assertTrue(GROVE.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.DEVILISH_FOLIO + "&ajax=1");
      }
    }
  }

  @Nested
  class Wormwood {
    private static final KoLAdventure PLEASURE_DOME =
        AdventureDatabase.getAdventureByName("The Stately Pleasure Dome");
    private static final KoLAdventure MOULDERING_MANSION =
        AdventureDatabase.getAdventureByName("The Mouldering Mansion");
    private static final KoLAdventure ROGUE_WINDMILL =
        AdventureDatabase.getAdventureByName("The Rogue Windmill");

    @Test
    public void cannotAdventureWithoutEffectOrItem() {
      assertThat(PLEASURE_DOME.canAdventure(), is(false));
      assertThat(MOULDERING_MANSION.canAdventure(), is(false));
      assertThat(ROGUE_WINDMILL.canAdventure(), is(false));
    }

    @Test
    public void canAdventureWithEffectActive() {
      var cleanups = new Cleanups(withEffect(EffectPool.ABSINTHE));
      try (cleanups) {
        assertThat(PLEASURE_DOME.canAdventure(), is(true));
        assertThat(MOULDERING_MANSION.canAdventure(), is(true));
        assertThat(ROGUE_WINDMILL.canAdventure(), is(true));
      }
    }

    @Test
    public void canAdventureWithItemInInventory() {
      var cleanups = new Cleanups(withItem(ItemPool.ABSINTHE));
      try (cleanups) {
        assertThat(PLEASURE_DOME.canAdventure(), is(true));
        assertThat(MOULDERING_MANSION.canAdventure(), is(true));
        assertThat(ROGUE_WINDMILL.canAdventure(), is(true));
      }
    }

    @Test
    public void cannotPrepareForAdventureWithoutItemAndEffect() {
      assertThat(PLEASURE_DOME.prepareForAdventure(), is(false));
      assertThat(MOULDERING_MANSION.prepareForAdventure(), is(false));
      assertThat(ROGUE_WINDMILL.prepareForAdventure(), is(false));
    }

    @Test
    public void canPrepareForAdventureWithEffect() {
      setupFakeClient();

      var cleanups = new Cleanups(withEffect(EffectPool.ABSINTHE), withItem(ItemPool.ABSINTHE));
      try (cleanups) {
        assertTrue(PLEASURE_DOME.canAdventure());
        assertTrue(PLEASURE_DOME.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureWithItem() {
      setupFakeClient();

      var cleanups = new Cleanups(withItem(ItemPool.ABSINTHE));
      try (cleanups) {
        assertTrue(PLEASURE_DOME.canAdventure());
        assertTrue(PLEASURE_DOME.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.ABSINTHE + "&ajax=1");
      }
    }
  }

  @Nested
  class Spaaace {
    private static final KoLAdventure RONALDUS =
        AdventureDatabase.getAdventureByName("Domed City of Ronaldus");
    private static final KoLAdventure GRIMACIA =
        AdventureDatabase.getAdventureByName("Domed City of Grimacia");
    private static final KoLAdventure HAMBURGLARIS =
        AdventureDatabase.getAdventureByName("Hamburglaris Shield Generator");

    @Test
    public void cannotAdventureWithoutEffectOrItem() {
      assertThat(RONALDUS.canAdventure(), is(false));
      assertThat(GRIMACIA.canAdventure(), is(false));
      assertThat(HAMBURGLARIS.canAdventure(), is(false));
    }

    @Test
    public void canAdventureWithEffectActive() {
      var cleanups = new Cleanups(withEffect(EffectPool.TRANSPONDENT));
      try (cleanups) {
        assertThat(RONALDUS.canAdventure(), is(true));
        assertThat(GRIMACIA.canAdventure(), is(true));
        assertThat(HAMBURGLARIS.canAdventure(), is(true));
      }
    }

    @Test
    public void canAdventureWithItemInInventory() {
      var cleanups = new Cleanups(withItem(ItemPool.TRANSPORTER_TRANSPONDER));
      try (cleanups) {
        assertThat(RONALDUS.canAdventure(), is(true));
        assertThat(GRIMACIA.canAdventure(), is(true));
        assertThat(HAMBURGLARIS.canAdventure(), is(true));
      }
    }

    @Test
    public void cannotPrepareForAdventureWithoutItemAndEffect() {
      assertThat(RONALDUS.prepareForAdventure(), is(false));
      assertThat(GRIMACIA.prepareForAdventure(), is(false));
      assertThat(HAMBURGLARIS.prepareForAdventure(), is(false));
    }

    @Test
    public void canPrepareForAdventureWithEffect() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withEffect(EffectPool.TRANSPONDENT), withItem(ItemPool.TRANSPORTER_TRANSPONDER));
      try (cleanups) {
        assertTrue(RONALDUS.canAdventure());
        assertTrue(RONALDUS.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureWithItem() {
      setupFakeClient();

      var cleanups = new Cleanups(withItem(ItemPool.TRANSPORTER_TRANSPONDER));
      try (cleanups) {
        assertTrue(RONALDUS.canAdventure());
        assertTrue(RONALDUS.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.TRANSPORTER_TRANSPONDER + "&ajax=1");
      }
    }
  }

  @Nested
  class DeepMachineTunnels {
    private static final KoLAdventure DEEP_MACHINE_TUNNELS =
        AdventureDatabase.getAdventureByName("The Deep Machine Tunnels");

    @Test
    public void cannotAdventureWithoutFamiliarOrEffectOrItem() {
      assertThat(DEEP_MACHINE_TUNNELS.canAdventure(), is(false));
    }

    @Test
    public void canAdventureWithFamiliarInTerrarium() {
      var cleanups = new Cleanups(withFamiliarInTerrarium(FamiliarPool.MACHINE_ELF));
      try (cleanups) {
        assertThat(DEEP_MACHINE_TUNNELS.canAdventure(), is(true));
      }
    }

    @Test
    public void canAdventureWithFamiliarAtSide() {
      var cleanups = new Cleanups(withFamiliar(FamiliarPool.MACHINE_ELF));
      try (cleanups) {
        assertThat(DEEP_MACHINE_TUNNELS.canAdventure(), is(true));
      }
    }

    @Test
    public void canAdventureWithEffectActive() {
      var cleanups = new Cleanups(withEffect(EffectPool.INSIDE_THE_SNOWGLOBE));
      try (cleanups) {
        assertThat(DEEP_MACHINE_TUNNELS.canAdventure(), is(true));
      }
    }

    @Test
    public void canAdventureWithItemInInventory() {
      var cleanups = new Cleanups(withItem(ItemPool.MACHINE_SNOWGLOBE));
      try (cleanups) {
        assertThat(DEEP_MACHINE_TUNNELS.canAdventure(), is(true));
      }
    }

    @Test
    public void cannotPrepareForAdventureWithoutFamiliarOrItemOrEffect() {
      assertThat(DEEP_MACHINE_TUNNELS.canAdventure(), is(false));
    }

    @Test
    public void canPrepareForAdventureWithFamiliarAtSide() {
      setupFakeClient();
      var cleanups = new Cleanups(withFamiliar(FamiliarPool.MACHINE_ELF));
      try (cleanups) {
        assertTrue(DEEP_MACHINE_TUNNELS.canAdventure());
        assertTrue(DEEP_MACHINE_TUNNELS.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureWithEffect() {
      setupFakeClient();
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.INSIDE_THE_SNOWGLOBE), withItem(ItemPool.MACHINE_SNOWGLOBE));
      try (cleanups) {
        assertTrue(DEEP_MACHINE_TUNNELS.canAdventure());
        assertTrue(DEEP_MACHINE_TUNNELS.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canPrepareForAdventureWithFamiliarInTerrarium() {
      setupFakeClient();
      var cleanups = new Cleanups(withFamiliarInTerrarium(FamiliarPool.MACHINE_ELF));
      try (cleanups) {
        assertTrue(DEEP_MACHINE_TUNNELS.canAdventure());
        assertTrue(DEEP_MACHINE_TUNNELS.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/familiar.php",
            "action=newfam&newfam=" + FamiliarPool.MACHINE_ELF + "&ajax=1");
      }
    }

    @Test
    public void canPrepareForAdventureWithItem() {
      setupFakeClient();
      var cleanups = new Cleanups(withItem(ItemPool.MACHINE_SNOWGLOBE));
      try (cleanups) {
        assertTrue(DEEP_MACHINE_TUNNELS.canAdventure());
        assertTrue(DEEP_MACHINE_TUNNELS.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.MACHINE_SNOWGLOBE + "&ajax=1");
      }
    }
  }

  @Nested
  class Spacegate {
    private static final KoLAdventure SPACEGATE =
        AdventureDatabase.getAdventureByName("Through the Spacegate");

    @Test
    void cannotAdventureWithoutAccess() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", false), withProperty("_spacegateToday", false));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }

    @Test
    void canAdventureWithCoordinatesAndTurns() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "ABCDEFG"),
              withProperty("_spacegateTurnsLeft", 2));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(true));
      }
    }

    @Test
    void cannotAdventureWithoutCoordinates() {
      var cleanups = new Cleanups(withProperty("spacegateAlways", true));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWithoutTurns() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "ABCDEF"),
              withProperty("_spacegateTurnsLeft", 0));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }

    @Test
    void canAdventureWithPortableSpacegateAndTurns() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", false),
              withProperty("_spacegateToday", true),
              withProperty("_spacegateTurnsLeft", 2));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(true));
      }
    }

    @Test
    void cannotAdventureWithPortableSpacegateWithoutTurns() {
      var cleanups =
          new Cleanups(
              withProperty("_spacegateToday", true), withProperty("_spacegateTurnsLeft", 0));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureInKoE() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "ABCDEF"),
              withProperty("_spacegateTurnsLeft", 2),
              withPath(Path.KINGDOM_OF_EXPLOATHING));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(false));
      }
    }

    @Test
    void canPrepareForAdventureWithEquipmentEquipped() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "ABCDEFG"),
              withProperty("_spacegateTurnsLeft", 2),
              withProperty("_spacegateGear", "exo-servo leg braces"),
              withEquipped(Slot.PANTS, ItemPool.EXO_SERVO_LEG_BRACES));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(true));
        assertThat(SPACEGATE.prepareForAdventure(), is(true));
        var requests = client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void canPrepareForAdventureWithEquipmentInInventory() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "ABCDEFG"),
              withProperty("_spacegateTurnsLeft", 2),
              withProperty("_spacegateGear", "exo-servo leg braces"),
              withEquippableItem(ItemPool.EXO_SERVO_LEG_BRACES));

      try (cleanups) {
        assertThat(SPACEGATE.canAdventure(), is(true));
        assertThat(SPACEGATE.prepareForAdventure(), is(true));
        var requests = client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&action=equip&whichitem=" + ItemPool.EXO_SERVO_LEG_BRACES);
      }
    }

    @Test
    void canPrepareForAdventureAndAcquireEquipment() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("spacegateAlways", true),
              withProperty("_spacegateCoordinates", "ABCDEFG"),
              withProperty("_spacegateTurnsLeft", 2),
              withProperty("_spacegateGear", "exo-servo leg braces"),
              withLastLocation(SPACEGATE));

      try (cleanups) {
        client.addResponse(200, html("request/test_spacegate_hazards_2.html"));
        assertThat(SPACEGATE.canAdventure(), is(true));
        assertThat(SPACEGATE.prepareForAdventure(), is(true));
        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=494");
        assertPostRequest(
            requests.get(1),
            "/inv_equip.php",
            "which=2&ajax=1&action=equip&whichitem=" + ItemPool.EXO_SERVO_LEG_BRACES);
      }
    }

    @Test
    void canPrepareForAdventureAndFindAndAcquireEquipment() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("_spacegateToday", true),
              withProperty("_spacegateTurnsLeft", 20),
              withProperty("_spacegateGear"),
              withLastLocation(SPACEGATE));

      try (cleanups) {
        client.addResponse(200, html("request/test_spacegate_hazards_1.html"));
        client.addResponse(200, ""); // api.php
        assertThat(SPACEGATE.canAdventure(), is(true));
        assertThat(SPACEGATE.prepareForAdventure(), is(true));
        var requests = client.getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=494");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(
            requests.get(2),
            "/inv_equip.php",
            "which=2&ajax=1&action=equip&whichitem=" + ItemPool.RAD_CLOAK);
      }
    }
  }

  @Nested
  class Orchard {
    @SuppressWarnings("unused")
    private enum Chambers {
      HATCHING("The Hatching Chamber", -1, -1),
      FEEDING(
          "The Feeding Chamber",
          EffectPool.FILTHWORM_LARVA_STENCH,
          ItemPool.FILTHWORM_HATCHLING_GLAND),
      GUARDS(
          "The Royal Guard Chamber",
          EffectPool.FILTHWORM_DRONE_STENCH,
          ItemPool.FILTHWORM_DRONE_GLAND),
      QUEENS(
          "The Filthworm Queen's Chamber",
          EffectPool.FILTHWORM_GUARD_STENCH,
          ItemPool.FILTHWORM_GUARD_GLAND);

      private final KoLAdventure adventure;
      private final int effectId;
      private final int itemId;

      Chambers(final String adventureName, final int effectId, final int itemId) {
        this.adventure = AdventureDatabase.getAdventureByName(adventureName);
        this.effectId = effectId;
        this.itemId = itemId;
      }

      public boolean canAdventure() {
        return adventure.canAdventure();
      }

      public boolean prepareForAdventure() {
        return adventure.prepareForAdventure();
      }

      public int getEffectId() {
        return effectId;
      }

      public int getItemId() {
        return itemId;
      }
    }

    @Test
    void cannotAdventureOutsideWar() {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "unstarted"));

      try (cleanups) {
        assertThat(Chambers.HATCHING.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWhenQueenIsSlain() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withItem(ItemPool.FILTHWORM_QUEEN_HEART));

      try (cleanups) {
        assertThat(Chambers.HATCHING.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWhenQueenHeartIsHandedIn() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.ISLAND_WAR, "step1"),
              withProperty("sidequestOrchardCompleted", "fratboy"));

      try (cleanups) {
        assertThat(Chambers.HATCHING.canAdventure(), is(false));
      }
    }

    @CartesianTest
    void canAdventureInChambersWithRightEffect(
        @CartesianTest.Enum Chambers chamber,
        @Values(booleans = {true, false}) final boolean haveEffect) {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "step1"));

      if (haveEffect) cleanups.add(withEffect(chamber.getEffectId()));

      try (cleanups) {
        assertThat(chamber.canAdventure(), is(haveEffect || chamber == Chambers.HATCHING));
      }
    }

    @CartesianTest
    void canAdventureInChambersWithRightGland(
        @CartesianTest.Enum Chambers chamber,
        @Values(booleans = {true, false}) final boolean haveGland) {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "step1"));

      if (haveGland) cleanups.add(withItem(chamber.getItemId()));

      try (cleanups) {
        assertThat(chamber.canAdventure(), is(haveGland || chamber == Chambers.HATCHING));
      }
    }

    private static final KoLAdventure UNKNOWN =
        new KoLAdventure("Orchard", "adventure.php", "696969", "The Chamber of Filthworm Commerce");

    @Test
    void cannotAdventureInUnknownNewOrchardZone() {
      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "step1"));

      try (cleanups) {
        assertThat(UNKNOWN.canAdventure(), is(false));
      }
    }

    @CartesianTest
    void preparingForChamberUsesGland(
        @CartesianTest.Enum Chambers chamber,
        @Values(booleans = {true, false}) final boolean haveGland,
        @Values(booleans = {true, false}) final boolean haveEffect) {
      setupFakeClient();

      var cleanups = new Cleanups(withQuestProgress(Quest.ISLAND_WAR, "step1"));

      if (haveGland) cleanups.add(withItem(chamber.getItemId()));
      if (haveEffect) cleanups.add(withEffect(chamber.getEffectId()));

      try (cleanups) {
        var success = chamber.prepareForAdventure();
        var requests = getRequests();

        var nothingToDo = chamber == Chambers.HATCHING || haveEffect;

        if (nothingToDo || !haveGland) {
          assertThat(requests, hasSize(0));
        } else {
          assertThat(requests, hasSize(1));
          assertPostRequest(
              requests.get(0), "/inv_use.php", "whichitem=" + chamber.getItemId() + "&ajax=1");
        }

        assertThat(success, is(nothingToDo || haveGland));
      }
    }
  }

  @Nested
  class TheSea {

    private static final KoLAdventure DEEPS =
        AdventureDatabase.getAdventureByName("The Briny Deeps");
    private static final KoLAdventure DEEPERS =
        AdventureDatabase.getAdventureByName("The Brinier Deepers");
    private static final KoLAdventure DEEPESTS =
        AdventureDatabase.getAdventureByName("The Briniest Deepests");

    @Test
    public void noAccessWithoutOldGuy() {
      var cleanups = new Cleanups(withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED));
      try (cleanups) {
        assertFalse(DEEPS.canAdventure());
        assertFalse(DEEPERS.canAdventure());
        assertFalse(DEEPESTS.canAdventure());
      }
    }

    @Test
    public void accessWithOldGuy() {
      var cleanups = new Cleanups(withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(DEEPS.canAdventure());
        assertTrue(DEEPERS.canAdventure());
        assertTrue(DEEPESTS.canAdventure());
      }
    }

    @Test
    public void mustBreathUnderwater() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED), withContinuationState());
      try (cleanups) {
        assertTrue(DEEPS.canAdventure());
        assertFalse(DEEPS.prepareForAdventure());
        assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
        assertEquals("You can't breathe underwater.", KoLmafia.lastMessage);
      }
    }

    @Test
    public void familiarMustBreathUnderwater() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
              withContinuationState(),
              withEquipped(Slot.CONTAINER, ItemPool.OLD_SCUBA_TANK),
              withFamiliar(FamiliarPool.PARROT));
      try (cleanups) {
        assertTrue(DEEPS.canAdventure());
        assertFalse(DEEPS.prepareForAdventure());
        assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
        assertEquals("Your familiar can't breathe underwater.", KoLmafia.lastMessage);
      }
    }

    @Nested
    class TheSeaFloor {
      private static final KoLAdventure GARDEN =
          AdventureDatabase.getAdventureByName("An Octopus's Garden");
      private static final KoLAdventure WRECK =
          AdventureDatabase.getAdventureByName("The Wreck of the Edgar Fitzsimmons");
      private static final KoLAdventure TRENCH =
          AdventureDatabase.getAdventureByName("The Marinara Trench");
      private static final KoLAdventure MINE = AdventureDatabase.getAdventureByName("Anemone Mine");
      private static final KoLAdventure BAR = AdventureDatabase.getAdventureByName("The Dive Bar");
      private static final KoLAdventure OUTPOST =
          AdventureDatabase.getAdventureByName("The Mer-Kin Outpost");
      private static final KoLAdventure CORRAL =
          AdventureDatabase.getAdventureByName("The Coral Corral");
      private static final KoLAdventure ABYSS =
          AdventureDatabase.getAdventureByName("The Caliginous Abyss");
      private static final KoLAdventure PARK =
          AdventureDatabase.getAdventureByName("The Skate Park");
      private static final KoLAdventure REEF = AdventureDatabase.getAdventureByName("Madness Reef");

      private static final AdventureResult BLACK_GLASS = ItemPool.get(ItemPool.BLACK_GLASS);

      @Nested
      class PreValidation {
        private Cleanups withSeaFloorProperties() {
          return new Cleanups(
              withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
              withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED),
              withProperty("mapToAnemoneMinePurchased", false),
              withProperty("mapToMadnessReefPurchased", false),
              withProperty("mapToTheDiveBarPurchased", false),
              withProperty("mapToTheMarinaraTrenchPurchased", false),
              withProperty("mapToTheSkateParkPurchased", false),
              withProperty("corralUnlocked", false));
        }

        @Test
        public void noAccessWithoutOldGuy() {
          var builder = new FakeHttpClientBuilder();
          var client = builder.client;
          var cleanups =
              new Cleanups(
                  withHttpClientBuilder(builder),
                  withSeaFloorProperties(),
                  withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED));
          try (cleanups) {
            assertFalse(GARDEN.preValidateAdventure());
            assertFalse(WRECK.preValidateAdventure());
            assertFalse(TRENCH.preValidateAdventure());
            assertFalse(MINE.preValidateAdventure());
            assertFalse(BAR.preValidateAdventure());
            assertFalse(OUTPOST.preValidateAdventure());
            assertFalse(CORRAL.preValidateAdventure());
            assertFalse(ABYSS.preValidateAdventure());
            assertFalse(PARK.preValidateAdventure());
            assertFalse(REEF.preValidateAdventure());

            var requests = client.getRequests();
            assertThat(requests, hasSize(0));
          }
        }

        @Test
        public void alwaysOpenAreasNeedNoMapVisit() {
          var builder = new FakeHttpClientBuilder();
          var client = builder.client;
          var cleanups = new Cleanups(withHttpClientBuilder(builder), withSeaFloorProperties());
          try (cleanups) {
            assertTrue(GARDEN.preValidateAdventure());

            var requests = client.getRequests();
            assertThat(requests, hasSize(0));
          }
        }

        @Test
        public void abyssOpenWithBlackGlass() {
          var builder = new FakeHttpClientBuilder();
          var client = builder.client;
          var cleanups =
              new Cleanups(
                  withHttpClientBuilder(builder), withSeaFloorProperties(), withItem(BLACK_GLASS));
          try (cleanups) {
            assertTrue(ABYSS.preValidateAdventure());

            var requests = client.getRequests();
            assertThat(requests, hasSize(0));
          }
        }

        @Test
        public void abyssNotOpenWithoutBlackGlass() {
          var builder = new FakeHttpClientBuilder();
          var client = builder.client;
          var cleanups = new Cleanups(withHttpClientBuilder(builder), withSeaFloorProperties());
          try (cleanups) {
            assertFalse(ABYSS.preValidateAdventure());

            var requests = client.getRequests();
            assertThat(requests, hasSize(0));
          }
        }

        @Test
        public void checkAccessByVisitingMap() {
          var builder = new FakeHttpClientBuilder();
          var client = builder.client;
          var cleanups = new Cleanups(withHttpClientBuilder(builder), withSeaFloorProperties());
          try (cleanups) {
            client.addResponse(200, html("request/test_visit_sea_floor.html"));

            assertTrue(WRECK.preValidateAdventure());
            assertTrue(TRENCH.preValidateAdventure());
            assertTrue(MINE.preValidateAdventure());
            assertTrue(BAR.preValidateAdventure());
            assertTrue(OUTPOST.preValidateAdventure());
            assertTrue(CORRAL.preValidateAdventure());
            assertTrue(PARK.preValidateAdventure());
            assertTrue(REEF.preValidateAdventure());

            var requests = client.getRequests();
            assertThat(requests, hasSize(1));
            assertGetRequest(requests.get(0), "/seafloor.php", null);
          }
        }
      }

      @Test
      public void noAccessWithoutOldGuy() {
        var cleanups = new Cleanups(withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED));
        try (cleanups) {
          assertFalse(GARDEN.canAdventure());
          assertFalse(WRECK.canAdventure());
          assertFalse(TRENCH.canAdventure());
          assertFalse(MINE.canAdventure());
          assertFalse(BAR.canAdventure());
          assertFalse(OUTPOST.canAdventure());
          assertFalse(CORRAL.canAdventure());
          assertFalse(ABYSS.canAdventure());
          assertFalse(PARK.canAdventure());
          assertFalse(REEF.canAdventure());
        }
      }

      @Test
      public void gardenIsAlwaysAvailable() {
        var cleanups = new Cleanups(withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED));
        try (cleanups) {
          assertTrue(GARDEN.canAdventure());
        }
      }

      @Test
      public void someZonesRequireMaps() {
        var cleanups = new Cleanups(withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED));
        try (cleanups) {
          assertFalse(TRENCH.canAdventure());
          assertFalse(MINE.canAdventure());
          assertFalse(BAR.canAdventure());
          assertFalse(PARK.canAdventure());
          assertFalse(REEF.canAdventure());
        }
      }

      @Test
      public void anemoneMineHasMap() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("mapToAnemoneMinePurchased", true));
        try (cleanups) {
          assertTrue(MINE.canAdventure());
        }
      }

      @Test
      public void marinaraTrenchHasMap() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("mapToTheMarinaraTrenchPurchased", true));
        try (cleanups) {
          assertTrue(TRENCH.canAdventure());
        }
      }

      @Test
      public void divebarHasMap() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("mapToTheDiveBarPurchased", true));
        try (cleanups) {
          assertTrue(BAR.canAdventure());
        }
      }

      @Test
      public void skateParkHasMap() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("mapToTheSkateParkPurchased", true));
        try (cleanups) {
          assertTrue(PARK.canAdventure());
        }
      }

      @Test
      public void madnessReefHasMap() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("mapToMadnessReefPurchased", true));
        try (cleanups) {
          assertTrue(REEF.canAdventure());
        }
      }

      @Test
      public void someZonesRequireQuestProgress() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.UNSTARTED));
        try (cleanups) {
          assertFalse(WRECK.canAdventure());
          assertFalse(OUTPOST.canAdventure());
        }
      }

      @Test
      public void wreckRequiresQuest() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withQuestProgress(Quest.SEA_MONKEES, "step1"));
        try (cleanups) {
          assertTrue(WRECK.canAdventure());
        }
      }

      @Test
      public void outpostRequiresQuest() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withQuestProgress(Quest.SEA_MONKEES, "step6"));
        try (cleanups) {
          assertTrue(OUTPOST.canAdventure());
        }
      }

      @Test
      public void corralRequiresUnlock() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("corralUnlocked", false));
        try (cleanups) {
          assertFalse(CORRAL.canAdventure());
        }
      }

      @Test
      public void corralCanBeUnlocked() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("corralUnlocked", true));
        try (cleanups) {
          assertTrue(CORRAL.canAdventure());
        }
      }

      @Test
      public void abyssRequiresBlackGlass() {
        var cleanups = new Cleanups(withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED));
        try (cleanups) {
          assertFalse(ABYSS.canAdventure());
        }
      }

      @Test
      public void abbyssAvailableWithBlackGlass() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withContinuationState(),
                withEquipped(Slot.CONTAINER, ItemPool.OLD_SCUBA_TANK),
                withItem(BLACK_GLASS));
        try (cleanups) {
          assertTrue(ABYSS.canAdventure());
          assertFalse(ABYSS.prepareForAdventure());
          assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
          assertEquals("Equip your black glass in order to go there.", KoLmafia.lastMessage);
        }
      }

      @Test
      public void abbyssRequiresEquippedBlackGlass() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withContinuationState(),
                withEquipped(Slot.CONTAINER, ItemPool.OLD_SCUBA_TANK),
                withEquipped(Slot.ACCESSORY1, BLACK_GLASS));
        try (cleanups) {
          assertTrue(ABYSS.canAdventure());
          assertTrue(ABYSS.prepareForAdventure());
          assertEquals(MafiaState.CONTINUE, StaticEntity.getContinuationState());
        }
      }
    }

    @Nested
    class MerkinDeepcity {
      private static final KoLAdventure SCHOOL =
          AdventureDatabase.getAdventureByName("Mer-kin Elementary School");
      private static final KoLAdventure LIBRARY =
          AdventureDatabase.getAdventureByName("Mer-kin Library");
      private static final KoLAdventure GYMNASIUM =
          AdventureDatabase.getAdventureByName("Mer-kin Gymnasium");
      private static final KoLAdventure COLOSSEUM =
          AdventureDatabase.getAdventureByName("Mer-kin Colosseum");
      private static final KoLAdventure TEMPLE =
          AdventureDatabase.getAdventureByName("Mer-kin Temple");

      @Test
      public void noAccessWithoutOldGuy() {
        var cleanups = new Cleanups(withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.UNSTARTED));
        try (cleanups) {
          assertFalse(SCHOOL.canAdventure());
          assertFalse(LIBRARY.canAdventure());
          assertFalse(GYMNASIUM.canAdventure());
          assertFalse(COLOSSEUM.canAdventure());
          assertFalse(TEMPLE.canAdventure());
        }
      }

      @Test
      public void noAccessWithoutSeahorse() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("seahorseName", ""));
        try (cleanups) {
          assertFalse(SCHOOL.canAdventure());
          assertFalse(LIBRARY.canAdventure());
          assertFalse(GYMNASIUM.canAdventure());
          assertFalse(COLOSSEUM.canAdventure());
          assertFalse(TEMPLE.canAdventure());
        }
      }

      @Test
      public void accessWithSeahorse() {
        var cleanups =
            new Cleanups(
                withQuestProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED),
                withProperty("seahorseName", "Shimmerwings"));
        try (cleanups) {
          assertTrue(SCHOOL.canAdventure());
          assertTrue(LIBRARY.canAdventure());
          assertTrue(GYMNASIUM.canAdventure());
          assertTrue(COLOSSEUM.canAdventure());
          assertTrue(TEMPLE.canAdventure());
        }
      }
    }
  }

  @Nested
  class Holiday {
    private static final KoLAdventure SSPD =
        AdventureDatabase.getAdventure(AdventurePool.SSPD_STUPOR);

    private static final KoLAdventure YULETIDE =
        AdventureDatabase.getAdventure(AdventurePool.YULETIDE);

    @Test
    void mustBeSspdForStupor() {
      var cleanups = new Cleanups(withDay(2023, Month.FEBRUARY, 9), withInebriety(200));
      try (cleanups) {
        assertThat(SSPD.canAdventure(), is(false));
      }
    }

    @Test
    void mustBeDrunkForStupor() {
      var cleanups = new Cleanups(withDay(2023, Month.FEBRUARY, 10), withInebriety(1));
      try (cleanups) {
        assertThat(SSPD.canAdventure(), is(false));
      }
    }

    @Test
    void canAdventureInSspdStupor() {
      var cleanups = new Cleanups(withDay(2023, Month.FEBRUARY, 10), withInebriety(200));
      try (cleanups) {
        assertThat(SSPD.canAdventure(), is(true));
      }
    }

    @Test
    void canAdventureInSspdStuporOnCombinationSspdEvent() {
      var cleanups = new Cleanups(withDay(2011, Month.MARCH, 17), withInebriety(200));
      try (cleanups) {
        assertThat(SSPD.canAdventure(), is(true));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void handlesSimpleHolidayZones(final boolean isYuletide) {
      var cleanups = new Cleanups(withDay(2023, Month.JANUARY, isYuletide ? 18 : 20));
      try (cleanups) {
        assertThat(YULETIDE.canAdventure(), is(isYuletide));
      }
    }
  }

  @Nested
  class FantasyRealm {
    private static final KoLAdventure BANDITS =
        AdventureDatabase.getAdventureByName("The Bandit Crossroads");
    private static final KoLAdventure MOUNTAINS =
        AdventureDatabase.getAdventureByName("The Towering Mountains");

    @Test
    void canAdventure() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.canAdventure(), is(true));
      }
    }

    @Test
    void canAdventureWithDayPass() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", false),
              withProperty("_frToday", true),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.canAdventure(), is(true));
      }
    }

    @Test
    void cannotAdventureWithoutAccess() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", false),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWithoutHours() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 0),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.canAdventure(), is(false));
      }
    }

    @Test
    void cannotAdventureWithoutUnlock() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(MOUNTAINS.canAdventure(), is(false));
      }
    }

    @Test
    void cannotPrepareWithoutGem() {
      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"));

      try (cleanups) {
        assertThat(BANDITS.prepareForAdventure(), is(false));
      }
    }

    @Test
    void preparingEquipsGem() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"),
              withItem(ItemPool.FANTASY_REALM_GEM));

      try (cleanups) {
        var success = BANDITS.prepareForAdventure();

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&slot=1&action=equip&whichitem=" + ItemPool.FANTASY_REALM_GEM);
        assertThat(success, is(true));
      }
    }

    @Test
    void preparingRemovesFamiliar() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withProperty("frAlways", true),
              withProperty("_frToday", false),
              withProperty("_frHoursLeft", 5),
              withProperty("_frAreasUnlocked", "The Bandit Crossroads,"),
              withEquipped(Slot.ACCESSORY1, ItemPool.FANTASY_REALM_GEM),
              withFamiliar(FamiliarPool.PARROT));

      try (cleanups) {
        var success = BANDITS.prepareForAdventure();

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/familiar.php", "action=putback&ajax=1");
        assertThat(success, is(true));
      }
    }
  }

  @Nested
  class TheDrip {
    private static final KoLAdventure DRIPPING_TREES =
        AdventureDatabase.getAdventureByName("The Dripping Trees");
    private static final KoLAdventure DRIPPING_HALL =
        AdventureDatabase.getAdventureByName("The Dripping Hall");

    @Test
    void cannotAdventureWithoutDripHarness() {
      assertFalse(DRIPPING_TREES.canAdventure());
      assertFalse(DRIPPING_HALL.canAdventure());
    }

    @Test
    void cannotAdventureUnlessDrippingHallUnlocked() {
      var cleanups = new Cleanups(withEquipped(Slot.CONTAINER, ItemPool.DRIP_HARNESS));
      try (cleanups) {
        assertTrue(DRIPPING_TREES.canAdventure());
        assertFalse(DRIPPING_HALL.canAdventure());
      }
    }

    @Test
    void canAdventureWithnlessDrippingHallUnlocked() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CONTAINER, ItemPool.DRIP_HARNESS),
              withProperty("drippingHallUnlocked", true));
      try (cleanups) {
        assertTrue(DRIPPING_TREES.canAdventure());
        assertTrue(DRIPPING_HALL.canAdventure());
      }
    }

    @Test
    void canPrepareForAdventureWithDripHarnessEquipped() {
      setupFakeClient();

      var cleanups = new Cleanups(withEquipped(Slot.CONTAINER, ItemPool.DRIP_HARNESS));
      try (cleanups) {
        assertTrue(DRIPPING_TREES.canAdventure());
        assertTrue(DRIPPING_TREES.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void canPrepareForAdventureWithDripHarnessUnequipped() {
      setupFakeClient();

      var cleanups = new Cleanups(withEquippableItem(ItemPool.DRIP_HARNESS));
      try (cleanups) {
        assertTrue(DRIPPING_TREES.canAdventure());
        assertTrue(DRIPPING_TREES.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&action=equip&whichitem=" + ItemPool.DRIP_HARNESS);
      }
    }
  }

  private static final KoLAdventure SPOOKY_FOREST =
      AdventureDatabase.getAdventureByName("The Spooky Forest");

  @Test
  void canVisitSpookyForestIfCitadelQuestIsStarted() {
    var cleanups =
        new Cleanups(
            withQuestProgress(Quest.CITADEL, QuestDatabase.STARTED),
            withQuestProgress(Quest.LARVA, QuestDatabase.UNSTARTED));
    try (cleanups) {
      assertTrue(SPOOKY_FOREST.canAdventure());
    }
  }

  @Nested
  class MarketQuests {
    private static final KoLAdventure SKELETON_STORE =
        AdventureDatabase.getAdventureByName("The Skeleton Store");
    private static final KoLAdventure MADNESS_BAKERY =
        AdventureDatabase.getAdventureByName("Madness Bakery");
    private static final KoLAdventure OVERGROWN_LOT =
        AdventureDatabase.getAdventureByName("The Overgrown Lot");

    // For each zone:
    //   If have access, prepareForAdventure immediately returns true
    //   If have no access and have item, prepareForAdventure uses item.
    //   If have no access and don't have item, prepareForAdventure starts quest with NPC

    @Test
    void withAccessToSkeletonStoreMakesNoRequests() {
      var cleanups = new Cleanups(withProperty("skeletonStoreAvailable", true));
      setupFakeClient();
      try (cleanups) {
        var success = SKELETON_STORE.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(0));
        assertThat(success, is(true));
      }
    }

    @Test
    void withSkeletonStoreItemUsesItem() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("skeletonStoreAvailable", false),
              withQuestProgress(Quest.MEATSMITH, QuestDatabase.UNSTARTED),
              withItem(ItemPool.BONE_WITH_A_PRICE_TAG));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_bone_with_a_tag.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(SKELETON_STORE.canAdventure());
        assertTrue(SKELETON_STORE.prepareForAdventure());
        assertThat(Quest.MEATSMITH, isUnstarted());
        assertTrue(Preferences.getBoolean("skeletonStoreAvailable"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.BONE_WITH_A_PRICE_TAG + "&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void withoutSkeletonStoreItemStartsQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.MEATSMITH, QuestDatabase.UNSTARTED),
              withProperty("skeletonStoreAvailable", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_meatsmith_quest.html"));
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_visit_meatsmith_talk.html"));
        client.addResponse(200, html("request/test_visit_meatsmith_accept.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(SKELETON_STORE.canAdventure());
        assertTrue(SKELETON_STORE.prepareForAdventure());
        assertThat(Quest.MEATSMITH, isStarted());
        assertTrue(Preferences.getBoolean("skeletonStoreAvailable"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=meatsmith");
        assertPostRequest(requests.get(1), "/shop.php", "whichshop=meatsmith&action=talk");
        assertGetRequest(requests.get(2), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1059&option=1");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void withAccessToMadnessBakeryStoreMakesNoRequests() {
      var cleanups = new Cleanups(withProperty("madnessBakeryAvailable", true));
      setupFakeClient();
      try (cleanups) {
        var success = MADNESS_BAKERY.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(0));
        assertThat(success, is(true));
      }
    }

    @Test
    void withMadnessBakeryItemUsesItem() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.ARMORER, QuestDatabase.UNSTARTED),
              withProperty("madnessBakeryAvailable", false),
              withItem(ItemPool.HYPNOTIC_BREADCRUMBS));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_breadcrumbs.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(MADNESS_BAKERY.canAdventure());
        assertTrue(MADNESS_BAKERY.prepareForAdventure());
        assertThat(Quest.ARMORER, isUnstarted());
        assertTrue(Preferences.getBoolean("madnessBakeryAvailable"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0),
            "/inv_use.php",
            "whichitem=" + ItemPool.HYPNOTIC_BREADCRUMBS + "&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void withoutMadnessBakeryItemStartsQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.ARMORER, QuestDatabase.UNSTARTED),
              withProperty("madnessBakeryAvailable", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_armory_quest.html"));
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_visit_armory_talk.html"));
        client.addResponse(200, html("request/test_visit_armory_accept.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(MADNESS_BAKERY.canAdventure());
        assertTrue(MADNESS_BAKERY.prepareForAdventure());
        assertThat(Quest.ARMORER, isStarted());
        assertTrue(Preferences.getBoolean("madnessBakeryAvailable"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=armory");
        assertPostRequest(requests.get(1), "/shop.php", "whichshop=armory&action=talk");
        assertGetRequest(requests.get(2), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1065&option=1");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void withAccessToOvergrownLotMakesNoRequests() {
      var cleanups = new Cleanups(withProperty("overgrownLotAvailable", true));
      setupFakeClient();
      try (cleanups) {
        var success = OVERGROWN_LOT.prepareForAdventure();
        var requests = getRequests();
        assertThat(requests, hasSize(0));
        assertThat(success, is(true));
      }
    }

    @Test
    void withOvergrownLotItemUsesItem() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.DOC, QuestDatabase.UNSTARTED),
              withProperty("overgrownLotAvailable", false),
              withItem(ItemPool.BOOZE_MAP));
      try (cleanups) {
        client.addResponse(200, html("request/test_use_booze_cache_map.html"));
        client.addResponse(200, ""); // api.php
        assertTrue(OVERGROWN_LOT.canAdventure());
        assertTrue(OVERGROWN_LOT.prepareForAdventure());
        assertThat(Quest.DOC, isUnstarted());
        assertTrue(Preferences.getBoolean("overgrownLotAvailable"));

        var requests = client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(
            requests.get(0), "/inv_use.php", "whichitem=" + ItemPool.BOOZE_MAP + "&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    void withoutOvergrownLotItemStartsQuest() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withQuestProgress(Quest.DOC, QuestDatabase.UNSTARTED),
              withProperty("overgrownLotAvailable", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_visit_galaktik_quest.html"));
        client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        client.addResponse(200, html("request/test_visit_galaktik_talk.html"));
        client.addResponse(200, html("request/test_visit_galaktik_accept.html"));
        client.addResponse(200, ""); // api.php

        assertTrue(OVERGROWN_LOT.canAdventure());
        assertTrue(OVERGROWN_LOT.prepareForAdventure());
        assertTrue(Preferences.getBoolean("overgrownLotAvailable"));
        assertThat(Quest.DOC, isStarted());

        var requests = client.getRequests();
        assertThat(requests, hasSize(5));
        assertPostRequest(requests.get(0), "/shop.php", "whichshop=doc");
        assertPostRequest(requests.get(1), "/shop.php", "whichshop=doc&action=talk");
        assertGetRequest(requests.get(2), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=1064&option=1");
        assertPostRequest(requests.get(4), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }

  @Nested
  class Oasis {
    private static final KoLAdventure OASIS = AdventureDatabase.getAdventureByName("The Oasis");

    @Test
    void milestoneWillExploreDesertButNotOpenOasis() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.MILESTONE),
              withItem(ItemPool.BITCHIN_MEATCAR),
              withProperty("desertExploration", 0),
              withProperty("oasisAvailable", false));
      try (cleanups) {
        client.addResponse(200, html("request/test_milestone_explore_desert.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("inv_use.php?which=3&whichitem=11104&ajax=1");
        request.run();

        assertThat("desertExploration", isSetTo(5));
        assertThat("oasisAvailable", isSetTo(false));

        assertFalse(OASIS.canAdventure());
      }
    }

    @Test
    void fightInDesertWillExploreAndOpenOasis() {
      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.BITCHIN_MEATCAR),
              withEquipped(Slot.OFFHAND, ItemPool.UV_RESISTANT_COMPASS),
              withProperty("desertExploration", 10),
              withProperty("oasisAvailable", false),
              withLastLocation("The Arid, Extra-Dry Desert"),
              withFight());
      try (cleanups) {
        client.addResponse(200, html("request/test_open_oasis.html"));
        client.addResponse(200, ""); // api.php

        var request = new GenericRequest("fight.php?action=attack");
        request.run();

        assertThat("desertExploration", isSetTo(12));
        assertThat("oasisAvailable", isSetTo(true));

        assertTrue(OASIS.canAdventure());
      }
    }
  }

  @Nested
  class ShadowRift {
    private static final KoLAdventure SHADOW_RIFT =
        AdventureDatabase.getAdventureByName("Shadow Rift");
    private static final KoLAdventure DESERT_BEACH =
        AdventureDatabase.getAdventureByName("Shadow Rift (Desert Beach)");
    private static final KoLAdventure FOREST_VILLAGE =
        AdventureDatabase.getAdventureByName("Shadow Rift (Forest Village)");
    private static final KoLAdventure MCLARGEHUGE =
        AdventureDatabase.getAdventureByName("Shadow Rift (Mt. McLargeHuge)");
    private static final KoLAdventure BEANSTALK =
        AdventureDatabase.getAdventureByName("Shadow Rift (Somewhere Over the Beanstalk)");
    private static final KoLAdventure SPOOKYRAVEN =
        AdventureDatabase.getAdventureByName("Shadow Rift (Spookyraven Manor Third Floor)");
    private static final KoLAdventure PIXEL_REALM =
        AdventureDatabase.getAdventureByName("Shadow Rift (The 8-Bit Realm)");
    private static final KoLAdventure PYRAMID =
        AdventureDatabase.getAdventureByName("Shadow Rift (The Ancient Buried Pyramid)");
    private static final KoLAdventure CASTLE =
        AdventureDatabase.getAdventureByName("Shadow Rift (The Castle in the Clouds in the Sky)");
    private static final KoLAdventure DISTANT_WOODS =
        AdventureDatabase.getAdventureByName("Shadow Rift (The Distant Woods)");
    private static final KoLAdventure HIDDEN_CITY =
        AdventureDatabase.getAdventureByName("Shadow Rift (The Hidden City)");
    private static final KoLAdventure CEMETERY =
        AdventureDatabase.getAdventureByName("Shadow Rift (The Misspelled Cemetary)");
    private static final KoLAdventure NEARBY_PLAINS =
        AdventureDatabase.getAdventureByName("Shadow Rift (The Nearby Plains)");
    private static final KoLAdventure TOWN_RIGHT =
        AdventureDatabase.getAdventureByName("Shadow Rift (The Right Side of the Tracks)");

    @Test
    void cannotAdventureWithoutPayphoneOrPath() {
      assertFalse(TOWN_RIGHT.canAdventure());
    }

    @Test
    void canAdventureInASoL() {
      var cleanups = withPath(Path.SHADOWS_OVER_LOATHING);
      try (cleanups) {
        assertTrue(TOWN_RIGHT.canAdventure());
      }
    }

    @Test
    void canAdventureWithPayphoneInInventory() {
      var cleanups = withItem(ItemPool.CLOSED_CIRCUIT_PAY_PHONE);
      try (cleanups) {
        assertTrue(TOWN_RIGHT.canAdventure());
      }
    }

    @Test
    void cannotAdventureWithPayphoneInCloset() {
      var cleanups = withItemInCloset(ItemPool.CLOSED_CIRCUIT_PAY_PHONE);
      try (cleanups) {
        assertFalse(TOWN_RIGHT.canAdventure());
      }
    }

    @Test
    void onlyCertainShadowRiftsInitiallyOpen() {
      var cleanups = withPath(Path.SHADOWS_OVER_LOATHING);
      try (cleanups) {
        // Have to have visited a Shadow Rift Ingress
        assertFalse(SHADOW_RIFT.canAdventure());
        // Always open
        assertTrue(TOWN_RIGHT.canAdventure());
        assertTrue(NEARBY_PLAINS.canAdventure());
        // Woods must be open
        assertFalse(DISTANT_WOODS.canAdventure());
        assertFalse(FOREST_VILLAGE.canAdventure());
        assertFalse(PIXEL_REALM.canAdventure());
        // Lady Spookyraven Dancing quest must be done
        assertFalse(SPOOKYRAVEN.canAdventure());
        // Desert Beach must be accessible
        assertFalse(DESERT_BEACH.canAdventure());
        // Cyrpt quest or Wizard of Ego quest must be started
        assertFalse(CEMETERY.canAdventure());
        // Trapper Quest must be started
        assertFalse(MCLARGEHUGE.canAdventure());
        // Beanstalk must be planted (or plantable)
        assertFalse(BEANSTALK.canAdventure());
        // Garbage quest must be finished
        assertFalse(CASTLE.canAdventure());
        // Hidden City must be open
        assertFalse(HIDDEN_CITY.canAdventure());
        // Pyramid must be open
        assertFalse(PYRAMID.canAdventure());
      }
    }

    @Test
    void canVisitGenericRiftIfWithIngress() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withProperty("shadowRiftIngress", "mclargehuge"));
      try (cleanups) {
        // Have to have visited a Shadow Rift Ingress
        assertTrue(SHADOW_RIFT.canAdventure());
      }
    }

    @Test
    void canVisitWoodsRiftsIfLarvaQuestStarted() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED));
      try (cleanups) {
        // Have to have been given the LARVA quest
        assertTrue(DISTANT_WOODS.canAdventure());
        assertTrue(FOREST_VILLAGE.canAdventure());
        assertTrue(PIXEL_REALM.canAdventure());
      }
    }

    @Test
    void canVisitWoodsRiftsIfCitadelQuestStarted() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.CITADEL, QuestDatabase.STARTED));
      try (cleanups) {
        assertTrue(DISTANT_WOODS.canAdventure());
        assertTrue(FOREST_VILLAGE.canAdventure());
        assertTrue(PIXEL_REALM.canAdventure());
      }
    }

    @Test
    public void canVisitPixelRiftWithTransfunctionerEquipped() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED),
              withEquipped(Slot.ACCESSORY1, ItemPool.TRANSFUNCTIONER));
      try (cleanups) {
        assertTrue(PIXEL_REALM.canAdventure());
        assertTrue(PIXEL_REALM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void canVisitPixelRiftWithTransfunctionerInInventory() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED),
              withEquippableItem(ItemPool.TRANSFUNCTIONER));
      try (cleanups) {
        assertTrue(PIXEL_REALM.canAdventure());
        assertTrue(PIXEL_REALM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0),
            "/inv_equip.php",
            "which=2&ajax=1&slot=1&action=equip&whichitem=" + ItemPool.TRANSFUNCTIONER);
      }
    }

    @Test
    public void canVisitPixelRiftIfCurrentRift() {
      setupFakeClient();

      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.LARVA, QuestDatabase.STARTED),
              withProperty("shadowRiftIngress", "8bit"));
      try (cleanups) {

        assertTrue(PIXEL_REALM.canAdventure());
        assertTrue(PIXEL_REALM.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    void canVisitSpookyravenRiftIfThirdFloorUnlocked() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED));
      try (cleanups) {
        // Have to have been given the LARVA quest
        assertTrue(SPOOKYRAVEN.canAdventure());
      }
    }

    @Test
    void canVisitDesertBeachRiftIfBeachAccessible() {
      var cleanups =
          new Cleanups(withPath(Path.SHADOWS_OVER_LOATHING), withItem(ItemPool.BITCHIN_MEATCAR));
      try (cleanups) {
        // Have to be able to get to the beach
        assertTrue(DESERT_BEACH.canAdventure());
      }
    }

    @Test
    void canVisitCemeteryRiftIfCyrptQuest() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.CYRPT, QuestDatabase.STARTED));
      try (cleanups) {
        // You can get to the Misspelled Cemetery to get to the Cyrpt
        assertTrue(CEMETERY.canAdventure());
      }
    }

    @Test
    void canVisitCemeteryRiftWithWizardOfEgoQuest() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.EGO, QuestDatabase.STARTED));
      try (cleanups) {
        // You can get to the Misspelled Cemetery to search Fernswarthy's grave
        assertTrue(CEMETERY.canAdventure());
      }
    }

    @Test
    void canVisitCemeteryRiftWithNemesisQuest() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.EGO, QuestDatabase.STARTED));
      try (cleanups) {
        // You can get to the Misspelled Cemetery to get to the find your legendary weapon
        assertTrue(CEMETERY.canAdventure());
      }
    }

    @Test
    void canVisitMcLargeHugeRiftIfTrapperQuest() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.TRAPPER, QuestDatabase.STARTED));
      try (cleanups) {
        // You can get to the Mt. McLargeHuge if you can visit the Trapper
        assertTrue(MCLARGEHUGE.canAdventure());
      }
    }

    @Test
    void canVisitBeanstalkRiftIfBeanstalkPlanted() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING), withQuestProgress(Quest.GARBAGE, "step1"));
      try (cleanups) {
        // You can get above the beanstalk if you have planted it
        assertTrue(BEANSTALK.canAdventure());
      }
    }

    @Test
    void canVisitBeanstalkRiftIfBeanstalkPlantable() {
      setupFakeClient();
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.GARBAGE, QuestDatabase.STARTED),
              withItem(ItemPool.ENCHANTED_BEAN));
      try (cleanups) {
        // You can get above the beanstalk if quest started and you have a bean
        assertTrue(BEANSTALK.canAdventure());
        assertTrue(BEANSTALK.prepareForAdventure());

        var requests = getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/place.php", "whichplace=plains&action=garbage_grounds");
      }
    }

    @Test
    void canVisitCastleRiftIfAirShipIsFinished() {
      var cleanups = new Cleanups(withPath(Path.SHADOWS_OVER_LOATHING), withItem(ItemPool.SOCK));
      try (cleanups) {
        // You can get above to the Giant Castle with a S.O.C.K.
        assertTrue(CASTLE.canAdventure());
      }
    }

    @Test
    void canVisitHiddenCityRiftIfTempleDone() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING), withQuestProgress(Quest.WORSHIP, "step4"));
      try (cleanups) {
        // You can get to the Hidden City if you have finished the Hidden Temple
        assertTrue(HIDDEN_CITY.canAdventure());
      }
    }

    @Test
    void canVisitPyramidRiftIfPyramidOpen() {
      var cleanups =
          new Cleanups(
              withPath(Path.SHADOWS_OVER_LOATHING),
              withQuestProgress(Quest.PYRAMID, QuestDatabase.STARTED));
      try (cleanups) {
        // You can get to the Pyramid once you have opened it
        assertTrue(PYRAMID.canAdventure());
      }
    }
  }

  @Nested
  class Small {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void canOnlyAdventureInDirtWhenSmall(boolean inRun) {
      var cleanups = new Cleanups(withPath(inRun ? Path.SMALL : Path.NONE));

      try (cleanups) {
        var area = AdventureDatabase.getAdventureByName("Fight in the Dirt");
        assertThat(area.canAdventure(), is(inRun));
      }
    }
  }

  @Nested
  class Crimbo23 {
    @Test
    void checksTownAfterWarChange() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("crimbo23ArmoryAtWar", false),
              withProperty("crimbo23BarAtWar", false),
              withProperty("crimbo23CafeAtWar", false),
              withProperty("crimbo23CottageAtWar", false),
              withProperty("crimbo23FoundryAtWar", false));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_place_crimbo23_1.html"));

        var failure =
            KoLAdventure.findAdventureFailure(html("request/test_adventure_crimbo23_peace.html"));
        assertThat(failure, greaterThan(0));
        assertThat("crimbo23ArmoryAtWar", isSetTo(false));
        assertThat("crimbo23BarAtWar", isSetTo(true));
        assertThat("crimbo23CafeAtWar", isSetTo(false));
        assertThat("crimbo23CottageAtWar", isSetTo(true));
        assertThat("crimbo23FoundryAtWar", isSetTo(false));
      }
    }
  }
}
