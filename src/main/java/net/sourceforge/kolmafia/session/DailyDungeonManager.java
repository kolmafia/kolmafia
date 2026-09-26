package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DailyDungeonManager {
  private static final Pattern CHAMBER_PATTERN = Pattern.compile("[Cc]hamber <b>#(\\d+)</b>");

  public static void handleRoomEntrance(String text, RoomType roomType) {
    Matcher chamberMatcher = CHAMBER_PATTERN.matcher(text);
    if (chamberMatcher.find()) {
      int chamber = StringUtilities.parseInt(chamberMatcher.group(1));
      handleRoomEntrance(chamber, roomType);
    }
  }

  public static void handleRoomEntrance(int chamber, RoomType roomType) {
    Preferences.setInteger("_lastDailyDungeonRoom", chamber - 1);
    updateDailyDungeonRoom(chamber, roomType);
  }

  public static void handleCurrentRoomCompletion(RoomType roomType) {
    int chamber = Preferences.getInteger("_lastDailyDungeonRoom") + 1;
    handleRoomCompletion(chamber, roomType);
  }

  public static void handleRoomCompletion(int chamber, RoomType roomType) {
    Preferences.setInteger("_lastDailyDungeonRoom", chamber);
    updateDailyDungeonRoom(chamber, roomType);

    if (chamber >= 15) {
      Preferences.setBoolean("dailyDungeonDone", true);
    }
  }

  // Visible for testing
  static void updateDailyDungeonRoom(int chamber, RoomType roomType) {
    if (chamber < 1 || chamber > 14) {
      return;
    }
    String ddData = Preferences.getString("dailyDungeonRooms");
    if (!validPref(ddData)) {
      ddData = Preferences.getDefault("dailyDungeonRooms");
    }
    ddData = ddData.substring(0, chamber - 1) + roomType.code + ddData.substring(chamber);
    Preferences.setString("dailyDungeonRooms", ddData);
  }

  // A valid value for the dailyDungeonRooms preference is of the form "XXXX_XXXX_XXXX",
  // where each X is one of 'M', 'D', 'T', or '?'.
  private static final Pattern VALID_PREF_PATTERN =
      Pattern.compile("[MDT?]{4}_[MDT?]{4}_[MDT?]{4}");

  static boolean validPref(String ddData) {
    return VALID_PREF_PATTERN.matcher(ddData).matches();
  }

  public enum RoomType {
    DOOR('D'),
    TRAP('T'),
    MONSTER('M'),
    TREASURE('_');

    public final char code;

    RoomType(char code) {
      this.code = code;
    }
  }
}
