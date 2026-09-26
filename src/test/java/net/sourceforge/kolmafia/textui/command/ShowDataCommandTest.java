package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withDay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.time.Month;
import org.junit.jupiter.api.Test;

public class ShowDataCommandTest extends AbstractCommandTestBase {

  @Test
  public void canPrintDateInfo() {
    this.command = "moons";

    var cleanups = withDay(2022, Month.AUGUST, 21);
    try (cleanups) {
      String output = execute("");
      assertThat(output, containsString("August 21, 2022 - Martinus 6"));
      assertThat(output, containsString("Ronald: waning gibbous"));
      assertThat(output, containsString("Grimace: first quarter"));
      assertThat(output, containsString("Mini-moon: front center"));
      assertThat(output, containsString("Arrrbor Day: 22 days"));
      assertThat(output, containsString("3 days until Muscle."));
    }
  }
}
