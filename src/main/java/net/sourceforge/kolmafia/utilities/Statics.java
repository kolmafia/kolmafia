package net.sourceforge.kolmafia.utilities;

import net.sourceforge.kolmafia.persistence.DateTimeManager;

public class Statics {
  private Statics() {}

  public static DateTimeManager DateTimeManager = new DateTimeManager();

  static void setDateTimeManager(DateTimeManager dtm) {
    DateTimeManager = dtm;
  }
}
