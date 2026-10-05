package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withDay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.time.Month;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import org.junit.jupiter.api.Test;

class SetHolidayCommandTest extends AbstractCommandTestBase {
  public SetHolidayCommandTest() {
    this.command = "holiday";
  }

  @Test
  void canOverrideHoliday() {
    try (var _ = withDay(2023, Month.JULY, 4)) {
      execute("Yuletide");
      assertThat(HolidayDatabase.getEvents(), containsInAnyOrder("Yuletide", "Dependence Day"));
    }
  }
}
