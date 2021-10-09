package net.sourceforge.kolmafia.webui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class MineDecorator {
  private static final Pattern MINE_PATTERN = Pattern.compile("mine=(\\d+)");
  private static final Pattern WHICH_PATTERN = Pattern.compile("which=(\\d+)");
  private static final Pattern IMG_PATTERN = Pattern.compile("<img[^>]+>");
  private static final Pattern TD_PATTERN =
      Pattern.compile(
          "<td.*?src=['\"](.*?)['\"].*?alt=['\"](.*?) \\(([\\d+]),([\\d+])\\)['\"].*?</td>");

  public static final void decorate(final String location, final StringBuffer buffer) {
    // Replace difficult to see sparkles with more obvious images
    StringUtilities.globalStringReplace(
        buffer,
        KoLmafia.imageServerPath() + "otherimages/mine/wallsparkle",
        "/images/otherimages/mine/wallsparkle");

    if (buffer.indexOf("<div id='postload'") == -1) {
      return;
    }

    // Determine which mine we are in
    Matcher m = MINE_PATTERN.matcher(location);
    if (!m.find()) {
      return;
    }

    // Fetch explored layout of that mine.
    String data = Preferences.getString("mineLayout" + m.group(1));

    // Find the ore squares in the image.
    m = TD_PATTERN.matcher(buffer.toString());
    if (!m.find()) {
      return;
    }

    buffer.setLength(0);
    do {
      if (!m.group(2).equals("Open Cavern")) {
        continue;
      }

      // KoL now lists squares as (col,row).
      // Columns go from 0 to 7. Rows go from 0 to 6
      int col = StringUtilities.parseInt(m.group(3));
      int row = StringUtilities.parseInt(m.group(4));

      int which = (row * 8) + col;

      Matcher n = Pattern.compile("#" + which + "(<.*?>)").matcher(data);
      if (!n.find()) {
        continue;
      }
      m.appendReplacement(
          buffer, "<td width=50 height=50 background='$1' align=center>" + n.group(1) + "</td>");
    } while (m.find());
    m.appendTail(buffer);
  }

  public static final void parseResponse(final String location, final String responseText) {
    if (KoLCharacter.hasSkill("Unaccompanied Miner")
        && StandardRequest.isAllowed("Skills", "Unaccompanied Miner")
        && Preferences.getInteger("_unaccompaniedMinerUsed") < 5) {
      if (responseText.contains("Mining a chunk of the cavern wall takes")) {
        Preferences.setInteger("_unaccompaniedMinerUsed", 5);
      } else if (responseText.contains(
          "You start digging. You hit the rock with all your might.")) {
        Preferences.increment("_unaccompaniedMinerUsed");
      }
    }

    if (Preferences.getInteger("lastMiningReset") != KoLCharacter.getAscensions()) {
      for (int i = 1; i < 10; ++i) {
        if (Preferences.getString("mineLayout" + i).length() > 0) {
          Preferences.setString("mineLayout" + i, "");
        }
      }
      Preferences.setInteger("lastMiningReset", KoLCharacter.getAscensions());
    }

    if (responseText.contains(
        "You use a convenient stick of minin' dynamite to quickly and effortlessly blast away some rock.")) {
      ResultProcessor.processItem(ItemPool.MININ_DYNAMITE, -1);
    }

    Matcher m = MINE_PATTERN.matcher(location);
    if (!m.find()) {
      return;
    }
    String pref = "mineLayout" + m.group(1);
    if (location.indexOf("reset=1") != -1) {
      Preferences.setString(pref, "");
      return;
    }
    m = WHICH_PATTERN.matcher(location);
    if (!m.find()
        || (!responseText.contains("You acquire")
            && !responseText.contains(
                "/itemimages/hp.gif\" height=30 width=30></td><td valign=center class=effect>"))) {
      return;
    }
    String which = m.group(1);
    m = IMG_PATTERN.matcher(responseText);
    if (!m.find()) {
      return;
    }
    Preferences.setString(pref, Preferences.getString(pref) + "#" + which + m.group(0));
  }
}
