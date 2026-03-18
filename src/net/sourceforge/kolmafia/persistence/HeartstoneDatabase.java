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
    var compact = monsterName.replaceAll("\\s+", "");
    var bytes = compact.getBytes(StandardCharsets.UTF_8);
    var length = bytes.length;
    // even length has no middle
    if (length % 2 == 0) {
      return null;
    }
    int mid = length / 2;
    var middle = new String(new byte[] {bytes[mid]}, StandardCharsets.UTF_8);
    if (!middle.matches("^[A-Za-z]$")) {
      return null;
    }
    return new MiddleLetter(middle.toUpperCase(Locale.ENGLISH), mid);
  }
}
