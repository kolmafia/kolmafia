package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDay;
import static net.sourceforge.kolmafia.persistence.HolidayDatabase.ROLLOVER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import java.time.Month;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

  @Nested
  class Monday {
    @ParameterizedTest
    @CsvSource({"22, true", "23, false"})
    void canDetectTodayIsMonday(final int date, final boolean isMonday) {
      var cleanups = new Cleanups(withDay(2022, Month.AUGUST, date));

      try (cleanups) {
        assertThat(HolidayDatabase.isMonday(), equalTo(isMonday));
      }
    }

    @ParameterizedTest
    @CsvSource({"22, true", "23, false"})
    void canDetectADayIsMonday(final int date, final boolean isMonday) {
      var dateTime = ZonedDateTime.of(2022, Month.AUGUST.getValue(), date, 0, 0, 0, 0, ROLLOVER);
      assertThat(HolidayDatabase.isMonday(dateTime), equalTo(isMonday));
    }
  }

  @Nested
  class RealLifeHolidays {
    @ParameterizedTest
    @CsvSource({
      "0101,Festival of Jarlsberg",
      "0214,Valentine's Day",
      "0317,St. Sneaky Pete's Day",
      "0704,Dependence Day",
      "0417,Oyster Egg Day",
      "1124,Feast of Boris",
      "1031,Halloween",
      "0202,Groundhog Day",
      "0401,April Fool's Day",
      "0919,Talk Like a Pirate Day",
      "1225,Crimbo",
      "1022,Holatuwol's Birthday",
      "0923,Veracity's Birthday",
      "0217,Gausie's Birthday",
      "0102,"
    })
    void canIdentifyRealLifeHolidays(final String date, final String holiday) {
      assertThat(HolidayDatabase.getRealLifeHoliday("2022" + date), equalTo(holiday));
    }
  }
}
