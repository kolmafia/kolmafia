package internal.helpers;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import net.sourceforge.kolmafia.persistence.DateTimeManager;

public class TestDateTimeManager extends DateTimeManager {
  private LocalDateTime time;

  public ZonedDateTime getArizonaDateTime() {
    return ZonedDateTime.of(time, ARIZONA);
  }

  public ZonedDateTime getRolloverDateTime() {
    return ZonedDateTime.of(time, ROLLOVER);
  }

  public TestDateTimeManager(LocalDateTime time) {
    this.time = time;
  }
}
