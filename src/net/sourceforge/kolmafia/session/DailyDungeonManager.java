package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.preferences.Preferences;

public class DailyDungeonManager {
  public static void updateDailyDungeonRoom(int chamber, RoomType roomType) {
    String ddData = Preferences.getString("dailyDungeonRooms");
    if (ddData.length() < 14) {
      // This shouldn't ever happen unless the user manually updates the preference to something
      // broken, but oh well.
      ddData = ddData + "????_????_????".substring(ddData.length());
    }
    ddData = ddData.substring(0, chamber - 1) + roomType.code + ddData.substring(chamber);
    Preferences.setString("dailyDungeonRooms", ddData);
  }

  public enum RoomType {
    DOOR('D'),
    TRAP('T'),
    MONSTER('M'),
    TREASURE('_');

    private final char code;

    RoomType(char code) {
      this.code = code;
    }
  }
}
