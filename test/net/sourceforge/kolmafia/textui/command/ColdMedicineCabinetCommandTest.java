package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.setMoxie;
import static internal.helpers.Player.setMuscle;
import static internal.helpers.Player.setMysticality;
import static internal.helpers.Player.setProperty;
import static internal.helpers.Player.setWorkshed;
import static internal.helpers.Player.withTurnsPlayed;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
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

  @Nested
  class Guessing {
    @Nested
    class Equipment {
      @BeforeAll
      public static void beforeAll() {
        KoLCharacter.reset("ColdMedicineCabinetCommandTest");
      }

      @ParameterizedTest
      @CsvSource({"0, ice crown", "1, frozen jeans", "2, ice wrap", "3, ice wrap", "4, ice wrap"})
      void showsRightEquipmentForNumberTaken(int taken, String itemName) {
        var cleanups =
            new Cleanups(
                setProperty("_coldMedicineEquipmentTaken", taken),
                setProperty("_nextColdMedicineConsult", 1),
                setWorkshed(ItemPool.COLD_MEDICINE_CABINET),
                withTurnsPlayed(0));
        try (cleanups) {
          String output = execute("");

          assertThat(output, containsString("Your next equipment should be " + itemName));
          assertContinueState();
        }
      }
    }

    @Nested
    class Booze {
      @BeforeAll
      public static void beforeAll() {
        KoLCharacter.reset("ColdMedicineCabinetCommandTest");
      }

      @ParameterizedTest
      @CsvSource({
        "100, 0, 0, Doc's Fortifying Wine",
        "0, 100, 0, Doc's Smartifying Wine",
        "0, 0, 100, Doc's Limbering Wine",
        "100, 0, 100, Doc's Special Reserve Wine",
        "100, 100, 100, Doc's Special Reserve Wine",
        "3, 3, 3, Doc's Medical-Grade Wine",
      })
      void showsRightWineForStatBuff(int mus, int mys, int mox, String itemName) {
        var cleanups =
            new Cleanups(
                setMuscle(1, mus),
                setMysticality(1, mys),
                setMoxie(1, mox),
                setProperty("_nextColdMedicineConsult", 1),
                setWorkshed(ItemPool.COLD_MEDICINE_CABINET),
                withTurnsPlayed(0));

        try (cleanups) {
          String output = execute("");

          assertThat(output, containsString("Your next booze should be " + itemName));
          assertContinueState();
        }
      }
    }

    @Nested
    class Pill {
      @BeforeAll
      public static void beforeAll() {
        KoLCharacter.reset("ColdMedicineCabinetCommandTest");
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
  }
}
