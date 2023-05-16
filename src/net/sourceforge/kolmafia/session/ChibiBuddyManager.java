package net.sourceforge.kolmafia.session;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class ChibiBuddyManager {
  private static Pattern CHIBI_NAME =
      Pattern.compile("&quot;I am (.*?), and I am sure we will be the best of friends!&quot;");

  private ChibiBuddyManager() {}

  public static void visitMainScreen(final String text) {
    Preferences.setBoolean("_chibiChanged", !text.contains("Have a ChibiChat"));
  }

  public static void postChoice2(final int choice, final int decision, final String text) {
    switch (choice) {
      case 627:
        if (decision == 5) {
          Preferences.setBoolean("_chibiChanged", true);
        }
        break;
      case 633:
        if (decision == 1) {
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_OFF, -1);
          ResultProcessor.processItem(ItemPool.CHIBIBUDDY_ON, 1);

          var matcher = CHIBI_NAME.matcher(text);

          if (matcher.find()) {
            Preferences.setString("chibiName", matcher.group(1));
          }
        }
        break;
    }
  }
}
