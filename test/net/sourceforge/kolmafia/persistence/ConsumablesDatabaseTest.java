package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.time.Month;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.ConsumableQuality;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
            ConsumablesDatabase.getAdventureRange("stillsuit distillate"), equalTo(adventures));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({"2016, 6, 18", "2011, 3, 17"})
  void borisDayImprovesSomeConsumables(int year, int month, int day) {
    var cleanups = new Cleanups(withDay(year, Month.of(month), day));

    try (cleanups) {
      ConsumablesDatabase.reset();
      assertThat(ConsumablesDatabase.getAdventureRange("cranberries"), is(3.0));
      assertThat(ConsumablesDatabase.getQuality("cranberries"), is(ConsumableQuality.GOOD));
      assertThat(ConsumablesDatabase.getAdventureRange("redrum"), is(7.0));
      assertThat(ConsumablesDatabase.getQuality("redrum"), is(ConsumableQuality.GOOD));
      assertThat(ConsumablesDatabase.getAdventureRange("vodka and cranberry"), is(7.5));
      assertThat(ConsumablesDatabase.getQuality("vodka and cranberry"), is(ConsumableQuality.GOOD));
    }

    ConsumablesDatabase.reset();
  }
}
