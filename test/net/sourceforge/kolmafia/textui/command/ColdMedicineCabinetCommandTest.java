package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
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
        new Cleanups(setProperty("lastCombatEnvironments", environment.repeat(11) + "x".repeat(9)));

    try (cleanups) {
      String output = execute("");

      assertThat(output, startsWith("Your next pill is " + pill));
      assertContinueState();
    }
  }

  @Test
  void showsFleshazoleForNoOverallMajority() {
    var cleanups = new Cleanups(setProperty("lastCombatEnvironments", "iiiiiioooooouuuuuuio"));

    try (cleanups) {
      String output = execute("");

      assertThat(output, startsWith("Your next pill is Fleshazole&trade;"));
      assertContinueState();
    }
  }
}
