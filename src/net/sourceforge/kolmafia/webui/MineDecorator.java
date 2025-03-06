package net.sourceforge.kolmafia.webui;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
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

  public static void decorate(final String location, final StringBuffer buffer) {
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

  private static final Pattern MINE_STATE =
      Pattern.compile("alt='([^(]*?) \\(([123456]),([123456])\\)'");

  private static void parseState(final int mine, final String responseText) {
    var state =
        MINE_STATE
            .matcher(responseText)
            .results()
            .map(
                m -> {
                  var linear =
                      ((StringUtilities.parseInt(m.group(3)) - 1) * 6)
                          + (StringUtilities.parseInt(m.group(2)) - 1);
                  var code =
                      switch (m.group(1)) {
                        case "Open Cavern" -> "o";
                        case "Promising Chunk of Wall" -> "*";
                        case "Rocky Wall" -> "X";
                        default -> "?";
                      };
                  return Map.entry(linear, code);
                })
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(Collectors.joining());

    Preferences.setString("mineState" + mine, state);
  }

  private static void parseResult(
      final int mine, final String location, final String responseText) {
    var m = WHICH_PATTERN.matcher(location);
    if (!m.find()
        || (!responseText.contains("You acquire")
            && !responseText.contains(
                "/itemimages/hp.gif\" height=30 width=30></td><td valign=center class=effect>"))) {
      return;
    }

    var which = m.group(1);
    m = IMG_PATTERN.matcher(responseText);

    if (!m.find()) {
      return;
    }

    var pref = "mineLayout" + mine;

    Preferences.setString(pref, Preferences.getString(pref) + "#" + which + m.group(0));
  }

  public static void parseResponse(final String location, final String responseText) {
    if (KoLCharacter.hasSkill(SkillPool.UNACCOMPANIED_MINER)
        && StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Unaccompanied Miner")
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
        Preferences.setString("mineLayout" + i, "");
        Preferences.setString("mineState" + i, "");
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

    var mine = StringUtilities.parseInt(m.group(1));

    parseState(mine, responseText);

    if (location.contains("reset=1")) {
      Preferences.setString("mineLayout" + mine, "");
      return;
    }

    parseResult(mine, location, responseText);
  }
}
