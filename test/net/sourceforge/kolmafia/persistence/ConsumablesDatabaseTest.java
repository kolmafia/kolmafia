package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSign;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Month;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.Attribute;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.ConsumableQuality;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConsumablesDatabaseTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(true);
    KoLCharacter.reset("ConsumablesDatabaseTest");
    Preferences.reset("ConsumablesDatabaseTest");
  }

  @Nested
  class Basic {
    private static final String nonexistent = "kjfdsalkjjlkfdalkjfdsa";

    @Test
    void fullness() {
      assertThat(ConsumablesDatabase.getRawFullness(nonexistent), nullValue());
      assertThat(ConsumablesDatabase.getRawFullness("Sacramento wine"), nullValue());
      assertThat(ConsumablesDatabase.getRawFullness("jumping horseradish"), is(1));
      assertThat(ConsumablesDatabase.getFullness(nonexistent), is(0));
      assertThat(ConsumablesDatabase.getFullness("Sacramento wine"), is(0));
      assertThat(ConsumablesDatabase.getFullness("jumping horseradish"), is(1));
    }

    @Test
    void inebriety() {
      assertThat(ConsumablesDatabase.getRawInebriety(nonexistent), nullValue());
      assertThat(ConsumablesDatabase.getRawInebriety("jumping horseradish"), nullValue());
      assertThat(ConsumablesDatabase.getRawInebriety("Sacramento wine"), is(1));
      assertThat(ConsumablesDatabase.getInebriety(nonexistent), is(0));
      assertThat(ConsumablesDatabase.getInebriety("jumping horseradish"), is(0));
      assertThat(ConsumablesDatabase.getInebriety("Sacramento wine"), is(1));
    }

    @Test
    void spleen() {
      assertThat(ConsumablesDatabase.getRawSpleenHit(nonexistent), nullValue());
      assertThat(ConsumablesDatabase.getRawSpleenHit("jumping horseradish"), nullValue());
      assertThat(ConsumablesDatabase.getRawSpleenHit("antimatter wad"), is(2));
      assertThat(ConsumablesDatabase.getSpleenHit(nonexistent), is(0));
      assertThat(ConsumablesDatabase.getSpleenHit("jumping horseradish"), is(0));
      assertThat(ConsumablesDatabase.getSpleenHit("antimatter wad"), is(2));
    }

    @Test
    void drunkibears() {
      assertThat(ConsumablesDatabase.getRawFullness("green drunki-bear"), is(4));
      assertThat(ConsumablesDatabase.getRawInebriety("green drunki-bear"), is(4));
      assertThat(ConsumablesDatabase.getFullness("green drunki-bear"), is(4));
      assertThat(ConsumablesDatabase.getInebriety("green drunki-bear"), is(4));
    }

    @Test
    void currentAdventures() {
      assertThat(ConsumablesDatabase.getAverageAdventures(nonexistent), is(0.0));
      assertThat(ConsumablesDatabase.getAverageAdventures("cold wad"), is(0.0));
      assertThat(ConsumablesDatabase.getAverageAdventures("Sacramento wine"), is(5.5));
    }

    @Test
    void currentAdventuresFood() {
      var cleanups =
          new Cleanups(
              withProperty("milkOfMagnesiumActive", true),
              withProperty("munchiesPillsUsed", 0),
              withEffect(EffectPool.BARREL_OF_LAUGHS, 5),
              withSkill("Gourmand"));
      try (cleanups) {
        assertThat(ConsumablesDatabase.getAverageAdventures(nonexistent), is(0.0));
        assertThat(ConsumablesDatabase.getAverageAdventures("jumping horseradish"), is(12.5));
        assertThat(ConsumablesDatabase.getAverageAdventures("Sacramento wine"), is(5.5));
      }
    }

    @Test
    void currentAdventuresBooze() {
      var cleanups =
          new Cleanups(withEffect(EffectPool.BEER_BARREL_POLKA, 5), withEffect(EffectPool.ODE));
      try (cleanups) {
        assertThat(ConsumablesDatabase.getAverageAdventures(nonexistent), is(0.0));
        assertThat(ConsumablesDatabase.getAverageAdventures("jumping horseradish"), is(5.5));
        assertThat(ConsumablesDatabase.getAverageAdventures("Sacramento wine"), is(7.5));
      }
    }

    @Nested
    class Notes {
      @Test
      void canGetListOfAttributes() {
        var consumable = ConsumablesDatabase.getConsumableByName("beertini");
        assertThat(
            ConsumablesDatabase.getAttributes(consumable),
            hasItems(Attribute.BEER, Attribute.MARTINI));
      }

      @ParameterizedTest
      @ValueSource(strings = {"Dump Truck", "herbal stuffing"})
      void canGetBlankListOfAttributes(final String itemName) {
        var consumable = ConsumablesDatabase.getConsumableByName(itemName);
        assertThat(ConsumablesDatabase.getAttributes(consumable), hasSize(0));
      }

      @Test
      void knowledgeOfMartinis() {
        assertThat(ConsumablesDatabase.isMartini(-1), is(false));
        assertThat(ConsumablesDatabase.isMartini(ItemPool.SACRAMENTO_WINE), is(false));
        assertThat(ConsumablesDatabase.isMartini(ItemPool.MARTINI), is(true));
      }

      @Test
      void knowledgeOfWines() {
        assertThat(ConsumablesDatabase.isWine(-1), is(false));
        assertThat(ConsumablesDatabase.isWine(ItemPool.SACRAMENTO_WINE), is(true));
        assertThat(ConsumablesDatabase.isWine(ItemPool.MARTINI), is(false));
      }

      @Test
      void knowledgeOfBeers() {
        assertThat(ConsumablesDatabase.isBeer(-1), is(false));
        assertThat(ConsumablesDatabase.isBeer(ItemPool.GREEN_BEER), is(true));
        assertThat(ConsumablesDatabase.isBeer(ItemPool.MARTINI), is(false));
      }

      @Test
      void knowledgeOfCannedBeers() {
        assertThat(ConsumablesDatabase.isCannedBeer(-1), is(false));
        assertThat(ConsumablesDatabase.isCannedBeer(41 /* ice-cold Sir Schlitz */), is(true));
        assertThat(ConsumablesDatabase.isCannedBeer(ItemPool.GREEN_BEER), is(false));
      }

      @Test
      void knowledgeOfLasagnas() {
        assertThat(ConsumablesDatabase.isLasagna(-1), is(false));
        assertThat(ConsumablesDatabase.isLasagna(ItemPool.FISHY_FISH_LASAGNA), is(true));
        assertThat(ConsumablesDatabase.isLasagna(ItemPool.HELL_RAMEN), is(false));
      }

      @Test
      void knowledgeOfSauciness() {
        assertThat(ConsumablesDatabase.isSaucy(-1), is(false));
        assertThat(ConsumablesDatabase.isSaucy(ItemPool.FISHY_FISH_LASAGNA), is(false));
        assertThat(ConsumablesDatabase.isSaucy(ItemPool.HELL_RAMEN), is(true));
      }

      @Test
      void knowledgeOfPizzas() {
        assertThat(ConsumablesDatabase.isPizza(-1), is(false));
        assertThat(ConsumablesDatabase.isPizza(ItemPool.DIABOLIC_PIZZA), is(true));
        assertThat(ConsumablesDatabase.isPizza(ItemPool.HELL_RAMEN), is(false));
      }

      @Test
      void knowledgeOfBeans() {
        assertThat(ConsumablesDatabase.isBeans(-1), is(false));
        assertThat(ConsumablesDatabase.isBeans(ItemPool.MUS_BEANS_PLATE), is(true));
        assertThat(ConsumablesDatabase.isBeans(ItemPool.HELL_RAMEN), is(false));
      }

      @Test
      void knowledgeOfSalads() {
        assertThat(ConsumablesDatabase.isSalad(-1), is(false));
        assertThat(ConsumablesDatabase.isSalad(ItemPool.KUDZU_SALAD), is(true));
        assertThat(ConsumablesDatabase.isSalad(ItemPool.HELL_RAMEN), is(false));
      }
    }

    @Test
    void levelRequirement() {
      assertThat(ConsumablesDatabase.getLevelReqByName("extra-greasy slider"), is(13));

      var cleanups = withLevel(1);
      try (cleanups) {
        assertThat(ConsumablesDatabase.meetsLevelRequirement("extra-greasy slider"), is(false));
      }

      var cleanups2 = new Cleanups(withLevel(13), withInteractivity(true));
      try (cleanups2) {
        assertThat(ConsumablesDatabase.meetsLevelRequirement("extra-greasy slider"), is(true));
      }
    }

    @Test
    void baseStatGains() {
      assertThat(ConsumablesDatabase.getBaseMuscleByName(nonexistent), equalTo(""));
      assertThat(ConsumablesDatabase.getBaseMysticalityByName(nonexistent), equalTo(""));
      assertThat(ConsumablesDatabase.getBaseMoxieByName(nonexistent), equalTo(""));

      assertThat(ConsumablesDatabase.getBaseMuscleByName("mushroom pizza"), equalTo("0"));
      assertThat(ConsumablesDatabase.getBaseMysticalityByName("mushroom pizza"), equalTo("15-18"));
      assertThat(ConsumablesDatabase.getBaseMoxieByName("mushroom pizza"), equalTo("0"));
    }

    @Test
    void calculatedStatGains() {
      assertThat(ConsumablesDatabase.getMuscleRange(nonexistent), equalTo("+0.0"));
      assertThat(ConsumablesDatabase.getMysticalityRange(nonexistent), equalTo("+0.0"));
      assertThat(ConsumablesDatabase.getMoxieRange(nonexistent), equalTo("+0.0"));

      assertThat(ConsumablesDatabase.getMuscleRange("mushroom pizza"), equalTo("+0.0"));
      assertThat(ConsumablesDatabase.getMysticalityRange("mushroom pizza"), equalTo("+16.5"));
      assertThat(ConsumablesDatabase.getMoxieRange("mushroom pizza"), equalTo("+0.0"));

      var cleanups = withSkill("Pizza Lover");
      try (cleanups) {
        assertThat(ConsumablesDatabase.getMuscleRange("mushroom pizza"), equalTo("+0.0"));
        assertThat(ConsumablesDatabase.getMysticalityRange("mushroom pizza"), equalTo("+33.0"));
        assertThat(ConsumablesDatabase.getMoxieRange("mushroom pizza"), equalTo("+0.0"));
      }

      var cleanups2 =
          new Cleanups(
              withEffect("Different Way of Seeing Things"),
              withEffect(EffectPool.SYNTHESIS_LEARNING));
      try (cleanups2) {
        KoLCharacter.recalculateAdjustments();
        assertThat(ConsumablesDatabase.getMuscleRange("mushroom pizza"), equalTo("+0.0"));
        assertThat(ConsumablesDatabase.getMysticalityRange("mushroom pizza"), equalTo("+33.0"));
        assertThat(ConsumablesDatabase.getMoxieRange("mushroom pizza"), equalTo("+0.0"));
      }
    }
  }

  @Nested
  class VariableConsumables {
    @ParameterizedTest
    @CsvSource({"0, 0, 0", "9, 0, 0", "10, 2, 3.0", "50, 10, 5.0", "100, 20, 6.0"})
    void setDistillateData(int drams, int effectTurns, double adventures) {
      var cleanups = new Cleanups(withProperty("familiarSweat", drams));

      try (cleanups) {
        ConsumablesDatabase.setDistillateData();
        assertThat(
            ConsumablesDatabase.getNotes("stillsuit distillate"),
            equalTo(effectTurns + " Buzzed on Distillate"));
        assertThat(
            ConsumablesDatabase.getAverageAdventures("stillsuit distillate"), equalTo(adventures));
      }
    }
  }

  @Nested
  class AdventureRange {
    @Test
    void appliesOde() {
      var cleanups = new Cleanups(withEffect(EffectPool.ODE, 3));

      try (cleanups) {
        assertThat(ConsumablesDatabase.getAverageAdventures("bottle of gin"), is(6.0));
      }
    }

    @Disabled("We don't apply this yet! We need to refactor the gain effects")
    @Test
    void partlyAppliesOde() {
      var cleanups = new Cleanups(withEffect(EffectPool.ODE, 2));

      try (cleanups) {
        assertThat(ConsumablesDatabase.getAverageAdventures("bottle of gin"), is(6.0));
      }
    }

    @Test
    void doesNotApplyOdeToStillsuit() {
      var cleanups = new Cleanups(withEffect(EffectPool.ODE), withProperty("familiarSweat", 10));

      try (cleanups) {
        ConsumablesDatabase.setDistillateData();
        assertThat(ConsumablesDatabase.getAverageAdventures("stillsuit distillate"), is(3.0));
      }
    }

    @Test
    void appliesMilk() {
      var cleanups = new Cleanups(withProperty("milkOfMagnesiumActive", true));

      try (cleanups) {
        assertThat(ConsumablesDatabase.getAverageAdventures("fortune cookie"), is(6.0));
      }
    }

    @Test
    void doesNotApplyMilkToSushi() {
      var cleanups = new Cleanups(withProperty("milkOfMagnesiumActive", true));

      try (cleanups) {
        assertThat(ConsumablesDatabase.getAverageAdventures("beefy nigiri"), is(6.0));
      }
    }

    @ParameterizedTest
    @CsvSource({"2016, 6, 18", "2011, 3, 17"})
    void borisDayImprovesSomeConsumables(int year, int month, int day) {
      var cleanups = new Cleanups(withDay(year, Month.of(month), day));

      try (cleanups) {
        ConsumablesDatabase.reset();
        assertThat(ConsumablesDatabase.getAverageAdventures("bottle of gin"), is(3.0));
        assertThat(ConsumablesDatabase.getQuality("bottle of gin"), is(ConsumableQuality.CRAPPY));
        assertThat(ConsumablesDatabase.getAverageAdventures("cranberries"), is(3.0));
        assertThat(ConsumablesDatabase.getQuality("cranberries"), is(ConsumableQuality.GOOD));
        assertThat(ConsumablesDatabase.getAverageAdventures("redrum"), is(7.0));
        assertThat(ConsumablesDatabase.getQuality("redrum"), is(ConsumableQuality.GOOD));
        assertThat(ConsumablesDatabase.getAverageAdventures("vodka and cranberry"), is(7.5));
        assertThat(
            ConsumablesDatabase.getQuality("vodka and cranberry"), is(ConsumableQuality.GOOD));
      }

      ConsumablesDatabase.reset();
    }
  }

  @Nested
  class TCRS {
    static Cleanups CLEANUPS = new Cleanups();

    @BeforeAll
    static void beforeAll() {
      CLEANUPS.add(withPath(AscensionPath.Path.CRAZY_RANDOM_SUMMER_TWO));
      CLEANUPS.add(withClass(AscensionClass.PASTAMANCER));
      CLEANUPS.add(withSign(ZodiacSign.PACKRAT));

      DebugDatabase.cacheItemDescriptionText(
          ItemPool.RING, html("request/test_tcrs_desc_item_ring.html"));
      TCRSDatabase.loadTCRSData();
    }

    @AfterAll
    static void afterAll() throws IOException {
      CLEANUPS.close();

      DebugDatabase.cacheItemDescriptionText(
          ItemPool.RING, html("request/test_normal_desc_item_ring.html"));
      TCRSDatabase.resetModifiers();
      try (var walker = Files.walk(KoLConstants.DATA_LOCATION.toPath())) {
        walker
            .map(Path::toFile)
            .filter(f -> f.getName().startsWith("TCRS_"))
            .filter(f -> f.getName().endsWith(".txt"))
            .forEach(File::delete);
      }
    }

    @Test
    void spleenItemsAreModified() {
      // Spleen items should now be size 1 and provide no adventures.
      assertThat(ConsumablesDatabase.getSpleenHit("antimatter wad"), is(1));
      assertThat(ConsumablesDatabase.getBaseAdventureRange("antimatter wad"), equalTo("0"));
    }

    @Test
    void consumableAttributesAreMaintained() {
      // Attributes should be maintained
      assertThat(ConsumablesDatabase.isWine(ItemPool.BUCKET_OF_WINE), is(true));
      assertThat(ConsumablesDatabase.isCannedBeer(ItemPool.WILLER), is(true));
    }
  }
}
