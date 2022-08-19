package net.sourceforge.kolmafia.persistence;

import static net.sourceforge.kolmafia.persistence.HolidayDatabase.ROLLOVER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.time.Month;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HolidayDatabaseTest {
  @Nested
  class HamburgularPosition {
    @Test
    void noPositionBeforeCollision() {
      var date = ZonedDateTime.of(2006, Month.JUNE.getValue(), 2, 0, 0, 0, 0, ROLLOVER);
      var position = HolidayDatabase.getHamburglarPosition(date);

      assertThat(position, equalTo(-1));
    }

    @Test
    void canDeterminePosition() {
      var date = ZonedDateTime.of(2013, Month.FEBRUARY.getValue(), 17, 0, 0, 0, 0, ROLLOVER);
      var position = HolidayDatabase.getHamburglarPosition(date);

      assertThat(position, equalTo(4));
    }
  }
}
