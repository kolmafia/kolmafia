package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HobopolisDecorator {
  private static final Pattern HOBOPOLIS_IMG_PATTERN =
      Pattern.compile("otherimages/hobopolis/[a-z]+(\\d+)");

  private HobopolisDecorator() {}

  public static final void handleTownSquare(final String responseText) {
    // Nothing to do until battle is done
    if (!responseText.contains("WINWINWIN")) {
      return;
    }

    String itemName = null;

    // Man, you really hit that hobo hard! So hard, in fact, that
    // you knocked him completely out of his skin, which is now
    // lying in a wrinkly heap on the ground.
    if (responseText.contains("wrinkly heap on the ground")) {
      itemName = "hobo skin";
    }

    // Boy, you sure scorched that hobo good! All that's left is a
    // smoking pair of boots!
    else if (responseText.contains("smoking pair of boots")) {
      itemName = "charred hobo boots";
    }

    // Wow! You froze that hobo so hard that he shattered into a
    // million pieces! All that's left is a pair of frozen eyeballs.
    else if (responseText.contains("pair of frozen eyeballs")) {
      itemName = "frozen hobo eyeballs";
    }

    // Man, you really did a number on that hobo. All that's left
    // of him is a pile of foul-smelling guts.
    else if (responseText.contains("pile of foul-smelling guts")) {
      itemName = "stinking hobo guts";
    }

    // Boy, you sure scared that hobo! He ran off so fast that he
    // left his skull behind!
    else if (responseText.contains("he left his skull behind")) {
      itemName = "creepy hobo skull";
    }

    // Wow. You embarrassed that hobo so thoroughly that he ran off
    // without his crotch!
    else if (responseText.contains("he ran off without his crotch")) {
      itemName = "hobo crotch";
    }

    if (itemName != null) {
      String message = "Richard takes a " + itemName;
      RequestLogger.updateSessionLog(message);
      RequestLogger.printLine(message);

      AdventureResult result = AdventureResult.tallyItem(itemName);
      AdventureResult.addResultToList(KoLConstants.tally, result);
    }
  }

  public static final void decorate(String location, StringBuffer buffer) {
    if (!location.contains("place=1")) {
      Matcher m = HOBOPOLIS_IMG_PATTERN.matcher(buffer);
      if (m.find()) {
        StringUtilities.singleStringReplace(
            buffer, "</b>", "</b> <font size=1>(image " + m.group(1) + ")</font>");
      }
    }

    if (location.contains("place=4")) {
      int pos = buffer.lastIndexOf("</body>");
      String ending = buffer.substring(pos);
      buffer.delete(pos, Integer.MAX_VALUE);
      buffer.append("<form># tires -> # kills: <select>");
      for (int i = 1; i <= 49; ++i) {
        float kills = 0.1f * i * i + 0.7f * i + 0.5f;
        buffer.append("<option>");
        buffer.append(i);
        buffer.append(" -> ");
        buffer.append((int) kills);
      }
      buffer.append("</select></form>");
      buffer.append(ending);
    }
  }
}
