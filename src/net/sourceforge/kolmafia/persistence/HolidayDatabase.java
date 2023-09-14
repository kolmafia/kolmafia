package net.sourceforge.kolmafia.persistence;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

public class HolidayDatabase {
  // The epoch of the Kingdom of Loathing.
  private static ZonedDateTime NEWYEAR = null;

  // The date of White Wednesday, which throws everything off by a day
  private static ZonedDateTime BOUNDARY = null;

  // The day the thing crashed into Grimace
  private static ZonedDateTime COLLISION = null;

  private static int RONALD_PHASE = -1;
  private static int GRIMACE_PHASE = -1;
  private static int HAMBURGLAR_POSITION = -1;
  private static String HOLIDAY_OVERRIDE = null;

  static {
    reset();
  }

  public static void reset() {
    NEWYEAR =
        ZonedDateTime.of(
            2005, Month.SEPTEMBER.getValue(), 17, 0, 0, 0, 0, DateTimeManager.ROLLOVER);
    BOUNDARY =
        ZonedDateTime.of(2005, Month.OCTOBER.getValue(), 27, 0, 0, 0, 0, DateTimeManager.ROLLOVER);
    COLLISION =
        ZonedDateTime.of(2006, Month.JUNE.getValue(), 3, 0, 0, 0, 0, DateTimeManager.ROLLOVER);
    RONALD_PHASE = -1;
    GRIMACE_PHASE = -1;
    HAMBURGLAR_POSITION = -1;
    HOLIDAY_OVERRIDE = null;
    guessPhaseStep();
  }

  // static final array of status effect day predictions
  // within the KoL lunar calendar.

  private static final String[] STAT_EFFECT = {
    "Moxie bonus today and yesterday.",
    "3 days until Mysticism.",
    "2 days until Mysticism.",
    "Mysticism bonus tomorrow (not today).",
    "Mysticism bonus today (not tomorrow).",
    "3 days until Muscle.",
    "2 days until Muscle.",
    "Muscle bonus tomorrow (not today).",
    "Muscle bonus today and tomorrow.",
    "Muscle bonus today and yesterday.",
    "2 days until Mysticism.",
    "Mysticism bonus tomorrow (not today).",
    "Mysticism bonus today (not tomorrow).",
    "2 days until Moxie.",
    "Moxie bonus tomorrow (not today).",
    "Moxie bonus today and tomorrow."
  };

  // static final array of month names, as they exist within
  // the KoL calendar.

  private static final String[] MONTH_NAMES = {
    "",
    "Jarlsuary",
    "Frankruary",
    "Starch",
    "April",
    "Martinus",
    "Bill",
    "Bor",
    "Petember",
    "Carlvember",
    "Porktober",
    "Boozember",
    "Dougtember"
  };

  // static final array of HOLIDAYS.  This holiday is filled with the
  // name of the holiday which occurs on the given KoL month and
  // given KoL day.

  private static final String[][] HOLIDAYS = new String[13][9];

  static {
    for (int i = 0; i < 13; ++i) {
      for (int j = 0; j < 9; ++j) {
        HolidayDatabase.HOLIDAYS[i][j] = null;
      }
    }

    // Initialize all the known HOLIDAYS here so that
    // they can be used in later initializers.

    HolidayDatabase.HOLIDAYS[1][1] = "Festival of Jarlsberg"; // Jarlsuary 1
    HolidayDatabase.HOLIDAYS[2][4] = "Valentine's Day"; // Frankuary 4
    HolidayDatabase.HOLIDAYS[3][3] = "St. Sneaky Pete's Day"; // Starch 3
    HolidayDatabase.HOLIDAYS[4][2] = "Oyster Egg Day"; // April 2
    HolidayDatabase.HOLIDAYS[5][2] = "El Dia De Los Muertos Borrachos"; // Martinus 2
    HolidayDatabase.HOLIDAYS[6][3] = "Generic Summer Holiday"; // Bill 3
    HolidayDatabase.HOLIDAYS[7][4] = "Dependence Day"; // Bor 4
    HolidayDatabase.HOLIDAYS[8][4] = "Arrrbor Day"; // Petember 4
    HolidayDatabase.HOLIDAYS[9][6] = "Lab&oacute;r Day"; // Carlvember 6
    HolidayDatabase.HOLIDAYS[10][8] = "Halloween"; // Porktober 8
    HolidayDatabase.HOLIDAYS[11][7] = "Feast of Boris"; // Boozember 7
    HolidayDatabase.HOLIDAYS[12][4] = "Yuletide"; // Dougtember
  }

  private HolidayDatabase() {}

