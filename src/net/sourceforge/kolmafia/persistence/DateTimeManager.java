package net.sourceforge.kolmafia.persistence;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static net.sourceforge.kolmafia.KoLmafia.kolTimeZone;

public class DateTimeManager {
  public static final ZoneId ROLLOVER = ZoneId.of(kolTimeZone.getID());
  public static final ZoneId ARIZONA = ZoneId.of("GMT-0700");

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

  private DateTimeManager() {}
}
