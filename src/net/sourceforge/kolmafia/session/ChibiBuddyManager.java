package net.sourceforge.kolmafia.session;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ChibiBuddyManager {
  private static final Pattern CHIBI_NAME =
      Pattern.compile("&quot;I am (.*?), and I am sure we will be the best of friends!&quot;");

  private static final Pattern CHIBI_STATS =
      Pattern.compile("<td height=25>(.*?): </td><td><img.*?title=\"(\\d+) dots\"></td>");

  private static final Pattern CHIBI_AGE = Pattern.compile("</s><center>.*? is (\\d) days old.<p>");

  private ChibiBuddyManager() {}

  public static void visit(final int choice, final String text) {
    if (text.contains("<b>Oh no!</b>")) {
      // Adventures are reset but ChibiChanged is not
      Preferences.resetToDefault(
          "_chibiAdventures",
          "chibiAlignment",
          "chibiBirthday",
          "chibiFitness",
          "chibiIntelligence",
          "chibiLastVisit",
          "chibiName",
          "chibiSocialization");
      ResultProcessor.processItem(ItemPool.CHIBIBUDDY_ON, -1);
      ResultProcessor.processItem(ItemPool.CHIBIBUDDY_OFF, 1);
      return;
    }

    var daycount = KoLCharacter.getCurrentDays();
    Preferences.setInteger("chibiLastVisit", daycount);

    var ageMatcher = CHIBI_AGE.matcher(text);
    if (ageMatcher.find()) {
      Preferences.setInteger(
          "chibiBirthday", daycount - StringUtilities.parseInt(ageMatcher.group(1)));
    }

    if (text.contains("value=\"Put your ChibiBuddy&trade; away\"")) {
      Preferences.setBoolean("_chibiChanged", !text.contains("value=\"Have a ChibiChat&trade;\">"));
    }

    var statMatcher = CHIBI_STATS.matcher(text);

    while (statMatcher.find()) {
      var stat = statMatcher.group(1);
      var value = StringUtilities.parseInt(statMatcher.group(2));
      Preferences.setInteger("chibi" + stat, value);
    }
  }

  public static void postChoice2(final int choice, final int decision, final String text) {
    switch (choice) {
      case 627:
        if (decision == 5) {
          Preferences.setBoolean("_chibiChanged", true);
        }
        break;
      case 628:
      case 629:
      case 630:
      case 631:
        if (!text.contains("Results:")) return;
        if (decision == 1 || decision == 2) {
          Preferences.increment("_chibiAdventures", 1, 5, false);
        }
      case 633:
        if (decision == 1) {
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_OFF, -1);
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_ON, 1);

          var matcher = CHIBI_NAME.matcher(text);
          if (matcher.find()) {
            Preferences.setString("chibiName", matcher.group(1));
          }

          Preferences.setInteger("chibiBirthday", KoLCharacter.getGlobalDays());
          Preferences.setInteger("chibiLastVisit", KoLCharacter.getGlobalDays());
        }
        break;
    }
  }
}