  public static void guessPhaseStep() {
    try {
      int calendarDay = HolidayDatabase.getDayInKoLYear();
      int phaseStep = (calendarDay + 16) % 16;

      HolidayDatabase.RONALD_PHASE = phaseStep % 8;
      HolidayDatabase.GRIMACE_PHASE = phaseStep / 2;
      HolidayDatabase.HAMBURGLAR_POSITION =
          HolidayDatabase.getHamburglarPosition(DateTimeManager.getRolloverDateTime());
    } catch (Exception e) {
      // This should not happen. Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  public static void setMoonPhases(final int ronaldPhase, final int grimacePhase) {
    HolidayDatabase.guessPhaseStep();
    int oldStep = HolidayDatabase.getPhaseStep();

    HolidayDatabase.RONALD_PHASE = ronaldPhase;
    HolidayDatabase.GRIMACE_PHASE = grimacePhase;

    int newStep = HolidayDatabase.getPhaseStep();

    if (oldStep != newStep) {
      // There are several reasons why our calculation of the KoL
      // moon phases might disagree with KoL's

      // - We assume KoL dates change at midnight of a calendar
      //   day. They do not. They change at KoL Rollover, which is
      //   before midnight in Arizona.
      //
      // - If you are in a different timezone than the KoL servers,
      //   Rollover happens at different times. Even if you are in
      //   the same timezone as Arizona, you may observe DST, and KoL
      //   does not.
      //
      // - The local computer clock may be off

      // If we ignore, for the moment, the last case, KoLmafia's idea
      // of the KoL date could be one off from KoL's idea:
      //
      // - If Rollover is finished but it is not yet midnight in this
      //   timezone, KoL will be one day ahead
      //
      // - If it is after midnight in this timezone, but Rollover has
      //   not occurred for KoL, KoL will be one day behind.

      // Therefore, assuming the local computer clock is correct, We
      // could be one day out of phase in either direction.

      // Unfortunately, a computer's clock can be arbitrarily
      // incorrect. We can accomodate that if we can, somehow, synch
      // up with the date used by KoL itself. We have two options:

      // We can easily see KoL's moon phases on the top menu. These
      // go through a 16-day cycle. If the local clock is 7 or fewer
      // days off, we can assume that our relative error to KoL is
      // from -8 to +7.

      // We can see KoL's actual month and day from the chatlaunch
      // frame. That goes through a 96-day cycle, which would allow
      // us to correct a phase error from -48 to +47.

      // For now, since we already read the moon phases, we'll do the
      // former, and assume that the phase error is between -8 days
      // and +7 days.

      int phaseError = oldStep - newStep;

      if (phaseError > 7) {
        phaseError -= 16;
      }

      String message =
          "Old phase = " + oldStep + " new phase = " + newStep + " phase error = " + phaseError;
      RequestLogger.printLine(message);

      // Adjust the new year by the appropriate number of days.
      HolidayDatabase.NEWYEAR = HolidayDatabase.NEWYEAR.plusDays(phaseError);
      HolidayDatabase.BOUNDARY = HolidayDatabase.BOUNDARY.plusDays(phaseError);
      HolidayDatabase.COLLISION = HolidayDatabase.COLLISION.plusDays(phaseError);
    }

    HolidayDatabase.HAMBURGLAR_POSITION =
        HolidayDatabase.getHamburglarPosition(DateTimeManager.getRolloverDateTime());
  }

  public static int getRonaldPhase() {
    return HolidayDatabase.RONALD_PHASE + 1;
  }

  public static int getGrimacePhase() {
    return HolidayDatabase.GRIMACE_PHASE + 1;
  }

  public static int getHamburglarPosition(final ZonedDateTime dateTime) {
    if (dateTime.isBefore(COLLISION)) {
      return -1;
    }

    var days = Duration.between(HolidayDatabase.COLLISION, dateTime).toDays();
    return (int) ((days * 2 % 11 + 11) % 11);
  }

  /**
   * Method to return which phase of the moon is currently appearing over the Kingdom of Loathing,
   * as a string.
   *
   * @return The current phase of Ronald
   */
  public static String getRonaldPhaseAsString() {
    return HolidayDatabase.getPhaseName(HolidayDatabase.RONALD_PHASE);
  }

  /**
   * Method to return which phase of the moon is currently appearing over the Kingdom of Loathing,
   * as a string.
   *
   * @return The current phase of Ronald
   */
  public static String getGrimacePhaseAsString() {
    return HolidayDatabase.getPhaseName(HolidayDatabase.GRIMACE_PHASE);
  }

  public static String getPhaseName(final int phase) {
    return switch (phase) {
      case 0 -> "new moon";
      case 1 -> "waxing crescent";
      case 2 -> "first quarter";
      case 3 -> "waxing gibbous";
      case 4 -> "full moon";
      case 5 -> "waning gibbous";
      case 6 -> "third quarter";
      case 7 -> "waning crescent";
      default -> "unknown";
    };
  }

  public static String getHamburglarPositionAsString() {
    return HolidayDatabase.getHamburglarPositionName(HolidayDatabase.HAMBURGLAR_POSITION);
  }

  public static String getHamburglarPositionName(final int phase) {
    return switch (phase) {
      case 0 -> "in front of Grimace, L side";
      case 1 -> "in front of Grimace, R side";
      case 2 -> "far right";
      case 3 -> "behind Grimace";
      case 4 -> "in back, near Grimace";
      case 5 -> "in back, near Ronald";
      case 6 -> "behind Ronald";
      case 7 -> "far left";
      case 8 -> "in front of Ronald, L side";
      case 9 -> "in front of Ronald, R side";
      case 10 -> "front center";
      default -> "unknown";
    };
  }

  /**
   * Returns the moon effect applicable today, or the amount of time until the next moon effect
   * becomes applicable if today is not a moon effect day.
   */
  public static String getMoonEffect() {
    return HolidayDatabase.getMoonEffect(
        HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE);
  }

  /**
   * Returns the moon effect applicable at the given phase step, or the amount of time until the
   * next moon effect, given the phase value.
   */
  public static String getMoonEffect(final int ronaldPhase, final int grimacePhase) {
    int phaseStep = HolidayDatabase.getPhaseStep(ronaldPhase, grimacePhase);
    return phaseStep == -1
        ? "Could not determine moon phase."
        : HolidayDatabase.STAT_EFFECT[phaseStep];
  }

  public static int getRonaldMoonlight(final int ronaldPhase) {
    return ronaldPhase > 4 ? 8 - ronaldPhase : ronaldPhase;
  }

  public static int getGrimaceMoonlight(final int grimacePhase) {
    return grimacePhase > 4 ? 8 - grimacePhase : grimacePhase;
  }

  /**
   * Returns the "phase step" currently recognized by the KoL calendar. This corresponds to the day
   * within the KoL lunar calendar, which has a cycle of 16 days.
   */
  public static int getPhaseStep() {
    return HolidayDatabase.getPhaseStep(
        HolidayDatabase.RONALD_PHASE, HolidayDatabase.GRIMACE_PHASE);
  }

  /**
   * Returns the "phase step" currently recognized by the KoL calendar, corresponding to the given
   * phases. This corresponds to the day within the KoL lunar calendar, which has a cycle of 16
   * days.
   */
  public static int getPhaseStep(final int ronaldPhase, final int grimacePhase) {
    return grimacePhase >= 4 ? 8 + ronaldPhase : ronaldPhase;
  }

  /** Returns whether or not the grue will fight during the given moon phases. */
  public static boolean getGrueEffect(
      final int ronaldPhase, final int grimacePhase, final int hamburglarPosition) {
    return HolidayDatabase.getMoonlight(ronaldPhase, grimacePhase, hamburglarPosition) < 5;
  }

  /** Returns the effect percentage of Blood of the Wereseal for today. */
  public static double getBloodEffect() {
    return HolidayDatabase.getBloodEffect(
        HolidayDatabase.RONALD_PHASE,
        HolidayDatabase.GRIMACE_PHASE,
        HolidayDatabase.HAMBURGLAR_POSITION);
  }

  /** Returns the effect percentage of Blood of the Wereseal for the given moon phase. */
  public static double getBloodEffect(
      final int ronaldPhase, final int grimacePhase, final int hamburglarPosition) {
    // Yendor says: "I have 2538 base Muscle; the effect gives +1597, or 62.92%. So the percentage
    // is not being rounded."
    return 10.0
        + 20.0
            * Math.sqrt(
                HolidayDatabase.getMoonlight(ronaldPhase, grimacePhase, hamburglarPosition));
  }

  /**
   * Returns the effect percentage (as a whole number integer) of the Talisman of Baio for the given
   * moon phases.
   */
  public static int getBaioEffect(
      final int ronaldPhase, final int grimacePhase, final int hamburglarPosition) {
    return HolidayDatabase.getMoonlight(ronaldPhase, grimacePhase, hamburglarPosition) * 10;
  }

  public static int getGrimaciteEffect() {
    return HolidayDatabase.getGrimaciteEffect(
        HolidayDatabase.RONALD_PHASE,
        HolidayDatabase.GRIMACE_PHASE,
        HolidayDatabase.HAMBURGLAR_POSITION);
  }

  public static int getGrimaciteEffect(
      final int ronaldPhase, final int grimacePhase, final int hamburglarPosition) {
    int grimaceEffect =
        4
            - HolidayDatabase.getGrimaceMoonlight(grimacePhase)
            + HolidayDatabase.getHamburglarDarkness(ronaldPhase, grimacePhase, hamburglarPosition);
    return grimaceEffect * 10;
  }

  /** Returns the effect of the Jekyllin for the given moon phases */
  public static String getJekyllinEffect(
      final int ronaldPhase, final int grimacePhase, final int hamburglarPosition) {
    int moonlight = HolidayDatabase.getMoonlight(ronaldPhase, grimacePhase, hamburglarPosition);
    return "+" + (9 - moonlight) + " stats, " + (15 + moonlight * 5) + "% items";
  }

  /** Utility method which determines the moonlight available, given the current moon phases. */
  public static int getMoonlight() {
    return HolidayDatabase.getMoonlight(
        HolidayDatabase.RONALD_PHASE,
        HolidayDatabase.GRIMACE_PHASE,
        HolidayDatabase.HAMBURGLAR_POSITION);
  }

  /** Utility method which determines the moonlight available, given the moon phases as stated. */
  private static int getMoonlight(
      final int ronaldPhase, final int grimacePhase, final int hamburglarPosition) {
    int ronaldLight = HolidayDatabase.getRonaldMoonlight(ronaldPhase);
    int grimaceLight = HolidayDatabase.getGrimaceMoonlight(grimacePhase);
    int hamburglarLight =
        HolidayDatabase.getHamburglarLight(ronaldPhase, grimacePhase, hamburglarPosition);
    return ronaldLight + grimaceLight + hamburglarLight;
  }

  public static int getHamburglarLight(
      final int ronaldPhase, final int grimacePhase, final int hamburglarPosition) {
    //         6    5    4    3
    //
    //       /---\          /---\
    //   7   | R |          | G |   2
    //       \___/          \___/
    //
    //       8   9    10    0   1

    switch (hamburglarPosition) {
      case 0:
        if (grimacePhase > 0 && grimacePhase < 5) {
          return -1;
        }
        return 1;

      case 1:
        if (grimacePhase < 4) {
          return 1;
        }
        return -1;

      case 2:
        if (grimacePhase > 3) {
          return 1;
        }
        return 0;

      case 4:
        if (grimacePhase > 0 && grimacePhase < 5) {
          return 1;
        }
        return 0;

      case 5:
        if (ronaldPhase > 3) {
          return 1;
        }
        return 0;

      case 7:
        if (ronaldPhase > 0 && ronaldPhase < 5) {
          return 1;
        }
        return 0;

      case 8:
        if (ronaldPhase > 0 && ronaldPhase < 5) {
          return -1;
        }
        return 1;

      case 9:
        if (ronaldPhase < 4) {
          return 1;
        }
        return -1;

      case 10:
        int totalEffect = 0;
        if (ronaldPhase > 3) {
          ++totalEffect;
        }
        if (grimacePhase > 0 && grimacePhase < 5) {
          ++totalEffect;
        }
        return totalEffect;

      default:
        return 0;
    }
  }

  public static int getHamburglarDarkness(
      final int ronaldPhase, final int grimacePhase, final int hamburglarPosition) {
    //         6    5    4    3
    //
    //       /---\          /---\
    //   7   | R |          | G |   2
    //       \___/          \___/
    //
    //       8   9    10    0   1

    switch (hamburglarPosition) {
      case 0:
        if (grimacePhase > 0 && grimacePhase < 5) {
          return 1;
        }
        return -1;

      case 1:
        if (grimacePhase < 4) {
          return -1;
        }
        return 1;

      case 2:
        if (grimacePhase > 3) {
          return 0;
        }
        return 1;

      case 4:
        if (grimacePhase > 0 && grimacePhase < 5) {
          return 0;
        }
        return 1;

      case 5:
        if (ronaldPhase > 3) {
          return 0;
        }
        return 1;

      case 7:
        if (ronaldPhase > 0 && ronaldPhase < 5) {
          return 0;
        }
        return 1;

      case 8:
        if (ronaldPhase > 0 && ronaldPhase < 5) {
          return 1;
        }
        return 0;

      case 9:
        if (ronaldPhase < 4) {
          return 0;
        }
        return 1;

      case 10:
        if (ronaldPhase > 3) {
          return 0;
        }
        if (grimacePhase > 0 && grimacePhase < 5) {
          return 0;
        }
        return 1;

      default:
        return 0;
    }
  }

  /**
   * Computes the difference in days based on the given millisecond counts since January 1, 1970.
   */
  public static long getDayDifference(final ZonedDateTime dateTime) {
    var days = ChronoUnit.DAYS.between(HolidayDatabase.NEWYEAR, dateTime);

    if (dateTime.isAfter(HolidayDatabase.BOUNDARY)) {
      days -= 1;
    }

    return days;
  }

  public static long getDayDifference() {
    return getDayDifference(DateTimeManager.getRolloverDateTime());
  }

  public static int getDayInKoLYear(final ZonedDateTime timeDate) {
    int dayDifference = (int) Math.floor(getDayDifference(timeDate));
    return (dayDifference + 96) % 96;
  }

  public static int getDayInKoLYear() {
    return getDayInKoLYear(DateTimeManager.getRolloverDateTime());
  }

  public static int getTimeInKoLDay(final ZonedDateTime timeDate) {
    return (int)
        ChronoUnit.MILLIS.between(timeDate.withHour(0).withMinute(0).withSecond(0), timeDate);
  }

  public static int getTimeInKoLDay() {
    return getTimeInKoLDay(DateTimeManager.getRolloverDateTime());
  }

  /**
   * Utility method which calculates which day of the KoL calendar you're currently on, based on the
   * number of milliseconds since January 1, 1970.
   */
  public static String getCalendarDayAsString(final int day) {
    int[] calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray(day);
    return HolidayDatabase.MONTH_NAMES[calendarDayAsArray[0]] + " " + calendarDayAsArray[1];
  }

  public static String getCalendarDayAsString(final ZonedDateTime dateTime) {
    return getCalendarDayAsString(HolidayDatabase.getDayInKoLYear(dateTime));
  }

  public static String getCalendarDayAsString() {
    return getCalendarDayAsString(DateTimeManager.getRolloverDateTime());
  }

  /** Utility method which decomposes a given calendar day into its actual calendar components. */
  private static int[] convertCalendarDayToArray(final int calendarDay) {
    return new int[] {calendarDay / 8 % 12 + 1, calendarDay % 8 + 1};
  }

  /**
   * Utility method which returns the given day count as an easily-understood string (today,
   * tomorrow) instead of just "x days".
   */
  public static String getDayCountAsString(final int dayCount) {
    return switch (dayCount) {
      case 0 -> "today";
      case 1 -> "tomorrow";
      default -> dayCount + " days";
    };
  }

  public static String getDayCountAsString(final int dayCount, final String event) {
    var dayCountString = getDayCountAsString(dayCount);
    if (dayCount > 1) return dayCountString + " until " + event;
    return event + " " + dayCountString;
  }

  /** Returns the KoL calendar month associated with the given date in the real world. */
  public static int getCalendarMonth(final ZonedDateTime dateTime) {
    return HolidayDatabase.convertCalendarDayToArray(HolidayDatabase.getDayInKoLYear(dateTime))[0];
  }

  /** Returns whether the given day is a game holiday. */
  public static boolean isGameHoliday(final ZonedDateTime dateTime) {
    return HolidayDatabase.getGameHoliday(dateTime) != null;
  }

  public static boolean isRealLifeHoliday(final ZonedDateTime dateTime) {
    return HolidayDatabase.getRealLifeHoliday(dateTime) != null;
  }

  /** Returns whether the given day's most important attribute is being a muscle day. */
  public static boolean isMuscleDay(final ZonedDateTime dateTime) {
    return getStatDay(dateTime) == Stat.MUSCLE;
  }

  /** Returns whether the given day's most important attribute is being a mysticality day. */
  public static boolean isMysticalityDay(final ZonedDateTime dateTime) {
    return getStatDay(dateTime) == Stat.MYSTICALITY;
  }

  /** Returns whether the given day's most important attribute is being a moxie day. */
  public static boolean isMoxieDay(final ZonedDateTime dateTime) {
    return getStatDay(dateTime) == Stat.MOXIE;
  }

  public static Stat getStatDay() {
    return getStatDay(DateTimeManager.getRolloverDateTime());
  }

  /** Returns whether the given day is a stat day and if so what stat. */
  public static Stat getStatDay(final ZonedDateTime dateTime) {
    return switch (HolidayDatabase.getDayInKoLYear(dateTime) % 16) {
      case 0, 15 -> Stat.MOXIE;
      case 4, 12 -> Stat.MYSTICALITY;
      case 8, 9 -> Stat.MUSCLE;
      default -> Stat.NONE;
    };
  }

  /** Returns a complete list of all holiday predictions for the given day, as an array. */
  public static String[] getHolidayPredictions(final ZonedDateTime dateTime) {
    List<HolidayEntry> holidayList = new ArrayList<>();
    int currentCalendarDay = HolidayDatabase.getDayInKoLYear(dateTime);

    int[] calendarDayAsArray;

    for (int i = 0; i < 96; ++i) {
      calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray(i);
      String holiday = HolidayDatabase.HOLIDAYS[calendarDayAsArray[0]][calendarDayAsArray[1]];

      if (holiday != null) {
        int currentEstimate = (i - currentCalendarDay + 96) % 96;
        var holidayTester = dateTime;

        for (int j = 0; j < currentEstimate; ++j) {
          var testResult = HolidayDatabase.getRealLifeHoliday(holidayTester);

          if (testResult != null && testResult.equals(holiday)) {
            currentEstimate = j;
          }

          holidayTester = holidayTester.plusDays(1);
        }

        holidayList.add(
            new HolidayEntry(
                currentEstimate,
                HolidayDatabase.HOLIDAYS[calendarDayAsArray[0]][calendarDayAsArray[1]]));
      }
    }

    // If today is a real life holiday that doesn't map to a KoL
    // holiday, list it here.

    if (HolidayDatabase.getGameHoliday(dateTime) == null) {
      String holiday = HolidayDatabase.getRealLifeOnlyHoliday(dateTime);
      if (holiday != null) {
        holidayList.add(new HolidayEntry(0, holiday));
      }
    }

    Collections.sort(holidayList);
    HolidayEntry[] holidayArray = new HolidayEntry[holidayList.size()];
    holidayList.toArray(holidayArray);
    String[] predictionsArray = new String[holidayList.size()];
    for (int i = 0; i < holidayArray.length; ++i) {
      predictionsArray[i] = holidayArray[i].toString();
    }

    return predictionsArray;
  }

  private record HolidayEntry(int offset, String name) implements Comparable<HolidayEntry> {

    @Override
    public int compareTo(final HolidayEntry o) {
      if (o == null) {
        return -1;
      }

      if (this.offset != o.offset) {
        return this.offset - o.offset;
      }

      return this.name.compareTo(o.name);
    }

    @Override
    public boolean equals(final Object o) {
      if (!(o instanceof HolidayEntry other)) {
        return false;
      }

      return (this.offset == other.offset) && this.name.equals(other.name);
    }

    @Override
    public String toString() {
      return this.name + ": " + getDayCountAsString(offset);
    }
  }

  public static String getGameHolidayInDays(final ZonedDateTime dateTime, final int days) {
    int calendarDay = getDayInKoLYear(dateTime);
    return getGameHoliday((calendarDay + days) % 96);
  }

  public static void setHoliday(final String holiday) {
    HOLIDAY_OVERRIDE = holiday.isBlank() ? null : holiday;
  }

  public static List<String> getHolidays() {
    return getHolidays(DateTimeManager.getRolloverDateTime());
  }

  public static List<String> getHolidays(final boolean replaceWithSpecial) {
    return getHolidays(DateTimeManager.getRolloverDateTime(), replaceWithSpecial);
  }

  public static List<String> getHolidays(final ZonedDateTime dateTime) {
    return getHolidays(dateTime, true);
  }

  /** Returns the KoL holiday associated with the given date in the real world. */
  public static List<String> getHolidays(
      final ZonedDateTime dateTime, final boolean replaceWithSpecial) {
    var holidays = new ArrayList<String>();

    String gameHoliday = getGameHoliday(dateTime);
    if (gameHoliday != null) holidays.add(gameHoliday);

    String realHoliday = getRealLifeHoliday(dateTime);
    if (realHoliday != null) holidays.add(realHoliday);

    if (replaceWithSpecial) {
      if (holidays.contains("St. Sneaky Pete's Day") && holidays.contains("Feast of Boris")) {
        holidays.clear();
        holidays.add("Drunksgiving");
      } else if (holidays.contains("Feast of Boris")
          && holidays.contains("El Dia De Los Muertos Borrachos")) {
        holidays.clear();
        holidays.add("El Dia De Los Muertos Borrachos y Agradecido");
      }
    }

    if (HOLIDAY_OVERRIDE != null) holidays.add(HOLIDAY_OVERRIDE);

    return holidays;
  }

  public static String getHoliday() {
    return getHoliday(DateTimeManager.getRolloverDateTime());
  }

  public static String getHoliday(final ZonedDateTime dateTime) {
    return String.join(" / ", getHolidays(dateTime));
  }

  public static List<String> getEvents() {
    return getEvents(DateTimeManager.getRolloverDateTime());
  }

  public static List<String> getEvents(final ZonedDateTime dateTime) {
    var list = new ArrayList<>(getHolidays(dateTime));

    // Include this pseudo-event because the day before Labor Day essentially has +10 rollover
    // adventures
    if ("Lab&oacute;r Day".equals(getGameHolidayInDays(dateTime, 1))) {
      list.add("Lab&oacute;r Day Eve");
    }

    var statDay = getStatDay(dateTime);
    if (statDay != Stat.NONE) {
      list.add(statDay.toString() + " Day");
    }

    return list;
  }

  public static String getHolidaySummary() {
    return getHolidaySummary(DateTimeManager.getRolloverDateTime());
  }

  public static String getHolidaySummary(final ZonedDateTime dateTime) {
    var holiday = getHoliday(dateTime);

    if (!holiday.isBlank()) return getDayCountAsString(0, holiday);

    for (int i = 0; i < 96; ++i) {
      holiday = getGameHolidayInDays(dateTime, i);

      if (holiday != null) {
        return getDayCountAsString(i, holiday);
      }
    }

    return "";
  }

  public static String getGameHoliday(final int calendarDay) {
    int[] calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray(calendarDay);
    return HolidayDatabase.HOLIDAYS[calendarDayAsArray[0]][calendarDayAsArray[1]];
  }

  public static String getGameHoliday(final ZonedDateTime dateTime) {
    return HolidayDatabase.getGameHoliday(HolidayDatabase.getDayInKoLYear(dateTime));
  }

  private static Year cachedYear = null;
  private static MonthDay easter = null;
  private static MonthDay thanksgiving = null;

  protected static MonthDay getEaster(final Year year) {
    // Apparently Easter isn't the second Sunday in April,
    // it actually depends on the occurrence of the first
    // ecclesiastical full moon after the Spring Equinox
    // (http://aa.usno.navy.mil/faq/docs/easter.html)
    int y = year.getValue();
    int c = y / 100;
    int n = y - 19 * (y / 19);
    int k = (c - 17) / 25;
    int i = c - c / 4 - (c - k) / 3 + 19 * n + 15;
    i = i - 30 * (i / 30);
    i = i - (i / 28) * (1 - (i / 28) * (29 / (i + 1)) * ((21 - n) / 11));
    int j = y + y / 4 + i + 2 - c + c / 4;
    j = j - 7 * (j / 7);
    int l = i - j;
    int m = 3 + (l + 40) / 44;
    int d = l + 28 - 31 * (m / 4);

    return MonthDay.of(m, d);
  }

  protected static MonthDay getThanksgiving(final Year year) {
    var firstOfNovember = year.atMonthDay(MonthDay.of(Month.NOVEMBER, 1));
    var firstThursday = Math.floorMod(3 - firstOfNovember.getDayOfWeek().ordinal(), 7);
    return MonthDay.of(Month.NOVEMBER, firstThursday + 22);
  }

  public static String getRealLifeHoliday(final TemporalAccessor date) {
    var currentYear = Year.from(date);

    if (currentYear != HolidayDatabase.cachedYear) {
      HolidayDatabase.cachedYear = currentYear;

      // Calculate holidays for the in-game timezone (days which start at rollover)
      HolidayDatabase.easter = getEaster(currentYear);
      HolidayDatabase.thanksgiving = getThanksgiving(currentYear);
    }

    // Real-life holiday list borrowed from JRSiebz's
    // variables for HOLIDAYS on the KoL JS Almanac
    // (http://home.cinci.rr.com/jrsiebz/KoL/almanac.html)
    var monthDay = MonthDay.from(date);

    if (monthDay.equals(MonthDay.of(Month.JANUARY, 1))) {
      return "Festival of Jarlsberg";
    }

    if (monthDay.equals(MonthDay.of(Month.FEBRUARY, 14))) {
      return "Valentine's Day";
    }

    if (monthDay.equals(MonthDay.of(Month.MARCH, 17))) {
      return "St. Sneaky Pete's Day";
    }

    if (monthDay.equals(MonthDay.of(Month.JULY, 4))) {
      return "Dependence Day";
    }

    if (monthDay.equals(HolidayDatabase.easter)) {
      return "Oyster Egg Day";
    }

    if (monthDay.equals(HolidayDatabase.thanksgiving)) {
      return "Feast of Boris";
    }

    if (monthDay.equals(MonthDay.of(Month.OCTOBER, 31))) {
      return "Halloween";
    }

    return HolidayDatabase.getRealLifeOnlyHoliday(date);
  }

  private static String getRealLifeOnlyHoliday(final TemporalAccessor date) {
    var monthDay = MonthDay.from(date);

    if (monthDay.equals(MonthDay.of(Month.FEBRUARY, 2))) {
      return "Groundhog Day";
    }

    if (monthDay.equals(MonthDay.of(Month.APRIL, 1))) {
      return "April Fool's Day";
    }

    if (monthDay.equals(MonthDay.of(Month.SEPTEMBER, 19))) {
      return "Talk Like a Pirate Day";
    }

    if (monthDay.equals(MonthDay.of(Month.DECEMBER, 25))) {
      return "Crimbo";
    }

    if (monthDay.equals(MonthDay.of(Month.OCTOBER, 22))) {
      return "Holatuwol's Birthday";
    }

    if (monthDay.equals(MonthDay.of(Month.SEPTEMBER, 23))) {
      return "Veracity's Birthday";
    }

    if (monthDay.equals(MonthDay.of(Month.FEBRUARY, 17))) {
      return "Gausie's Birthday";
    }

    return null;
  }

  /**
   * Is it Monday in Arizona
   *
   * @return Returns true if it is currently Monday in Arizona
   */
  public static boolean isMonday() {
    return isMonday(DateTimeManager.getArizonaDateTime());
  }

  /**
   * Is the given date and time a Monday in Arizona
   *
   * @return Returns true if it the given date and time is a Monday in Arizona
   */
  public static boolean isMonday(ZonedDateTime dateTime) {
    return dateTime.getDayOfWeek() == DayOfWeek.MONDAY;
  }

  /**
   * Is it December in Arizona
   *
   * @return Returns true if it is currently December in Arizona
   */
  public static boolean isDecember() {
    return isDecember(DateTimeManager.getArizonaDateTime());
  }

  /**
   * Is the given date and time in December in Arizona
   *
   * @return Returns true if the given date and time is in December in Arizona
   */
  public static boolean isDecember(ZonedDateTime dateTime) {
    return dateTime.getMonth() == Month.DECEMBER;
  }

  /**
   * Is it Saturday in Arizona
   *
   * @return Returns true if it is currently Saturday in Arizona
   */
  public static boolean isSaturday() {
    return isSaturday(DateTimeManager.getArizonaDateTime());
  }

  /**
   * Is it Saturday in Arizona
   *
   * @return Returns true if the given date and time is on Saturday in Arizona
   */
  public static boolean isSaturday(ZonedDateTime dateTime) {
    return dateTime.getDayOfWeek() == DayOfWeek.SATURDAY;
  }

  public static boolean isFeastOfBorisLike() {
    var holidays = getHolidays(DateTimeManager.getRolloverDateTime(), false);
    return holidays.contains("Feast of Boris");
  }

  public static void addPredictionHTML(
      final StringBuffer displayHTML, final ZonedDateTime dateTime, final int phaseStep) {
    HolidayDatabase.addPredictionHTML(displayHTML, dateTime, phaseStep, true);
  }

  public static void addPredictionHTML(
      final StringBuffer displayHTML,
      final ZonedDateTime today,
      final int phaseStep,
      final boolean addStatDays) {
    // Next display the upcoming stat days.

    if (addStatDays) {
      displayHTML.append("<nobr><b>Muscle Day</b>:&nbsp;");
      displayHTML.append(
          HolidayDatabase.getDayCountAsString(
              Math.min((24 - phaseStep) % 16, (25 - phaseStep) % 16)));
      displayHTML.append("</nobr><br>");

      displayHTML.append("<nobr><b>Mysticality Day</b>:&nbsp;");
      displayHTML.append(
          HolidayDatabase.getDayCountAsString(
              Math.min((20 - phaseStep) % 16, (28 - phaseStep) % 16)));
      displayHTML.append("</nobr><br>");

      displayHTML.append("<nobr><b>Moxie Day</b>:&nbsp;");
      displayHTML.append(
          HolidayDatabase.getDayCountAsString(
              Math.min((16 - phaseStep) % 16, (31 - phaseStep) % 16)));
      displayHTML.append("</nobr><br>&nbsp;<br>");
    }

    // Next display the upcoming holidays.  This is done
    // through loop calculations in order to minimize the
    // amount of code done to handle individual holidays.

    String[] holidayPredictions = HolidayDatabase.getHolidayPredictions(today);
    for (String holidayPrediction : holidayPredictions) {
      displayHTML.append("<nobr><b>");
      displayHTML.append(holidayPrediction.replaceAll(":", ":</b>&nbsp;"));
      displayHTML.append("</nobr><br>");
    }
  }
}
