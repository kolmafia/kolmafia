package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.setProperty;
import static internal.helpers.Player.setWorkshed;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ColdMedicineCabinetCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ColdMedicineCabinetCommandTest");
  }

  public ColdMedicineCabinetCommandTest() {
    this.command = "cmc";
  }

  @ParameterizedTest
  @CsvSource({
    "i, Extrovermectin&trade;",
    "o, Homebodyl&trade;",
    "u, Breathitin&trade;",
    "x, Fleshazole&trade;",
    "?, unknown"
  })
  void showsRightPillForRightMajority(String environment, String pill) {
    var cleanups =
        new Cleanups(
            setProperty("lastCombatEnvironments", environment.repeat(11) + "x".repeat(9)),
            setProperty("_nextColdMedicineConsult", 1),
            setWorkshed(ItemPool.COLD_MEDICINE_CABINET),
            withTurnsPlayed(0));

    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("Your next pill should be " + pill));
      assertContinueState();
    }
  }

  @Test
  void showsFleshazoleForNoOverallMajority() {
    var cleanups =
        new Cleanups(
            setProperty("lastCombatEnvironments", "iiiiiioooooouuuuuuio"),
            setProperty("_nextColdMedicineConsult", 1),
            setWorkshed(ItemPool.COLD_MEDICINE_CABINET),
            withTurnsPlayed(0));

    try (cleanups) {
      String output = execute("");

      assertThat(output, containsString("Your next pill should be Fleshazole&trade;"));
      assertContinueState();
    }
  }
}
