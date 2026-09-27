package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class UnusualConstructManager {
  private static final Pattern COLOR_PATTERN = Pattern.compile("(?:LANO|ROUTING) ([a-zA-Z]*)");

  private static int DISC = 0;

  private UnusualConstructManager() {}

  public static int disc() {
    return DISC;
  }

  public static boolean solve(final String responseText) {
    DISC = 0;

    // Give us a response text, at least!
    if (responseText == null) {
      return false;
    }

    // Extract the clues from the text
    Matcher matcher = COLOR_PATTERN.matcher(responseText);
    if (!matcher.find()) {
      return false;
    }

    String colorWord = matcher.group(1);

    if (colorWord.equals("CHO")
        || colorWord.equals("FUNI")
        || colorWord.equals("TAZAK")
        || colorWord.equals("CANARY")
        || colorWord.equals("CITRINE")
        || colorWord.equals("GOLD")) {
      DISC = ItemPool.STRANGE_DISC_YELLOW;
      return true;
    }

    if (colorWord.equals("CHAKRO")
        || colorWord.equals("ZEVE")
        || colorWord.equals("ZEVESTANO")
        || colorWord.equals("CRIMSON")
        || colorWord.equals("RUBY")
        || colorWord.equals("VERMILLION")) {
      DISC = ItemPool.STRANGE_DISC_RED;
      return true;
    }

    if (colorWord.equals("BUPABU")
        || colorWord.equals("PATA")
        || colorWord.equals("SOM")
        || colorWord.equals("OBSIDIAN")
        || colorWord.equals("EBONY")
        || colorWord.equals("JET")) {
      DISC = ItemPool.STRANGE_DISC_BLACK;
      return true;
    }

    if (colorWord.equals("BE")
        || colorWord.equals("ZAKSOM")
        || colorWord.equals("ZEVEBENI")
        || colorWord.equals("JADE")
        || colorWord.equals("VERDIGRIS")
        || colorWord.equals("EMERALD")) {
      DISC = ItemPool.STRANGE_DISC_GREEN;
      return true;
    }

    if (colorWord.equals("BELA")
        || colorWord.equals("BULAZAK")
        || colorWord.equals("BU")
        || colorWord.equals("FUFUGAKRO")
        || colorWord.equals("ULTRAMARINE")
        || colorWord.equals("SAPPHIRE")
        || colorWord.equals("COBALT")) {
      DISC = ItemPool.STRANGE_DISC_BLUE;
      return true;
    }

    if (colorWord.equals("NIPA")
        || colorWord.equals("PACHA")
        || colorWord.equals("SOMPAPA")
        || colorWord.equals("IVORY")
        || colorWord.equals("ALABASTER")
        || colorWord.equals("PEARL")) {
      DISC = ItemPool.STRANGE_DISC_WHITE;
      return true;
    }

    return false;
  }
}
