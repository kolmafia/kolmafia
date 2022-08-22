package net.sourceforge.kolmafia.persistence;

import static net.sourceforge.kolmafia.KoLConstants.DAILY_DATETIME_FORMAT;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants.Stat;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HolidayDatabase {
  public static final ZoneId ROLLOVER = ZoneId.of("GMT-0330");
  public static final ZoneId ARIZONA = ZoneId.of("GMT-0700");

  // The epoch of the Kingdom of Loathing.
  private static final ZonedDateTime NEWYEAR =
      ZonedDateTime.of(2005, Month.SEPTEMBER.getValue(), 17, 0, 0, 0, 0, ROLLOVER);

  // The date of White Wednesday, which throws everything off by a day
  private static final ZonedDateTime BOUNDARY =
      ZonedDateTime.of(2005, Month.OCTOBER.getValue(), 27, 0, 0, 0, 0, ROLLOVER);

  // The day the thing crashed into Grimace
  private static final ZonedDateTime COLLISION =
      ZonedDateTime.of(2006, Month.JUNE.getValue(), 3, 0, 0, 0, 0, ROLLOVER);

  private static int RONALD_PHASE = -1;
  private static int GRIMACE_PHASE = -1;
  private static int HAMBURGLAR_POSITION = -1;

  static {
    HolidayDatabase.guessPhaseStep();
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

  // static final array of when the special events in KoL occur, including
  // stat days, HOLIDAYS and all that jazz.  Values are false where
  // there is no special occasion, and true where there is.

  private static final int[] SPECIAL = new int[96];

  public static final int SP_NOTHING = 0;
  public static final int SP_HOLIDAY = 1;
  public static final int SP_MUSDAY = 2;
  public static final int SP_MYSDAY = 3;
  public static final int SP_MOXDAY = 4;

  static {
    // Assume there are no special days at all, and then
    // fill them in once they're encountered.

    for (int i = 0; i < 96; ++i) {
      HolidayDatabase.SPECIAL[i] = HolidayDatabase.SP_NOTHING;
    }

    // Muscle days occur every phase 8 and phase 9 on the
    // KoL calendar.

    for (int i = 8; i < 96; i += 16) {
      HolidayDatabase.SPECIAL[i] = HolidayDatabase.SP_MUSDAY;
    }
    for (int i = 9; i < 96; i += 16) {
      HolidayDatabase.SPECIAL[i] = HolidayDatabase.SP_MUSDAY;
    }

    // Mysticism days occur every phase 4 and phase 12 on the
    // KoL calendar.

    for (int i = 4; i < 96; i += 16) {
      HolidayDatabase.SPECIAL[i] = HolidayDatabase.SP_MYSDAY;
    }
    for (int i = 12; i < 96; i += 16) {
      HolidayDatabase.SPECIAL[i] = HolidayDatabase.SP_MYSDAY;
    }

    // Moxie days occur every phase 0 and phase 15 on the
    // KoL calendar.

    for (int i = 0; i < 96; i += 16) {
      HolidayDatabase.SPECIAL[i] = HolidayDatabase.SP_MOXDAY;
    }
    for (int i = 15; i < 96; i += 16) {
      HolidayDatabase.SPECIAL[i] = HolidayDatabase.SP_MOXDAY;
    }

    // Next, fill in the HOLIDAYS.  These are manually
    // computed based on the recurring day in the year
    // at which these occur.

    for (int i = 0; i < 13; ++i) {
      for (int j = 0; j < 9; ++j) {
        if (HolidayDatabase.HOLIDAYS[i][j] != null) {
          HolidayDatabase.SPECIAL[8 * i + j - 9] = HolidayDatabase.SP_HOLIDAY;
        }
      }
    }
  }

  private HolidayDatabase() {}

  public static void guessPhaseStep() {
    try {
      int calendarDay = HolidayDatabase.getDayInKoLYear();
      int phaseStep = (calendarDay + 16) % 16;

      HolidayDatabase.RONALD_PHASE = phaseStep % 8;
      HolidayDatabase.GRIMACE_PHASE = phaseStep / 2;
      HolidayDatabase.HAMBURGLAR_POSITION =
          HolidayDatabase.getHamburglarPosition(getRolloverDateTime());
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

      HolidayDatabase.NEWYEAR.plusDays(phaseError);
      HolidayDatabase.BOUNDARY.plusDays(phaseError);
      HolidayDatabase.COLLISION.plusDays(phaseError);
    }
    HolidayDatabase.HAMBURGLAR_POSITION =
        HolidayDatabase.getHamburglarPosition(getRolloverDateTime());
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

  public static int getDayInKoLYear(final ZonedDateTime timeDate) {
    int dayDifference = (int) Math.floor(getDayDifference(timeDate));
    return (dayDifference + 96) % 96;
  }

  public static int getDayInKoLYear() {
    return getDayInKoLYear(getRolloverDateTime());
  }

  public static int getTimeInKoLDay(final ZonedDateTime timeDate) {
    return (int)
        ChronoUnit.MILLIS.between(timeDate.withHour(0).withMinute(0).withSecond(0), timeDate);
  }

  public static int getTimeInKoLDay() {
    return getTimeInKoLDay(getRolloverDateTime());
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
    return getCalendarDayAsString(getRolloverDateTime());
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
    return dayCount == 0 ? "today" : dayCount == 1 ? "tomorrow" : dayCount + " days";
  }

  /** Returns the KoL calendar month associated with the given date in the real world. */
  public static int getCalendarMonth(final ZonedDateTime dateTime) {
    return HolidayDatabase.convertCalendarDayToArray(HolidayDatabase.getDayInKoLYear(dateTime))[0];
  }

  /** Returns whether or not the given day's most important attribute is being a holiday. */
  public static boolean isHoliday(final ZonedDateTime dateTime) {
    return HolidayDatabase.SPECIAL[HolidayDatabase.getDayInKoLYear(dateTime)]
        == HolidayDatabase.SP_HOLIDAY;
  }

  public static boolean isRealLifeHoliday(final ZonedDateTime dateTime) {
    return HolidayDatabase.getRealLifeHoliday(dateTime) != null;
  }

  /**
   * Returns whether or not the given day's most important attribute is being a muscle day. Note
   * that this ranks behind being a holiday, so HOLIDAYS which are also stat days (Halloween and
   * Oyster Egg Day, for example), will not be recognized as "stat days" in this method.
   */
  public static boolean isMuscleDay(final ZonedDateTime dateTime) {
    return HolidayDatabase.SPECIAL[HolidayDatabase.getDayInKoLYear(dateTime)]
        == HolidayDatabase.SP_MUSDAY;
  }

  /**
   * Returns whether or not the given day's most important attribute is being a mysticality day.
   * Note that this ranks behind being a holiday, so HOLIDAYS which are also stat days (Halloween
   * and Oyster Egg Day, for example), will not be recognized as "stat days" in this method.
   */
  public static boolean isMysticalityDay(final ZonedDateTime dateTime) {
    return HolidayDatabase.SPECIAL[HolidayDatabase.getDayInKoLYear(dateTime)]
        == HolidayDatabase.SP_MYSDAY;
  }

  /**
   * Returns whether or not the given day's most important attribute is being a moxie day. Note that
   * this ranks behind being a holiday, so HOLIDAYS which are also stat days (Halloween and Oyster
   * Egg Day, for example), will not be recognized as "stat days" in this method.
   */
  public static boolean isMoxieDay(final ZonedDateTime dateTime) {
    return HolidayDatabase.SPECIAL[HolidayDatabase.getDayInKoLYear(dateTime)]
        == HolidayDatabase.SP_MOXDAY;
  }

  /**
   * Returns whether or not the given day's most important attribute is being a moxie day. Note that
   * this ranks behind being a holiday, so HOLIDAYS which are also stat days (Halloween and Oyster
   * Egg Day, for example), will not be recognized as "stat days" in this method.
   */
  public static Stat statDay(final ZonedDateTime dateTime) {
    return switch (HolidayDatabase.SPECIAL[HolidayDatabase.getDayInKoLYear(dateTime)]) {
      case HolidayDatabase.SP_MUSDAY -> Stat.MUSCLE;
      case HolidayDatabase.SP_MYSDAY -> Stat.MYSTICALITY;
      case HolidayDatabase.SP_MOXDAY -> Stat.MOXIE;
      default -> Stat.NONE;
    };
  }

  public static String currentStatDay() {
    return switch (statDay(getRolloverDateTime())) {
      case MUSCLE -> "Muscle Day";
      case MYSTICALITY -> "Mysticality Day";
      case MOXIE -> "Moxie Day";
      default -> "None";
    };
  }

  /** Returns a complete list of all holiday predictions for the given day, as an array. */
  public static String[] getHolidayPredictions(final ZonedDateTime dateTime) {
    List<HolidayEntry> holidayList = new ArrayList<>();
    int currentCalendarDay = HolidayDatabase.getDayInKoLYear(dateTime);

    int[] calendarDayAsArray;

    for (int i = 0; i < 96; ++i) {
      if (HolidayDatabase.SPECIAL[i] == HolidayDatabase.SP_HOLIDAY) {
        calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray(i);
        int currentEstimate = (i - currentCalendarDay + 96) % 96;

        String holiday = HolidayDatabase.HOLIDAYS[calendarDayAsArray[0]][calendarDayAsArray[1]];

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

    if (HolidayDatabase.SPECIAL[HolidayDatabase.getDayInKoLYear(dateTime)]
        != HolidayDatabase.SP_HOLIDAY) {
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

  /**
   * Gets the date and time in Arizona's timezone. Almost everything in the game uses the date and
   * time based on rollover, so if you're using this: double check that its the correct function.
   *
   * @return Date and time in the Arizona time zone
   */
  public static ZonedDateTime getArizonaDateTime() {
    return ZonedDateTime.now(ARIZONA);
  }

  public static ZonedDateTime getRolloverDateTime() {
    return ZonedDateTime.now(ROLLOVER);
  }

  public static String getHoliday() {
    return getHoliday(false);
  }

  public static String getHoliday(final boolean showPredictions) {
    return getHoliday(getRolloverDateTime(), showPredictions);
  }

  public static String getHoliday(final ZonedDateTime dateTime) {
    return getHoliday(dateTime, false);
  }

  /** Returns the KoL holiday associated with the given date in the real world. */
  public static String getHoliday(final ZonedDateTime dateTime, final boolean showPrediction) {
    String gameHoliday = getGameHoliday(dateTime);
    String realHoliday = getRealLifeHoliday(dateTime);

    if (showPrediction && realHoliday == null) {
      if (gameHoliday != null) {
        return gameHoliday + " today";
      }

      int calendarDay = getDayInKoLYear(dateTime);
      gameHoliday = getGameHoliday((calendarDay + 1) % 96);

      if (gameHoliday != null) {
        return gameHoliday + " tomorrow";
      }

      for (int i = 2; i < 96; ++i) {
        gameHoliday = HolidayDatabase.getGameHoliday((calendarDay + i) % 96);

        if (gameHoliday != null) {
          return HolidayDatabase.getDayCountAsString(i) + " until " + gameHoliday;
        }
      }

      return "";
    }

    if (gameHoliday == null && realHoliday == null) {
      return "";
    }

    if (gameHoliday == null) {
      return realHoliday;
    }

    if (realHoliday == null) {
      return gameHoliday;
    }

    String holiday = realHoliday + " / " + gameHoliday;
    if (holiday.equals("St. Sneaky Pete's Day / Feast of Boris")
        || holiday.equals("Feast of Boris / St. Sneaky Pete's Day")) {
      holiday = "Drunksgiving";
    }

    return holiday;
  }

  public static String getGameHoliday(final int calendarDay) {
    int[] calendarDayAsArray = HolidayDatabase.convertCalendarDayToArray(calendarDay);
    return HolidayDatabase.HOLIDAYS[calendarDayAsArray[0]][calendarDayAsArray[1]];
  }

  public static String getGameHoliday(final ZonedDateTime dateTime) {
    return HolidayDatabase.getGameHoliday(HolidayDatabase.getDayInKoLYear(dateTime));
  }

  private static String cachedYear = "";
  private static String easter = "";
  private static String thanksgiving = "";

  public static String getRealLifeHoliday(final ZonedDateTime dateTime) {
    return HolidayDatabase.getRealLifeHoliday(dateTime.format(DAILY_DATETIME_FORMAT));
  }

  public static String getRealLifeHoliday(final String stringDate) {
    String currentYear = stringDate.substring(0, 4);
    if (!currentYear.equals(HolidayDatabase.cachedYear)) {
      HolidayDatabase.cachedYear = currentYear;
      // Calculate holidays for the in-game timezone (days which start at rollover)

      // Apparently, Easter isn't the second Sunday in April;
      // it actually depends on the occurrence of the first
      // ecclesiastical full moon after the Spring Equinox
      // (http://aa.usno.navy.mil/faq/docs/easter.html)

      int y = StringUtilities.parseInt(currentYear);
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

      HolidayDatabase.easter =
          ZonedDateTime.of(y, m, d, 0, 0, 0, 0, ROLLOVER).format(DAILY_DATETIME_FORMAT);

      var dayOfTheWeek =
          ZonedDateTime.of(y, Month.NOVEMBER.getValue(), 1, 0, 0, 0, 0, ROLLOVER).getDayOfWeek();
      HolidayDatabase.thanksgiving =
          switch (dayOfTheWeek) {
            case FRIDAY -> "1128";
            case SATURDAY -> "1127";
            case SUNDAY -> "1126";
            case MONDAY -> "1125";
            case TUESDAY -> "1124";
            case WEDNESDAY -> "1123";
            case THURSDAY -> "1122";
          };
    }

    // Real-life holiday list borrowed from JRSiebz's
    // variables for HOLIDAYS on the KoL JS Almanac
    // (http://home.cinci.rr.com/jrsiebz/KoL/almanac.html)

    if (stringDate.endsWith("0101")) {
      return "Festival of Jarlsberg";
    }

    if (stringDate.endsWith("0214")) {
      return "Valentine's Day";
    }

    if (stringDate.endsWith("0317")) {
      return "St. Sneaky Pete's Day";
    }

    if (stringDate.endsWith("0704")) {
      return "Dependence Day";
    }

    if (stringDate.equals(HolidayDatabase.easter)) {
      return "Oyster Egg Day";
    }

    if (stringDate.endsWith(HolidayDatabase.thanksgiving)) {
      return "Feast of Boris";
    }

    if (stringDate.endsWith("1031")) {
      return "Halloween";
    }

    return HolidayDatabase.getRealLifeOnlyHoliday(stringDate);
  }

  private static String getRealLifeOnlyHoliday(final ZonedDateTime dateTime) {
    return HolidayDatabase.getRealLifeOnlyHoliday(dateTime.format(DAILY_DATETIME_FORMAT));
  }

  private static String getRealLifeOnlyHoliday(final String stringDate) {
    if (stringDate.endsWith("0202")) {
      return "Groundhog Day";
    }

    if (stringDate.endsWith("0401")) {
      return "April Fool's Day";
    }

    if (stringDate.endsWith("0919")) {
      return "Talk Like a Pirate Day";
    }

    if (stringDate.endsWith("1225")) {
      return "Crimbo";
    }

    if (stringDate.endsWith("1022")) {
      return "Holatuwol's Birthday";
    }

    if (stringDate.endsWith("0923")) {
      return "Veracity's Birthday";
    }

    if (stringDate.endsWith("0217")) {
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
    return isMonday(getArizonaDateTime());
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
    return isDecember(getArizonaDateTime());
  }

  /**
   * Is the given date and time in December in Arizona
   *
   * @return Returns true if it the given date and time is in December in Arizona
   */
  public static boolean isDecember(ZonedDateTime dateTime) {
    return dateTime.getMonth() == Month.DECEMBER;
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
