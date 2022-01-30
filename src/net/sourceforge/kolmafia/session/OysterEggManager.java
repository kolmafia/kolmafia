package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class OysterEggManager {
  public static void trackEgg(String responseText) {
    if (!HolidayDatabase.getHoliday().contains("Oyster Egg Day")) return;
    if (responseText.contains("ou find an Oyster egg")) {
      Preferences.increment("_oysterEggsFound");
    }
  }
}
