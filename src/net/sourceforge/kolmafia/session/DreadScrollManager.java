package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class DreadScrollManager {
  public enum ClueType {
    LIBRARY1,
    HEALSCROLL,
    DEEP_DARK_VISIONS,
    KNUCKLEBONE,
    KILLSCROLL,
    LIBRARY2,
    WORKTEA,
    LIBRARY3
  }

  // You can't make heads or tails of the contents of the book, but
  // somebody has scrawled "<clue>" on the inside of the front cover in
  // what appears to be blood. Spooky.

  private static final Pattern LIBRARY1_PATTERN =
      Pattern.compile("somebody has scrawled &quot;<b>(.*?)</b>&quot;");

  // You flip through the book, finding almost nothing of actual
  // interest. You do notice, however, that there seem to be a lot of
  // references to <clue> creatures. Curious.

  private static final Pattern LIBRARY2_PATTERN =
      Pattern.compile("a lot of references to <b>(.*?)</b> creatures.");

  // You flip through the book, and find very little in the way of useful
  // information. There is one curious chapter, though, which just
  // consists of the phrase <clue> over and over, hundreds of
  // times. Creepy.

  private static final Pattern LIBRARY3_PATTERN =
      Pattern.compile("consists of the phrase <b>(.*?)</b> over and over");

  public static final void handleLibrary(String responseText) {
    Matcher matcher = LIBRARY1_PATTERN.matcher(responseText);
    if (matcher.find()) {
      DreadScrollManager.setClue(ClueType.LIBRARY1, matcher.group(1));
      return;
    }

    matcher = LIBRARY2_PATTERN.matcher(responseText);
    if (matcher.find()) {
      DreadScrollManager.setClue(ClueType.LIBRARY2, matcher.group(1));
      return;
    }

    matcher = LIBRARY3_PATTERN.matcher(responseText);
    if (matcher.find()) {
      DreadScrollManager.setClue(ClueType.LIBRARY3, matcher.group(1));
      return;
    }
  }

  // You sound out the strange text on the scroll. As you finish, you're
  // bathed in a hideous green glow. Horrific images begin to dance in
  // your mind -- three-lidded eyes in the darkness, tentacles squirming
  // along the ocean floor, a magnificent <clue>, smiling warmly in the
  // distance, publicity stills from Battlefield Earth... Your mind reels
  // at the horror, but your body feels strangely replenished...

  private static final Pattern HEALSCROLL_PATTERN = Pattern.compile("a magnificent <b>(.*?)</b>");

  public static final void handleHealscroll(String responseText) {
    Matcher matcher = HEALSCROLL_PATTERN.matcher(responseText);
    if (matcher.find()) {
      DreadScrollManager.setClue(ClueType.HEALSCROLL, matcher.group(1));
    }
  }

  // Something about the words on that scroll sticks in your mind. You
  // actually did recognize one of them: '<clue>'. Strange.

  private static final Pattern KILLSCROLL_PATTERN =
      Pattern.compile("recognize one of them: <b>&quot;(.*?)&quot;</b>");

  public static final void handleKillscroll(String responseText) {
    Matcher matcher = KILLSCROLL_PATTERN.matcher(responseText);
    if (matcher.find()) {
      DreadScrollManager.setClue(ClueType.KILLSCROLL, matcher.group(1));
    }
  }

  // You roll the bone, over and over, and every time it hits the ground,
  // it bounces straight <direction>. You get so weirded out by it that
  // you throw it away.

  private static final Pattern KNUCKLEBONE_PATTERN =
      Pattern.compile("it bounces straight <b>(.*?)</b>.");

  public static final void handleKnucklebone(String responseText) {
    Matcher matcher = KNUCKLEBONE_PATTERN.matcher(responseText);
    if (matcher.find()) {
      DreadScrollManager.setClue(ClueType.KNUCKLEBONE, matcher.group(1));
    }
  }

  // <b><clue></b>

  private static final Pattern DEEP_DARK_VISIONS_PATTERN =
      Pattern.compile(
          "You close your eyes and let Deep visions wash over you.*?<b>(.*?)</b>.*itemimages/hp.gif");

  public static final void handleDeepDarkVisions(String responseText) {
    Matcher matcher = DEEP_DARK_VISIONS_PATTERN.matcher(responseText);
    if (matcher.find()) {
      DreadScrollManager.setClue(ClueType.DEEP_DARK_VISIONS, matcher.group(1));
    }
  }

  // You manage to inadvertently drink a cup of that gross Mer-kin tea
  // while you're eating the sushi.  And hey, look -- the leaves in the
  // bottom look just like <b>an eel</b>!

  private static final Pattern WORKTEA_PATTERN =
      Pattern.compile("the leaves in the bottom look just like <b>([^<]*)</b>");

  public static final void handleWorktea(String responseText) {
    Matcher matcher = DreadScrollManager.WORKTEA_PATTERN.matcher(responseText);
    if (!matcher.find()) {
      return;
    }

    String clue = matcher.group(1);
    DreadScrollManager.setClue(ClueType.WORKTEA, clue);
    Preferences.setString("workteaClue", clue);
    ResultProcessor.processItem(ItemPool.MERKIN_WORKTEA, -1);
  }

  public static final String[][][] CLUE_DATA = {
    {
      {"Mer-kin Library 1", "dreadScroll1"},
      {"LONELY", "lonely"},
      {"DOUBLED", "doubled"},
      {"THRICE-CURSED", "thrice-cursed"},
      {"FOURTH", "fourth"},
    },
    {
      {"Mer-kin healscroll", "dreadScroll2"},
      {"starfish"},
      {"moonfish"},
      {"sunfish"},
      {"planetfish"},
    },
    {
      {"Deep Dark Visions", "dreadScroll3"},
      {"The House of Cards", "Cards"},
      {"The House of Blues", "Blues"},
      {"The House of Pancakes", "Pancakes"},
      {"The House of Pain", "Pain"},
    },
    {
      {"Mer-kin knucklebone", "dreadScroll4"},
      {"north", "Northern"},
      {"south", "Southern"},
      {"east", "Eastern"},
      {"west", "Western"},
    },
    {
      {"Mer-kin killscroll", "dreadScroll5"},
      {"red", "as red as blood"},
      {"black", "as black as ink"},
      {"green", "as green as bile"},
      {"yellow", "as yellow as piss"},
    },
    {
      {"Mer-kin Library 2", "dreadScroll6"},
      {"blind"},
      {"giant"},
      {"finless"},
      {"two-headed"},
    },
    {
      {"Mer-kin worktea", "dreadScroll7"},
      {"an eel", "eel"},
      {"a turtle", "turtle"},
      {"a shark", "shark"},
      {"a whale", "whale"},
    },
    {
      {"Mer-kin Library 3", "dreadScroll8"},
      {"one thousand squirming young"},
      {"two and twenty stillborn spawn"},
      {"conjoined triplets"},
      {"a brand new dance craze"},
    },
  };

  private static void setClue(ClueType clue, String value) {
    int index = clue.ordinal();
    String[][] data = CLUE_DATA[index];
    String setting = data[0][1];
    for (int i = 1; i <= 4; ++i) {
      if (value.equals(data[i][0])) {
        Preferences.setInteger(setting, i);
        break;
      }
    }

    String message = data[0][0] + " clue: " + value;
    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  private static String cluePhrase(ClueType clue) {
    int index = clue.ordinal();
    String[][] data = CLUE_DATA[index];
    String setting = data[0][1];
    int value = Preferences.getInteger(setting);

    if (value < 1 || value > 4) {
      return "???";
    }

    String[] option = data[value];
    return option.length > 1 ? option[1] : option[0];
  }

  private static void clueStatus(StringBuilder buffer, ClueType clue) {
    int index = clue.ordinal();
    String[][] data = CLUE_DATA[index];
    String setting = data[0][1];
    int value = Preferences.getInteger(setting);

    buffer.append(setting);
    buffer.append(" (");
    buffer.append(data[0][0]);
    buffer.append("): ");
    buffer.append(value);
    buffer.append(" (");
    buffer.append(value == 0 ? "unknown" : data[value][0]);
    buffer.append(")");
    buffer.append(KoLConstants.LINE_BREAK);
  }

  public static final String getClues() {
    StringBuilder buffer = new StringBuilder();
    clueStatus(buffer, ClueType.LIBRARY1);
    clueStatus(buffer, ClueType.HEALSCROLL);
    clueStatus(buffer, ClueType.DEEP_DARK_VISIONS);
    clueStatus(buffer, ClueType.KNUCKLEBONE);
    clueStatus(buffer, ClueType.KILLSCROLL);
    clueStatus(buffer, ClueType.LIBRARY2);
    clueStatus(buffer, ClueType.WORKTEA);
    clueStatus(buffer, ClueType.LIBRARY3);
    return buffer.toString();
  }

  // When the <thrice-cursed> <moonfish> is in the <House of Cards>,
  // and the <Northern> Current runs as <red as blood>,
  // when a <blind> <shark> births <two and twenty stillborn spawn>,
  // the Elder shall awaken.

  public static final String getScrollText() {

    String buffer =
        "When the "
            + cluePhrase(ClueType.LIBRARY1)
            + " "
            + cluePhrase(ClueType.HEALSCROLL)
            + " is in the House of "
            + cluePhrase(ClueType.DEEP_DARK_VISIONS)
            + ","
            + KoLConstants.LINE_BREAK
            + "and the "
            + cluePhrase(ClueType.KNUCKLEBONE)
            + " Current runs "
            + cluePhrase(ClueType.KILLSCROLL)
            + ","
            + KoLConstants.LINE_BREAK
            + "when a "
            + cluePhrase(ClueType.LIBRARY2)
            + " "
            + cluePhrase(ClueType.WORKTEA)
            + " births "
            + cluePhrase(ClueType.LIBRARY3)
            + ","
            + KoLConstants.LINE_BREAK
            + "the Elder shall awaken. ";
    return buffer;
  }

  public static final void decorate(final StringBuffer buffer) {
    StringUtilities.globalStringDelete(buffer, " selected");

    for (int pro = 1; pro <= 8; ++pro) {
      String[][] data = CLUE_DATA[pro - 1];
      String setting = data[0][1];
      int value = Preferences.getInteger(setting);
      if (value == 0) {
        continue;
      }

      String[] option = data[value];
      String key = option.length > 1 ? option[1] : option[0];

      String find = ">" + key;
      String replace = " selected>--&gt;" + key + "&lt;--";
      StringUtilities.globalStringReplace(buffer, find, replace);
    }
  }
}
