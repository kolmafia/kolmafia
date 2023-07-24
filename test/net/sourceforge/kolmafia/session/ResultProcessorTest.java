package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withNoItems;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withSign;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isFinished;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Month;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ResultProcessorTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("ResultProcessorTest");
    Preferences.reset("ResultProcessorTest");
    BanishManager.clearCache();
    InventoryManager.resetInventory();
  }

  @Nested
  class OysterEggs {
    private static final AdventureResult MAGNIFICENT_OYSTER_EGG =
        ItemPool.get(ItemPool.MAGNIFICENT_OYSTER_EGG);

    @Test
    public void obtainOysterEggAppropriately() {
      HolidayDatabase.guessPhaseStep();
      EquipmentManager.setEquipment(Slot.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
      // This was an Oyster Egg Day.
      final var cleanups = withDay(2022, Month.JANUARY, 29, 12, 0);
      try (cleanups) {
        ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
      }
    }

    @Test
    public void obtainOysterEggOnWrongDay() {
      EquipmentManager.setEquipment(Slot.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
      // This was not an Oyster Egg Day.
      final var cleanups = withDay(2022, Month.JANUARY, 30, 12, 0);
      try (cleanups) {
        ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
        assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
      }
    }

    @Test
    public void obtainOysterEggWithoutBasket() {
      // This was an Oyster Egg Day.
      final var cleanups = withDay(2022, Month.JANUARY, 29, 12, 0);
      try (cleanups) {
        ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
        assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
      }
    }

    @Test
    public void obtainOysterEggOnWrongDayAndWithoutBasket() {
      // This was not an Oyster Egg Day.
      final var cleanups = withDay(2022, Month.JANUARY, 30, 12, 0);
      try (cleanups) {
        ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
        assertEquals(Preferences.getInteger("_oysterEggsFound"), 0);
      }
    }
  }

  @Nested
  class CosmicBowlingBall {
    private static final AdventureResult COSMIC_BOWLING_BALL =
        ItemPool.get(ItemPool.COSMIC_BOWLING_BALL);

    @Test
    public void gettingCosmicBowlingBallInCombatResetsBanishes() {
      Preferences.setInteger("cosmicBowlingBallReturnCombats", 20);
      BanishManager.banishMonster("zmobie", BanishManager.Banisher.BOWL_A_CURVEBALL, true);
      assertTrue(BanishManager.isBanished("zmobie"));

      ResultProcessor.processResult(true, COSMIC_BOWLING_BALL);

      assertFalse(BanishManager.isBanished("zmobie"));
    }

    @Test
    public void gettingCosmicBowlingBallInCombatResetsReturnCombats() {
      Preferences.setInteger("cosmicBowlingBallReturnCombats", 20);
      BanishManager.banishMonster("zmobie", BanishManager.Banisher.BOWL_A_CURVEBALL, true);
      ResultProcessor.processResult(true, COSMIC_BOWLING_BALL);

      assertThat("cosmicBowlingBallReturnCombats", isSetTo(-1));
    }
  }

  @Nested
  class GoblinWater {
    private static final AdventureResult GOBLIN_WATER = ItemPool.get(ItemPool.GOBLIN_WATER);

    @Test
    public void gettingGoblinWaterFromAquagoblinCompletesGoblinQuest() {
      QuestDatabase.setQuestProgress(QuestDatabase.Quest.GOBLIN, QuestDatabase.STARTED);
      MonsterData aquaGoblinMonster = MonsterDatabase.findMonster("Aquagoblin");
      MonsterStatusTracker.setNextMonster(aquaGoblinMonster);
      ResultProcessor.processResult(true, GOBLIN_WATER);
      assertTrue(
          QuestDatabase.isQuestFinished(QuestDatabase.Quest.GOBLIN),
          "Getting Goblin water from AquaGoblin shoud finish the L05 Quest");
    }

    @Test
    public void gettingGoblinWaterFromCheengSpecsDoesNotCompleteGoblinQuest() {
      QuestDatabase.setQuestProgress(QuestDatabase.Quest.GOBLIN, QuestDatabase.UNSTARTED);
      MonsterData testMonster = MonsterDatabase.findMonster("zmobie");
      MonsterStatusTracker.setNextMonster(testMonster);
      ResultProcessor.processResult(true, GOBLIN_WATER);
      assertFalse(
          QuestDatabase.isQuestFinished(QuestDatabase.Quest.GOBLIN),
          "Getting Goblin Water from anyone but Aquagoblin during heavy rains should not finish L05 Quest");
    }
  }

  @Nested
  class SuburbsOfDis {
    private static final AdventureResult[] CLUMSINESS_STONES = {
      ItemPool.get(ItemPool.FURIOUS_STONE), ItemPool.get(ItemPool.VANITY_STONE)
    };

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void gettingFirstClumsinessStone(int stone) {
      var cleanups =
          new Cleanups(
              withQuestProgress(QuestDatabase.Quest.CLUMSINESS, 1),
              withProperty("clumsinessGroveBoss", "something"));

      try (cleanups) {
        ResultProcessor.processResult(true, CLUMSINESS_STONES[stone]);

        assertThat(QuestDatabase.Quest.CLUMSINESS, isStep(2));
        assertThat("clumsinessGroveBoss", isSetTo(""));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void gettingSecondClumsinessStone(int stone) {
      var cleanups =
          new Cleanups(
              withQuestProgress(QuestDatabase.Quest.CLUMSINESS, 3),
              withProperty("clumsinessGroveBoss", "something"),
              withItem(CLUMSINESS_STONES[(stone + 1) % 2]));

      try (cleanups) {
        ResultProcessor.processResult(true, CLUMSINESS_STONES[stone]);

        assertThat(QuestDatabase.Quest.CLUMSINESS, isFinished());
        assertThat("clumsinessGroveBoss", isSetTo(""));
      }
    }

    private static final AdventureResult[] GLACIER_STONES = {
      ItemPool.get(ItemPool.AVARICE_STONE), ItemPool.get(ItemPool.GLUTTONOUS_STONE)
    };

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void gettingFirstGlacierStone(int stone) {
      var cleanups =
          new Cleanups(
              withQuestProgress(QuestDatabase.Quest.GLACIER, 1),
              withProperty("glacierOfJerksBoss", "something"));

      try (cleanups) {
        ResultProcessor.processResult(true, GLACIER_STONES[stone]);

        assertThat(QuestDatabase.Quest.GLACIER, isStep(2));
        assertThat("glacierOfJerksBoss", isSetTo(""));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void gettingSecondGlacierStone(int stone) {
      var cleanups =
          new Cleanups(
              withQuestProgress(QuestDatabase.Quest.GLACIER, 3),
              withProperty("glacierOfJerksBoss", "something"),
              withItem(GLACIER_STONES[(stone + 1) % 2]));

      try (cleanups) {
        ResultProcessor.processResult(true, GLACIER_STONES[stone]);

        assertThat(QuestDatabase.Quest.GLACIER, isFinished());
        assertThat("glacierOfJerksBoss", isSetTo(""));
      }
    }

    private static final AdventureResult[] MAELSTROM_STONES = {
      ItemPool.get(ItemPool.LECHEROUS_STONE), ItemPool.get(ItemPool.JEALOUSY_STONE)
    };

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void gettingFirstMaelstromStone(int stone) {
      var cleanups =
          new Cleanups(
              withQuestProgress(QuestDatabase.Quest.MAELSTROM, 1),
              withProperty("maelstromOfLoversBoss", "something"));

      try (cleanups) {
        ResultProcessor.processResult(true, MAELSTROM_STONES[stone]);

        assertThat(QuestDatabase.Quest.MAELSTROM, isStep(2));
        assertThat("maelstromOfLoversBoss", isSetTo(""));
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    public void gettingSecondMaelstromStone(int stone) {
      var cleanups =
          new Cleanups(
              withQuestProgress(QuestDatabase.Quest.MAELSTROM, 3),
              withProperty("maelstromOfLoversBoss", "something"),
              withItem(MAELSTROM_STONES[(stone + 1) % 2]));

      try (cleanups) {
        ResultProcessor.processResult(true, MAELSTROM_STONES[stone]);

        assertThat(QuestDatabase.Quest.MAELSTROM, isFinished());
        assertThat("maelstromOfLoversBoss", isSetTo(""));
      }
    }
  }

  @Nested
  class Cookbookbat {
    private static Stream<AdventureResult> cookbookbatRecipes() {
      return Stream.of(
          ItemPool.get(ItemPool.ROBY_BORIS_BEER),
          ItemPool.get(ItemPool.ROBY_HONEY_BUN_OF_BORIS),
          ItemPool.get(ItemPool.ROBY_RATATOUILLE_DE_JARLSBERG),
          ItemPool.get(ItemPool.ROBY_JARLSBERGS_VEGETABLE_SOUP),
          ItemPool.get(ItemPool.ROBY_PETES_WILY_WHEY_BAR),
          ItemPool.get(ItemPool.ROBY_PETES_SNEAKY_SMOOTHIE),
          ItemPool.get(ItemPool.ROBY_BORIS_BREAD),
          ItemPool.get(ItemPool.ROBY_ROASTED_VEGETABLE_OF_J),
          ItemPool.get(ItemPool.ROBY_PETES_RICH_RICOTTA),
          ItemPool.get(ItemPool.ROBY_ROASTED_VEGETABLE_FOCACCIA),
          ItemPool.get(ItemPool.ROBY_PLAIN_CALZONE),
          ItemPool.get(ItemPool.ROBY_BAKED_VEGGIE_RICOTTA),
          ItemPool.get(ItemPool.ROBY_DEEP_DISH_OF_LEGEND),
          ItemPool.get(ItemPool.ROBY_CALZONE_OF_LEGEND),
          ItemPool.get(ItemPool.ROBY_PIZZA_OF_LEGEND));
    }

    @ParameterizedTest
    @MethodSource("cookbookbatRecipes")
    public void cookbookbatPropertyGetsUpdated(AdventureResult recipe) {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.COOKBOOKBAT),
              withProperty("_cookbookbatRecipeDrops", false));

      try (cleanups) {
        ResultProcessor.processResult(true, recipe);

        assertThat("_cookbookbatRecipeDrops", isSetTo(true));
      }
    }

    @Test
    public void cookbookbatPropertyNoUpdateIfNotAdventureResult() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.COOKBOOKBAT),
              withProperty("_cookbookbatRecipeDrops", false));

      try (cleanups) {
        ResultProcessor.processResult(false, ItemPool.get(ItemPool.ROBY_BAKED_VEGGIE_RICOTTA));

        assertThat("_cookbookbatRecipeDrops", isSetTo(false));
      }
    }
  }

  @Nested
  class InfiniteDropItems {
    private static Stream<Arguments> infiniteDropFamiliars() {
      return Stream.of(
          Arguments.of(
              FamiliarPool.ROCKIN_ROBIN, ItemPool.get(ItemPool.ROBIN_EGG), "_robinEggDrops"),
          Arguments.of(FamiliarPool.CANDLE, ItemPool.get(ItemPool.WAX_GLOB), "_waxGlobDrops"),
          Arguments.of(
              FamiliarPool.GARBAGE_FIRE,
              ItemPool.get(ItemPool.BURNING_NEWSPAPER),
              "_garbageFireDrops"),
          Arguments.of(
              FamiliarPool.GARBAGE_FIRE,
              ItemPool.get(ItemPool.TOASTED_HALF_SANDWICH),
              "_garbageFireDrops"),
          Arguments.of(
              FamiliarPool.GARBAGE_FIRE,
              ItemPool.get(ItemPool.MULLED_HOBO_WINE),
              "_garbageFireDrops"),
          Arguments.of(
              FamiliarPool.HOBO_IN_SHEEPS_CLOTHING,
              ItemPool.get(ItemPool.GRUBBY_WOOL),
              "_grubbyWoolDrops"));
    }

    @ParameterizedTest
    @MethodSource("infiniteDropFamiliars")
    public void propertyTracksDrops(int familiar, AdventureResult drop, String preference) {
      var cleanups = new Cleanups(withFamiliar(familiar), withProperty(preference, 0));

      try (cleanups) {
        ResultProcessor.processResult(true, drop);

        assertThat(preference, isSetTo(1));
      }
    }
  }

  @Nested
  class AutoCraft {
    @Nested
    class BonerdagonNecklace {
      private static AdventureResult HEMP_STRING = ItemPool.get(ItemPool.HEMP_STRING, 1);
      private static AdventureResult BONERDAGON_VERTEBRA =
          ItemPool.get(ItemPool.BONERDAGON_VERTEBRA, 1);
      private static AdventureResult BONERDAGON_NECKLACE =
          ItemPool.get(ItemPool.BONERDAGON_NECKLACE, 1);

      @Test
      public void getHempStringFromClosetCraftsNecklace() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withNoItems(),
                withItem(BONERDAGON_VERTEBRA),
                withItemInCloset(HEMP_STRING),
                withProperty("autoCraft", true),
                // The Plunger obviates meat paste
                withSign(ZodiacSign.MONGOOSE));

        try (cleanups) {
          client.addResponse(200, html("request/test_uncloset_hemp_string.html"));
          client.addResponse(200, html("request/test_create_bonerdagon_necklace.html"));
          client.addResponse(200, ""); // api.php

          var url = "inventory.php?action=closetpull&ajax=1&whichitem=218&qty=1";
          var request = new GenericRequest(url);
          request.run();

          // We no longer have the Bonerdagon vertebra
          assertThat(BONERDAGON_VERTEBRA.getCount(KoLConstants.inventory), is(0));

          // We do not have a hemp string in closet OR in inventory
          assertThat(HEMP_STRING.getCount(KoLConstants.inventory), is(0));
          assertThat(HEMP_STRING.getCount(KoLConstants.closet), is(0));

          // We have a Bonerdagon necklace
          assertThat(BONERDAGON_NECKLACE.getCount(KoLConstants.inventory), is(1));

          var requests = client.getRequests();
          assertThat(requests, hasSize(3));

          assertPostRequest(
              requests.get(0), "/inventory.php", "action=closetpull&ajax=1&whichitem=218&qty=1");
          assertPostRequest(
              requests.get(1), "/craft.php", "action=craft&mode=combine&ajax=1&a=218&b=1247&qty=1");
          assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void getHempStringFromFightCraftsNecklace() {
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withNoItems(),
                withItem(BONERDAGON_VERTEBRA),
                withFight(),
                withProperty("autoCraft", true),
                // The Plunger obviates meat paste
                withSign(ZodiacSign.MONGOOSE));

        try (cleanups) {
          client.addResponse(200, html("request/test_fight_win_hemp_string.html"));
          client.addResponse(200, html("request/test_create_bonerdagon_necklace.html"));
          client.addResponse(200, ""); // api.php
          client.addResponse(200, ""); // api.php

          var url = "fight.php?action=attack";
          var request = new GenericRequest(url);
          request.run();

          // We no longer have the Bonerdagon vertebra
          assertThat(BONERDAGON_VERTEBRA.getCount(KoLConstants.inventory), is(0));

          // We do not have a hemp string in inventory - even though we won one
          assertThat(HEMP_STRING.getCount(KoLConstants.inventory), is(0));

          // We have a Bonerdagon necklace
          assertThat(BONERDAGON_NECKLACE.getCount(KoLConstants.inventory), is(1));

          var requests = client.getRequests();
          assertThat(requests, hasSize(4));

          assertPostRequest(requests.get(0), "/fight.php", "action=attack");
          assertPostRequest(
              requests.get(1), "/craft.php", "action=craft&mode=combine&ajax=1&a=218&b=1247&qty=1");
          // Once for the fight
          assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
          // Once for the craft
          assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
        }
      }

      @Test
      public void getHempStringFromUsingForceCraftsNecklace() {
        // choice.php?pwd&whichchoice=1387&option=3
        // test_fight_force_hemp_string_.html
        // craft.php?action=craft&mode=combine&ajax=1&a=218&b=1247&qty=1
        // test_create_bonerdagon_necklace.html
        // api.php
        var builder = new FakeHttpClientBuilder();
        var client = builder.client;
        var cleanups =
            new Cleanups(
                withHttpClientBuilder(builder),
                withNoItems(),
                withItem(BONERDAGON_VERTEBRA),
                // Use the Force
                withHandlingChoice(1387),
                withProperty("autoCraft", true),
                // The Plunger obviates meat paste
                withSign(ZodiacSign.MONGOOSE));

        try (cleanups) {
          client.addResponse(200, html("request/test_fight_force_hemp_string.html"));
          client.addResponse(200, html("request/test_create_bonerdagon_necklace.html"));
          client.addResponse(200, ""); // api.php
          client.addResponse(200, ""); // api.php

          var url = "choice.php?pwd&whichchoice=1387&option=3";
          var request = new GenericRequest(url);
          request.run();

          // We are done with the choice
          assertThat(ChoiceManager.handlingChoice, is(false));

          // We no longer have the Bonerdagon vertebra
          assertThat(BONERDAGON_VERTEBRA.getCount(KoLConstants.inventory), is(0));

          // We do not have a hemp string in inventory - even though we won one
          assertThat(HEMP_STRING.getCount(KoLConstants.inventory), is(0));

          // We have a Bonerdagon necklace
          assertThat(BONERDAGON_NECKLACE.getCount(KoLConstants.inventory), is(1));

          var requests = client.getRequests();
          assertThat(requests, hasSize(3));

          assertPostRequest(requests.get(0), "/choice.php", "whichchoice=1387&option=3");
          assertPostRequest(
              requests.get(1), "/craft.php", "action=craft&mode=combine&ajax=1&a=218&b=1247&qty=1");
          assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        }
      }
    }
  }

  @Nested
  class FalsePositives {
    @Test
    void noIRefuseFalsePositive() {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try (PrintStream out = new PrintStream(stream, true)) {
        RequestLogger.openCustom(out);
        ResultProcessor.processResults(true, html("request/test_choice_visit_i_refuse.html"));
        RequestLogger.closeCustom();
      }

      assertThat(
          stream.toString(),
          not(
              containsString(
                  "Could not parse: You lose control of your legs, and begin running as fast as you can in a random direction")));
    }
  }
}
