package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.matchers.Preference.isSetTo;
import static internal.matchers.Quest.isFinished;
import static internal.matchers.Quest.isStep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.time.Month;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.combat.MonsterStatusTracker;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
      EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
      // This was an Oyster Egg Day.
      final var cleanups = withDay(2022, Month.JANUARY, 29, 12, 0);
      try (cleanups) {
        ResultProcessor.processResult(true, MAGNIFICENT_OYSTER_EGG);
      }
    }

    @Test
    public void obtainOysterEggOnWrongDay() {
      EquipmentManager.setEquipment(EquipmentManager.OFFHAND, ItemPool.get(ItemPool.OYSTER_BASKET));
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
    private static Stream<AdventureResult> coobookbatRecipes() {
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
    @MethodSource("coobookbatRecipes")
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
}
