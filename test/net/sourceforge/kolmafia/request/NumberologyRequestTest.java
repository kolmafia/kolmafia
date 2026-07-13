package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withPostChoice2;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.DailyLimitDatabase.DailyLimitType;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NumberologyRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("NumberologyRequestTest");
    Preferences.reset("NumberologyRequestTest");
  }

  @ParameterizedTest
  @CsvSource({"true,5", "false,2"})
  void numberologyLimitedUnderRestrictions(boolean canInteract, int remainingUsesExpected) {
    var cleanups =
        new Cleanups(
            withInteractivity(canInteract),
            withProperty("_universeCalculated", 1),
            withProperty("skillLevel144", 6));

    try (cleanups) {
      int remainingUses =
          DailyLimitType.CAST.getDailyLimit(SkillPool.CALCULATE_THE_UNIVERSE).getUsesRemaining();
      assertEquals(remainingUsesExpected, remainingUses);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "false,6", // Third in-run use locks out skill for the rest of the day
    "true,3" // Third use occurring out-of-run allows further uses
  })
  void numberologyQuirkHandled(boolean canInteract, int postCastUses) {
    var cleanups =
        new Cleanups(
            withInteractivity(canInteract),
            withProperty("_universeCalculated", 2),
            withProperty("skillLevel144", 6),
            withPostChoice2(1103, 0));

    try (cleanups) {
      assertThat("_universeCalculated", isSetTo(postCastUses));
    }
  }
}
