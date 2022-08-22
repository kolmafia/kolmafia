package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDay;
import static net.sourceforge.kolmafia.persistence.HolidayDatabase.ROLLOVER;
import static net.sourceforge.kolmafia.persistence.HolidayDatabase.getHolidayPredictions;
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

      assertThat(position, equalTo(7));
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

  @Nested
  class InGameHolidays {
    @ParameterizedTest
    @CsvSource({
      "1,1,Festival of Jarlsberg: today|Valentine's Day: 14 days|St. Sneaky Pete's Day: 21 days|Oyster Egg Day: 28 days|El Dia De Los Muertos Borrachos: 36 days|Generic Summer Holiday: 45 days|Dependence Day: 54 days|Arrrbor Day: 62 days|Lab&oacute;r Day: 72 days|Halloween: 82 days|Feast of Boris: 89 days|Yuletide: 94 days",
      "12,6,Lab&oacute;r Day: 6 days|Halloween: 16 days|Dependence Day: 22 days|Feast of Boris: 23 days|Yuletide: 28 days|Festival of Jarlsberg: 33 days|Valentine's Day: 44 days|St. Sneaky Pete's Day: 51 days|Oyster Egg Day: 58 days|El Dia De Los Muertos Borrachos: 66 days|Generic Summer Holiday: 75 days|Arrrbor Day: 92 days",
    })
    void canIdentifyAllHolidays(final int day, final int month, final String holidays) {
      var date = ZonedDateTime.of(2022, month, day, 0, 0, 0, 0, ROLLOVER);
      var actualHolidays = getHolidayPredictions(date);
      assertThat(String.join("|", actualHolidays), equalTo(holidays));
    }
  }

  @Nested
  class Moons {
    @ParameterizedTest
    @CsvSource({
      "1,waxing crescent,new moon",
      "2,first quarter,waxing crescent",
      "4,full moon,first quarter",
      "6,third quarter,waxing gibbous",
      "8,new moon,full moon",
      "9,waxing crescent,full moon",
      "11,waxing gibbous,waning gibbous",
      "13,waning gibbous,third quarter",
      "15,waning crescent,waning crescent",
    })
    void canIdentityMoonPosition(final int day, final String ronald, final String grimace) {
      var cleanups = withDay(2022, Month.AUGUST, day);

      try (cleanups) {
        assertThat(HolidayDatabase.getRonaldPhaseAsString(), equalTo(ronald));
        assertThat(HolidayDatabase.getGrimacePhaseAsString(), equalTo(grimace));
      }
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = ';',
        value = {
          "1;behind Grimace",
          "2;in back, near Ronald",
          "3;far left",
          "4;in front of Ronald, R side",
          "5;in front of Grimace, L side",
          "6;far right",
          "7;in back, near Grimace",
          "8;behind Ronald",
          "9;in front of Ronald, L side",
          "10;front center",
          "11;in front of Grimace, R side",
        })
    void canIdentityHamburglarPosition(final int day, final String hamburglar) {
      var cleanups = withDay(2022, Month.AUGUST, day);

      try (cleanups) {
        assertThat(HolidayDatabase.getHamburglarPositionAsString(), equalTo(hamburglar));
      }
    }
  }
}
