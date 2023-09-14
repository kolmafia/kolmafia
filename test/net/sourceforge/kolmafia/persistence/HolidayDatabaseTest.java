package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDay;
import static net.sourceforge.kolmafia.persistence.DateTimeManager.ROLLOVER;
import static net.sourceforge.kolmafia.persistence.HolidayDatabase.getEvents;
import static net.sourceforge.kolmafia.persistence.HolidayDatabase.getHolidayPredictions;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import internal.helpers.Cleanups;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

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
      "1,1,Festival of Jarlsberg",
      "2,14,Valentine's Day",
      "3,17,St. Sneaky Pete's Day",
      "7,4,Dependence Day",
      "4,17,Oyster Egg Day",
      "11,24,Feast of Boris",
      "10,31,Halloween",
      "2,2,Groundhog Day",
      "4,1,April Fool's Day",
      "9,19,Talk Like a Pirate Day",
      "12,25,Crimbo",
      "10,22,Holatuwol's Birthday",
      "9,23,Veracity's Birthday",
      "2,17,Gausie's Birthday",
      "1,2,"
    })
    void canIdentifyRealLifeHolidays(final int month, int day, final String holiday) {
      var date = LocalDate.of(2022, month, day);
      assertThat(HolidayDatabase.getRealLifeHoliday(date), equalTo(holiday));
    }

    @Test
    void canCalculateThanksgiving() {
      var date = HolidayDatabase.getThanksgiving(Year.of(1991));
      assertThat(date, equalTo(MonthDay.of(Month.NOVEMBER, 28)));
    }

    @Test
    void canCalculateEaster() {
      var date = HolidayDatabase.getEaster(Year.of(1991));
      assertThat(date, equalTo(MonthDay.of(Month.MARCH, 31)));
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void isRealHoliday(final boolean scaryDay) {
      var date = ZonedDateTime.of(2022, 10, scaryDay ? 31 : 1, 0, 0, 0, 0, ROLLOVER);

      assertThat(HolidayDatabase.isRealLifeHoliday(date), is(scaryDay));
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

    @Test
    void canIdentifyLaborDayEve() {
      var date = ZonedDateTime.of(2023, 4, 1, 0, 0, 0, 0, ROLLOVER);
      var events = getEvents(date);

      assertThat(
          events,
          containsInAnyOrder("Lab&oacute;r Day Eve", "April Fool's Day", "Mysticality Day"));
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void isGameHoliday(final boolean fob) {
      var date = ZonedDateTime.of(2023, 7, fob ? 24 : 25, 0, 0, 0, 0, ROLLOVER);

      assertThat(HolidayDatabase.isGameHoliday(date), is(fob));
    }

    @Test
    void getGameHolidayInDays() {
      var date = ZonedDateTime.of(2023, 7, 28, 0, 0, 0, 0, ROLLOVER);
      assertThat(HolidayDatabase.getGameHolidayInDays(date, 1), is("Yuletide"));
    }

    @CsvSource({
      "2023, 5, 4, 6 days until Valentine's Day",
      "2023, 5, 16, St. Sneaky Pete's Day tomorrow",
      "2023, 6, 1, El Dia De Los Muertos Borrachos today",
    })
    @ParameterizedTest
    void getHolidaySummary(final int year, final int month, final int day, final String summary) {
      var date = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, ROLLOVER);
      assertThat(HolidayDatabase.getHolidaySummary(date), is(summary));
    }

    @Test
    void handlesIfSomehowThereAreNoUpcomingHolidays() {
      try (var mock = Mockito.mockStatic(HolidayDatabase.class, Mockito.CALLS_REAL_METHODS)) {
        mock.when(() -> HolidayDatabase.getGameHolidayInDays(any(ZonedDateTime.class), anyInt()))
            .thenReturn(null);
        var date = ZonedDateTime.of(2023, 6, 2, 0, 0, 0, 0, ROLLOVER);
        assertThat(HolidayDatabase.getHolidaySummary(date), is(""));
      }
    }

    @CsvSource({
      "2011, 3, 17, Drunksgiving",
      "2017, 11, 23, El Dia De Los Muertos Borrachos y Agradecido",
      "2021, 3, 17, Yuletide / St. Sneaky Pete's Day",
    })
    @ParameterizedTest
    void getHoliday(final int year, final int month, final int day, final String holiday) {
      try (var cleanups = withDay(year, Month.of(month), day)) {
        assertThat(HolidayDatabase.getHoliday(), is(holiday));
      }
    }

    @CsvSource({
      "2011, 3, 17, Drunksgiving",
      "2017, 11, 23, El Dia De Los Muertos Borrachos y Agradecido",
      "2021, 3, 17, Yuletide / St. Sneaky Pete's Day",
    })
    @ParameterizedTest
    void getHolidays(final int year, final int month, final int day, final String holiday) {
      try (var cleanups = withDay(year, Month.of(month), day)) {
        assertThat(HolidayDatabase.getHolidays(), containsInAnyOrder(holiday.split(" / ")));
      }
    }

    @CsvSource({
      "2011, 3, 17, true", // Drunksgiving
      "2017, 11, 23, true", // Borrachos y Agradecido
      "2023, 4, 19, true", // Feast of Boris
      "2021, 4, 15, false", // Nothing
    })
    @ParameterizedTest
    void isFeastOfBorisLike(final int year, final int month, final int day, final boolean fobLike) {
      try (var cleanups = withDay(year, Month.of(month), day)) {
        assertThat(HolidayDatabase.isFeastOfBorisLike(), is(fobLike));
      }
    }
  }

  @Nested
  class StatDays {
    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void isMoxieDay(final boolean statDay) {
      var date = ZonedDateTime.of(2023, 7, statDay ? 2 : 3, 0, 0, 0, 0, ROLLOVER);

      assertThat(HolidayDatabase.isMoxieDay(date), is(statDay));
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void isMysticalityDay(final boolean statDay) {
      var date = ZonedDateTime.of(2023, 7, statDay ? 6 : 7, 0, 0, 0, 0, ROLLOVER);

      assertThat(HolidayDatabase.isMysticalityDay(date), is(statDay));
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void isMuscleDay(final boolean statDay) {
      var date = ZonedDateTime.of(2023, 7, statDay ? 11 : 12, 0, 0, 0, 0, ROLLOVER);

      assertThat(HolidayDatabase.isMuscleDay(date), is(statDay));
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
    void canIdentifyHamburglarPosition(final int day, final String hamburglar) {
      var cleanups = withDay(2022, Month.AUGUST, day);

      try (cleanups) {
        assertThat(HolidayDatabase.getHamburglarPositionAsString(), equalTo(hamburglar));
      }
    }

    @ParameterizedTest
    @CsvSource({
      // Phase Step 1 (error of -2)
      "4, 5951",
      // Phase Step 15 (error of 12; should be treated as -4)
      "18, 5953",
    })
    void canAdjustForInvalidMoonPhase(final int day, final long diff) {

      var cleanups = new Cleanups(withDay(2023, Month.AUGUST, day));

      try (cleanups) {
        // Phase Step 3
        HolidayDatabase.setMoonPhases(3, 2);

        assertThat(
            HolidayDatabase.getDayDifference(ZonedDateTime.of(2022, 1, 1, 0, 0, 0, 0, ROLLOVER)),
            is(diff));
      }
    }
  }
}
