package net.sourceforge.kolmafia.persistence;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class HeartstoneDatabase {
  private HeartstoneDatabase() {}

  public record MiddleLetter(String letter, int byteIndex) {}

  public static MiddleLetter middleLetter(String monsterName) {
    if (monsterName.isEmpty()) {
      return null;
    }
    var noSpaces = monsterName.replaceAll(" ", "");
    var bytes = noSpaces.getBytes(StandardCharsets.UTF_8);
    var length = bytes.length;
    // even length has no middle
    if (length % 2 == 0) {
      return null;
    }
    var middle =
        new String(new byte[] {bytes[length / 2]}, StandardCharsets.UTF_8)
            .toUpperCase(Locale.ENGLISH);
    if (!middle.matches("[A-Z]")) {
      return null;
    }
    return new MiddleLetter(middle, length / 2);
  }
}
