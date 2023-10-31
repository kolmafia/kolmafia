package net.sourceforge.kolmafia.utilities;

import internal.helpers.TestDateTimeManager;
import java.time.LocalDateTime;
import net.sourceforge.kolmafia.persistence.DateTimeManager;

public class TestStatics {
  public static void setDate(LocalDateTime time) {
    Statics.setDateTimeManager(new TestDateTimeManager(time));
  }

  public static void setDateTimeManager(DateTimeManager dtm) {
    Statics.setDateTimeManager(dtm);
  }
}
