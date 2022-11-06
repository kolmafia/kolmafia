package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Cleanups;
import java.time.Duration;
import java.time.Instant;
import java.time.Month;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.ConsumableQuality;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
      assertThat(ConsumablesDatabase.getRawFullness("jumping horseradish"), equalTo(1));
      assertThat(ConsumablesDatabase.getFullness(nonexistent), equalTo(0));
      assertThat(ConsumablesDatabase.getFullness("Sacramento wine"), equalTo(0));
      assertThat(ConsumablesDatabase.getFullness("jumping horseradish"), equalTo(1));
    }

    @Test
    void inebriety() {
      assertThat(ConsumablesDatabase.getRawInebriety(nonexistent), nullValue());
      assertThat(ConsumablesDatabase.getRawInebriety("jumping horseradish"), nullValue());
      assertThat(ConsumablesDatabase.getRawInebriety("Sacramento wine"), equalTo(1));
      assertThat(ConsumablesDatabase.getInebriety(nonexistent), equalTo(0));
      assertThat(ConsumablesDatabase.getInebriety("jumping horseradish"), equalTo(0));
      assertThat(ConsumablesDatabase.getInebriety("Sacramento wine"), equalTo(1));
    }

    @Test
    void spleen() {
      assertThat(ConsumablesDatabase.getRawSpleenHit(nonexistent), nullValue());
      assertThat(ConsumablesDatabase.getRawSpleenHit("jumping horseradish"), nullValue());
      assertThat(ConsumablesDatabase.getRawSpleenHit("antimatter wad"), equalTo(2));
      assertThat(ConsumablesDatabase.getSpleenHit(nonexistent), equalTo(0));
      assertThat(ConsumablesDatabase.getSpleenHit("jumping horseradish"), equalTo(0));
      assertThat(ConsumablesDatabase.getSpleenHit("antimatter wad"), equalTo(2));
    }

    @Test
    void currentAdventures() {
      assertThat(ConsumablesDatabase.getAverageAdventures(nonexistent), equalTo(0.0));
      assertThat(ConsumablesDatabase.getAverageAdventures("cold wad"), equalTo(0.0));
      assertThat(ConsumablesDatabase.getAverageAdventures("Sacramento wine"), equalTo(5.5));
    }

    @Test
    void currentAdventuresFood() {
      var cleanups = new Cleanups(
          withProperty("milkOfMagnesiumActive", true),
          withProperty("munchiesPillsUsed", 0),
          withEffect(EffectPool.BARREL_OF_LAUGHS, 5),
          withSkill("Gourmand")
          );
      try (cleanups) {
        assertThat(ConsumablesDatabase.getAverageAdventures(nonexistent), equalTo(0.0));
        assertThat(ConsumablesDatabase.getAverageAdventures("jumping horseradish"), equalTo(12.5));
        assertThat(ConsumablesDatabase.getAverageAdventures("Sacramento wine"), equalTo(5.5));
      }
    }

    @Test
    void currentAdventuresBooze() {
      var cleanups = new Cleanups(
          withEffect(EffectPool.BEER_BARREL_POLKA, 5),
          withEffect(EffectPool.ODE)
          );
      try (cleanups) {
        assertThat(ConsumablesDatabase.getAverageAdventures(nonexistent), equalTo(0.0));
        assertThat(ConsumablesDatabase.getAverageAdventures("jumping horseradish"), equalTo(5.5));
        assertThat(ConsumablesDatabase.getAverageAdventures("Sacramento wine"), equalTo(7.5));
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
}
