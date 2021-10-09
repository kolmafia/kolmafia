package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class RumpleManager {
  // adventure.php?snarfblat=381 -> Portal to Terrible Parents
  // 844/1 -> spy on parents
  // 844/2 -> enter the portal -> 846
  // 844/3 -> return to map

  // 846/1 -> Speak to father -> 847
  // 846/1 -> Speak to mother -> 847
  // 846/3 -> Speak to both parents -> 847
  // 846/4 -> exit to 844

  // 847/1 -> Appeal to greed -> 848
  // 847/2 -> Appeal to gluttony -> 848
  // 847/3 -> Appeal to vanity -> 848
  // 847/4 -> Appeal to laziness -> 848
  // 847/5 -> Appeal to lustfulness -> 848
  // 847/6 -> Appeal to violence -> 848

  // 848/1 -> 1 (3) kid offering
  // 848/2 -> 5 (7) kid offering
  // 848/3 -> 5 (7) kid offering
  // 848/4 -> exit to 846

  // place.php?whichplace=ioty2014_rumple&action=workshop -> Rumpelstiltskin's Workshop
  // 845/1 -> practice crafting -> 849
  // 845/2 -> craft stuff -> 850
  // 845/3 -> tinker around (leads to shop)
  // 845/4 -> return to map

  // 849/1 -> practice making filling
  // 849/2 -> practice making parchment
  // 849/3 -> practice making glass
  // 849/4 -> exit to 845

  // 850/1 -> turn 3 straw into 1 filling
  // 850/2 -> turn 3 leather into 1 parchment
  // 850/3 -> turn 3 clay into 1 glass
  // 850/4 -> exit to 845

  // <span class='guts'>You peer through the portal into a house full of activity.  Children are
  // everywhere!  The portal lets you watch them and their parents without fear of being noticed.
  // You see the father tearing down the blinds to peep out of the window. You watch some of the
  // many children play for awhile, and then you see the mother reclined in an overstuffed chair
  // eating a bag of bacon-flavored onion rings. You're distracted by yet more kids romping around,
  // and when you look back you see the father trying to squeeze into a girdle. Then the portal
  // shimmers and you see no more.</span>
  private static final Pattern GUTS_PATTERN =
      Pattern.compile("<span class='guts'>(.*?)</span>", Pattern.DOTALL);
  private static final Pattern PATTERN1 =
      Pattern.compile("without fear of being noticed. *Y([^.]*)", Pattern.DOTALL);
  private static final Pattern PATTERN2 = Pattern.compile("and then y([^.]*)", Pattern.DOTALL);
  private static final Pattern PATTERN3 =
      Pattern.compile("when you look back y([^.]*)", Pattern.DOTALL);

  private static final String NEITHER = "neither parent";
  private static final String FATHER = "the father";
  private static final String MOTHER = "the mother";
  private static final String BOTH = "both parents";

  private static String parent = RumpleManager.NEITHER;

  private static final String NONE = "good nature";
  private static final String GREED = "inherent greed";
  private static final String GLUTTONY = "gluttony";
  private static final String VANITY = "vanity";
  private static final String LAZINESS = "laziness";
  private static final String LUSTFULNESS = "lustfulness";
  private static final String VIOLENCE = "violent nature";

  private static String sin = RumpleManager.NONE;

  private static final String[][] sins = new String[3][];

  private enum State {
    CLOSED,
    STARTED,
    ENDED
  }

  private static State state = State.CLOSED;

  public static final void reset(final int choice) {
    if (choice == 4) {
      // No matter what the previous state was, it is started now
      RumpleManager.state = State.STARTED;
    }
    // If this quest is currently closed, nothing to do
    if (RumpleManager.state == State.CLOSED) {
      return;
    }

    // Otherwise, we are starting a new mask after having done a
    // gnome quest.

    // We lose all crafting ingredients

    int straw = InventoryManager.getCount(ItemPool.STRAW);
    if (straw > 0) {
      ResultProcessor.processItem(ItemPool.STRAW, -straw);
    }

    int leather = InventoryManager.getCount(ItemPool.LEATHER);
    if (leather > 0) {
      ResultProcessor.processItem(ItemPool.LEATHER, -leather);
    }

    int clay = InventoryManager.getCount(ItemPool.CLAY);
    if (clay > 0) {
      ResultProcessor.processItem(ItemPool.CLAY, -clay);
    }

    int filling = InventoryManager.getCount(ItemPool.FILLING);
    if (filling > 0) {
      ResultProcessor.processItem(ItemPool.FILLING, -filling);
    }

    int parchment = InventoryManager.getCount(ItemPool.PARCHMENT);
    if (parchment > 0) {
      ResultProcessor.processItem(ItemPool.PARCHMENT, -parchment);
    }

    int glass = InventoryManager.getCount(ItemPool.GLASS);
    if (glass > 0) {
      ResultProcessor.processItem(ItemPool.GLASS, -glass);
    }

    // Reset score
    Preferences.setInteger("rumpelstiltskinTurnsUsed", 0);
    Preferences.setInteger("rumpelstiltskinKidsRescued", 0);

    // Reset parent sins
    RumpleManager.resetSins();

    // If the zone wasn't just started, it is now closed
    if (choice != 4) {
      RumpleManager.state = State.CLOSED;
    }
  }

  public static final void resetSins() {
    RumpleManager.parent = RumpleManager.NEITHER;
    RumpleManager.sins[0] = null;
    RumpleManager.sins[1] = null;
    RumpleManager.sins[2] = null;
  }

  public static final void spyOnParents(final String responseText) {
    Matcher gutsMatcher = RumpleManager.GUTS_PATTERN.matcher(responseText);
    if (!gutsMatcher.find()) {
      System.out.println("no guts");
      return;
    }

    String guts = gutsMatcher.group(1);
    RumpleManager.sins[0] = RumpleManager.detectSins(RumpleManager.PATTERN1, guts);
    RumpleManager.sins[1] = RumpleManager.detectSins(RumpleManager.PATTERN2, guts);
    RumpleManager.sins[2] = RumpleManager.detectSins(RumpleManager.PATTERN3, guts);
  }

  private static String[] detectSins(final Pattern pattern, final String text) {
    Matcher matcher = pattern.matcher(text);
    if (!matcher.find()) {
      System.out.println("no glory");
      return null;
    }

    // Look at the message and decide which sin(s) applies to which parent
    String sin = "Y" + matcher.group(1);
    String[] record = RumpleManager.parseSins(sin);

    String sin1 = record[1];
    String sin2 = record[2];

    StringBuilder buffer = new StringBuilder();
    buffer.append(sin);
    buffer.append(" (");
    buffer.append(sin1 == RumpleManager.NONE ? "UNKNOWN" : sin1);
    if (sin2 != RumpleManager.NONE) {
      buffer.append(" or ");
      buffer.append(sin2);
    }
    buffer.append(")");

    String message = buffer.toString();

    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);

    return record;
  }

  private static final String[][] TELLS = {
    {"counting his possessions", RumpleManager.GREED, RumpleManager.NONE},
    {"counting her possessions", RumpleManager.GREED, RumpleManager.NONE},
    {"gold-plating a lily", RumpleManager.GREED, RumpleManager.NONE},
    {"studying a treasure map", RumpleManager.GREED, RumpleManager.NONE},
    {"writing a contract to make a high-interest loan", RumpleManager.GREED, RumpleManager.NONE},
    {"chowing down on a fistful of bacon", RumpleManager.GLUTTONY, RumpleManager.NONE},
    {"eating a chocolate rabbit", RumpleManager.GLUTTONY, RumpleManager.NONE},
    {"putting a fried egg on top of a cheeseburger", RumpleManager.GLUTTONY, RumpleManager.NONE},
    {
      "sprinkling extra cheese on a pizza already dripping with the stuff",
      RumpleManager.GLUTTONY,
      RumpleManager.NONE
    },
    {"checking his reflection in a spoon", RumpleManager.VANITY, RumpleManager.NONE},
    {"checking her reflection in a spoon", RumpleManager.VANITY, RumpleManager.NONE},
    {"plucking his eyebrows", RumpleManager.VANITY, RumpleManager.NONE},
    {"plucking her eyebrows", RumpleManager.VANITY, RumpleManager.NONE},
    {"putting in a teeth-whitening tray", RumpleManager.VANITY, RumpleManager.NONE},
    {"telling everyone that the song on the radio", RumpleManager.VANITY, RumpleManager.NONE},
    {
      "asking one of the kids to find the remote control",
      RumpleManager.LAZINESS,
      RumpleManager.NONE
    },
    {
      "collapsed deep in an easy chair, stifling a yawn", RumpleManager.LAZINESS, RumpleManager.NONE
    },
    {
      "lying on the floor, calling his spouse to come give a kiss",
      RumpleManager.LAZINESS,
      RumpleManager.LUSTFULNESS
    },
    {
      "lying on the floor, calling her spouse to come give a kiss",
      RumpleManager.LAZINESS,
      RumpleManager.LUSTFULNESS
    },
    {"lying on the floor", RumpleManager.LAZINESS, RumpleManager.NONE},
    {"sleeping soundly", RumpleManager.LAZINESS, RumpleManager.NONE},
    {"eyeing her spouse lasciviously", RumpleManager.LUSTFULNESS, RumpleManager.NONE},
    {"eyeing his spouse lasciviously", RumpleManager.LUSTFULNESS, RumpleManager.NONE},
    {"flipping through a lingerie catalog", RumpleManager.LUSTFULNESS, RumpleManager.NONE},
    {"moaning softly", RumpleManager.LUSTFULNESS, RumpleManager.NONE},
    {
      "peeking through the blinds at the attractive neighbors",
      RumpleManager.LUSTFULNESS,
      RumpleManager.NONE
    },
    {"kicking a dog", RumpleManager.VIOLENCE, RumpleManager.NONE},
    {"punching a hole in the wall of the house", RumpleManager.VIOLENCE, RumpleManager.NONE},
    {"screaming at the television", RumpleManager.VIOLENCE, RumpleManager.NONE},
    {"stubbing his toe on an ottoman", RumpleManager.VIOLENCE, RumpleManager.NONE},
    {"stubbing her toe on an ottoman", RumpleManager.VIOLENCE, RumpleManager.NONE},
    {"cutting a huge piece of cake to eat alone", RumpleManager.GREED, RumpleManager.GLUTTONY},
    {"opening a family-size bag of Cheat-Os", RumpleManager.GREED, RumpleManager.GLUTTONY},
    {"putting a golden ring on each finger", RumpleManager.GREED, RumpleManager.VANITY},
    {"shining a golden chalice to a reflective finish", RumpleManager.GREED, RumpleManager.VANITY},
    {
      "reclined in a chair, ordering stuff from the Home Shopping Network",
      RumpleManager.GREED,
      RumpleManager.LAZINESS
    },
    {
      "trying to shortchange the maid who is washing the dishes",
      RumpleManager.GREED,
      RumpleManager.LAZINESS
    },
    {"admiring a solid marble nude statue", RumpleManager.GREED, RumpleManager.LUSTFULNESS},
    {
      "admiring a valuable collection of artistic nudes",
      RumpleManager.GREED,
      RumpleManager.LUSTFULNESS
    },
    {"polishing a collection of solid silver daggers", RumpleManager.GREED, RumpleManager.VIOLENCE},
    {
      "loading a jewel-encrusted pistol with golden bullets",
      RumpleManager.GREED,
      RumpleManager.VIOLENCE
    },
    {
      "checking for stretch marks while downing a huge chocolate shake",
      RumpleManager.GLUTTONY,
      RumpleManager.VANITY
    },
    {"trying to squeeze into a girdle", RumpleManager.GLUTTONY, RumpleManager.VANITY},
    {
      "calling the dog over to lick french-fry grease",
      RumpleManager.GLUTTONY,
      RumpleManager.LAZINESS
    },
    {
      "reclined in an overstuffed chair eating a bag of bacon-flavored onion rings",
      RumpleManager.GLUTTONY,
      RumpleManager.LAZINESS
    },
    {"licking an all-day sucker", RumpleManager.GLUTTONY, RumpleManager.LUSTFULNESS},
    {"sensually over a rack of ribs", RumpleManager.GLUTTONY, RumpleManager.LUSTFULNESS},
    {
      "tearing apart an entire roasted chicken so hard the bones snap",
      RumpleManager.GLUTTONY,
      RumpleManager.VIOLENCE
    },
    {
      "throwing a bag of chips on the ground and stomping on it to open it",
      RumpleManager.GLUTTONY,
      RumpleManager.VIOLENCE
    },
    {
      "collapsed in an overstuffed chair, curling his eyelashes",
      RumpleManager.VANITY,
      RumpleManager.LAZINESS
    },
    {
      "collapsed in an overstuffed chair, curling her eyelashes",
      RumpleManager.VANITY,
      RumpleManager.LAZINESS
    },
    {
      "using the remote control to turn the TV to the Beauty Channel",
      RumpleManager.VANITY,
      RumpleManager.LAZINESS
    },
    {
      "checking out own his body and licking his lips seductively",
      RumpleManager.VANITY,
      RumpleManager.LUSTFULNESS
    },
    {
      "checking out own her body and licking her lips seductively",
      RumpleManager.VANITY,
      RumpleManager.LUSTFULNESS
    },
    {
      "practicing pick-up lines on his own reflection in a window",
      RumpleManager.VANITY,
      RumpleManager.LUSTFULNESS
    },
    {
      "practicing pick-up lines on her own reflection in a window",
      RumpleManager.VANITY,
      RumpleManager.LUSTFULNESS
    },
    {"angrily plucking stray eyebrow hairs", RumpleManager.VANITY, RumpleManager.VIOLENCE},
    {
      "kicking the dog, then making sure the kick didn't scuff",
      RumpleManager.VANITY,
      RumpleManager.VIOLENCE
    },
    {
      "reclined on the bed, idly peeping through the window to the neighbor's house",
      RumpleManager.LAZINESS,
      RumpleManager.LUSTFULNESS
    },
    {
      "half-heartedly kicking at the cat when it comes too close",
      RumpleManager.LAZINESS,
      RumpleManager.VIOLENCE
    },
    {"sleepily swiping at a whining kid", RumpleManager.LAZINESS, RumpleManager.VIOLENCE},
    {"aggressively kissing", RumpleManager.LUSTFULNESS, RumpleManager.VIOLENCE},
    {
      "tearing down the blinds to peep out of the window",
      RumpleManager.LUSTFULNESS,
      RumpleManager.VIOLENCE
    },
  };

  private static String[] parseSins(final String text) {
    String[] record = new String[3];
    record[0] =
        text.contains("father")
            ? RumpleManager.FATHER
            : text.contains("mother") ? RumpleManager.MOTHER : RumpleManager.NEITHER;

    String sin1 = RumpleManager.NONE;
    String sin2 = RumpleManager.NONE;

    for (String[] tell : RumpleManager.TELLS) {
      if (text.contains(tell[0])) {
        sin1 = tell[1];
        sin2 = tell[2];
        break;
      }
    }

    record[1] = sin1;
    record[2] = sin2;

    return record;
  }

  public static final void pickParent(final int choice) {
    switch (choice) {
      case 1:
        RumpleManager.parent = RumpleManager.FATHER;
        break;
      case 2:
        RumpleManager.parent = RumpleManager.MOTHER;
        break;
      case 3:
        RumpleManager.parent = RumpleManager.BOTH;
        break;
      case 4:
        if (Preferences.getInteger("rumpelstiltskinTurnsUsed") == 30) {
          RumpleManager.state = State.ENDED;
        }
        RumpleManager.resetSins();
        break;
    }
  }

  public static final void pickSin(final int choice) {
    switch (choice) {
      case 1:
        RumpleManager.sin = RumpleManager.GREED;
        break;
      case 2:
        RumpleManager.sin = RumpleManager.GLUTTONY;
        break;
      case 3:
        RumpleManager.sin = RumpleManager.VANITY;
        break;
      case 4:
        RumpleManager.sin = RumpleManager.LAZINESS;
        break;
      case 5:
        RumpleManager.sin = RumpleManager.LUSTFULNESS;
        break;
      case 6:
        RumpleManager.sin = RumpleManager.VIOLENCE;
        break;
      default:
        // There should be no other choice options
        RumpleManager.sin = RumpleManager.NONE;
        break;
    }
  }

  public static final void recordTrade(final String text) {
    int kids =
        (text.contains("one of h") || text.contains("one child"))
            ? 1
            : (text.contains("three of their")
                    || text.contains("three kids")
                    || text.contains("three whole children"))
                ? 3
                : (text.contains("semi-precious children") || text.contains("five kids"))
                    ? 5
                    : (text.contains("seven children")
                            || text.contains("seven kids")
                            || text.contains("seven of their not-so-precious-after-all children"))
                        ? 7
                        : 0;

    Preferences.increment("rumpelstiltskinKidsRescued", kids);

    // You get the sense that your bartering proposals are becoming wearisome.

    String message =
        "Appealing to the "
            + RumpleManager.sin
            + " of "
            + RumpleManager.parent
            + " allowed you to rescue "
            + kids
            + " "
            + (kids == 1 ? "child" : "children")
            + ".";

    RequestLogger.printLine(message);
    RequestLogger.updateSessionLog(message);
  }

  static final String STRAW = "straw";
  static final String LEATHER = "leather";
  static final String CLAY = "clay";
  static final String FILLING = "filling";
  static final String PARCHMENT = "parchment";
  static final String GLASS = "glass";

  private static final String[][] BRIBES = {
    {RumpleManager.GREED, RumpleManager.STRAW},
    {RumpleManager.GREED, RumpleManager.STRAW, RumpleManager.PARCHMENT},
    {RumpleManager.GREED, RumpleManager.CLAY, RumpleManager.FILLING},
    {RumpleManager.GLUTTONY, RumpleManager.STRAW},
    {RumpleManager.GLUTTONY, RumpleManager.LEATHER, RumpleManager.PARCHMENT},
    {RumpleManager.GLUTTONY, RumpleManager.CLAY, RumpleManager.FILLING},
    {RumpleManager.VANITY, RumpleManager.LEATHER},
    {RumpleManager.VANITY, RumpleManager.CLAY, RumpleManager.PARCHMENT},
    {RumpleManager.VANITY, RumpleManager.STRAW, RumpleManager.GLASS},
    {RumpleManager.LAZINESS, RumpleManager.LEATHER},
    {RumpleManager.LAZINESS, RumpleManager.LEATHER, RumpleManager.FILLING},
    {RumpleManager.LAZINESS, RumpleManager.STRAW, RumpleManager.GLASS},
    {RumpleManager.LUSTFULNESS, RumpleManager.CLAY},
    {RumpleManager.LUSTFULNESS, RumpleManager.STRAW, RumpleManager.PARCHMENT},
    {RumpleManager.LUSTFULNESS, RumpleManager.LEATHER, RumpleManager.GLASS},
    {RumpleManager.VIOLENCE, RumpleManager.CLAY},
    {RumpleManager.VIOLENCE, RumpleManager.CLAY, RumpleManager.GLASS},
    {RumpleManager.VIOLENCE, RumpleManager.LEATHER, RumpleManager.FILLING},
  };

  public static final void decorateWorkshop(final StringBuffer buffer) {
    // If you have been through the portal 5 times, you can still
    // access the workshop to craft things for use in tinkering,
    // but the parents are no longer an issue.
    if (RumpleManager.state == State.ENDED) {
      return;
    }

    // Otherwise, you should consider crafting things that will
    // help you tempt the parents.

    int insertionPoint = buffer.lastIndexOf("</form>") + 7;
    if (insertionPoint == 6) {
      return;
    }

    StringBuilder output = new StringBuilder();

    output.append("<p><center>");

    int turns = Preferences.getInteger("rumpelstiltskinTurnsUsed");
    if (turns > 0 && (turns % 6) == 0) {
      // The portal is open
      if (RumpleManager.sins[0] == null) {
        output.append("<font color=red>Go spy on the parents to get clues about their sins</font>");
      } else {
        for (String[] sin : RumpleManager.sins) {
          if (sin != null) {
            String parent = sin[0];
            String sin1 = sin[1];
            String sin2 = sin[2];

            output.append(parent);
            output.append(": ");
            output.append(sin1 == RumpleManager.NONE ? "UNKNOWN" : sin1);
            if (sin2 != RumpleManager.NONE) {
              output.append(" or ");
              output.append(sin2);
            }
            output.append("<br>");
          }
        }
      }
    } else {
      output.append("<font color=red>The portal is not open yet.</font>");
    }

    output.append("</center>");

    output.append("<p><center>");
    output.append("<table border=2 cols=4>");
    output.append("<tr>");
    output.append("<th>Sin</th>");
    output.append("<th>1 kid</th>");
    output.append("<th>5 kids</th>");
    output.append("<th>5 kids</th>");
    output.append("</tr>");

    String lastsin = RumpleManager.NONE;

    for (String[] row : RumpleManager.BRIBES) {
      String sin = row[0];
      if (lastsin != sin) {
        if (lastsin != RumpleManager.NONE) {
          output.append("</tr>");
        }
        output.append("<tr><td>");
        output.append(sin);
        output.append("</td>");
        lastsin = sin;
      }

      output.append("<td>");
      output.append(row[1]);
      if (row.length == 2) {
        output.append("&nbsp;");
      } else {
        output.append(" + ");
        output.append(row[2]);
      }
      output.append("</td>");
    }

    if (lastsin != RumpleManager.NONE) {
      output.append("</tr>");
    }

    output.append("</table></center>");

    buffer.insert(insertionPoint, output.toString());
  }
}
