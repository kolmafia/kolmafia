package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
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
}
