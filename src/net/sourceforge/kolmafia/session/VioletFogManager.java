package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceAdventures.Option;
import net.sourceforge.kolmafia.session.ChoiceAdventures.Spoilers;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.WikiUtilities;

public abstract class VioletFogManager {
  private static final Pattern CHOICE_PATTERN = Pattern.compile("whichchoice value=(\\d+)");

  // Range of choice numbers within the fog

  private static final int FIRST_CHOICE = 48;
  private static final int LAST_CHOICE = 70;

  // The various locations within the violet fog

  private static final String[] FogLocationNames = {
    "Violet Fog (Start)", // 48
    "Man on Bicycle", // 49
    "Pleasant-Faced Man", // 50
    "Man on Cornflake", // 51
    "Giant Chessboard", // 52
    "Improbable Mustache", // 53
    "Fog of Birds", // 54
    "Intense-Looking Man", // 55
    "Boat on River", // 56
    "Man in Sunglasses", // 57
    "Huge Caterpillar", // 58
    "Man in Bowler", // 59
    "Dance Number", // 60
    "Huge Mountain", // 61
    "The Big Scary Place (Headgear)", // 62
    "The Big Scary Place (Weapon)", // 63
    "The Big Scary Place (Garment)", // 64
    "The Prince of Wishful Thinking (Body)", // 65
    "The Prince of Wishful Thinking (Wisdom)", // 66
    "The Prince of Wishful Thinking (Charm)", // 67
    "She's So Unusual (Alcohol)", // 68
    "She's So Unusual (Food)", // 69
    "She's So Unusual (Herbs or Medicines)", // 70
  };

  private static final int[][] FogLocationExits = {
    {49, 50, 51}, // 48
    {52, 53, 56}, // 49
    {53, 54, 57}, // 50
    {52, 54, 55}, // 51
    {61, 65, 68}, // 52
    {61, 66, 69}, // 53
    {61, 67, 70}, // 54
    {58, 65, 70}, // 55
    {59, 66, 68}, // 56
    {60, 67, 69}, // 57
    {51, 52, 63}, // 58
    {49, 53, 62}, // 59
    {50, 54, 64}, // 60
    {49, 50, 51}, // 61
    {50, 52, 61}, // 62
    {51, 53, 61}, // 63
    {49, 54, 61}, // 64
    {50, 51, 54}, // 65
    {49, 51, 52}, // 66
    {49, 50, 53}, // 67
    {49, 50, 53}, // 68
    {50, 51, 54}, // 69
    {49, 51, 52}, // 70
  };

  // The routing table.
  //
  // One row for each fog location (48 - 70)
  // Each row contains one tuple for each possible fog destination (49 - 70)
  // Each tuple contains the Next Hop and the Hop Count to get there

  private static int[][][] FogRoutingTable;

  private static int[] routingTuple(final int source, final int destination) {
    if (source < VioletFogManager.FIRST_CHOICE
        || source > VioletFogManager.LAST_CHOICE
        || destination < VioletFogManager.FIRST_CHOICE + 1
        || destination > VioletFogManager.LAST_CHOICE) {
      return null;
    }
    return VioletFogManager.FogRoutingTable[source - VioletFogManager.FIRST_CHOICE][
        destination - VioletFogManager.FIRST_CHOICE - 1];
  }

  private static int nextHop(final int source, final int destination) {
    int[] tuple = VioletFogManager.routingTuple(source, destination);
    return tuple == null ? -1 : tuple[0];
  }

  static {
    VioletFogManager.buildRoutingTable();
    // printRoutingTable();
  }

  private static void buildRoutingTable() {
    // Get a zeroed array to start things off.
    VioletFogManager.FogRoutingTable =
        new int[VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE + 1]
            [VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE][2];
    int unfilled =
        (VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE + 1)
            * (VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE);

    // Seed it with final destinations: next hop = -1 and hopcount = 0
    for (int source = VioletFogManager.FIRST_CHOICE + 1;
        source <= VioletFogManager.LAST_CHOICE;
        ++source) {
      int[] tuple = VioletFogManager.routingTuple(source, source);
      tuple[0] = -1;
      tuple[1] = 0;
      --unfilled;
    }

    // Seed it with exit destinations: next hop = destination and hopcount = 1
    for (int source = VioletFogManager.FIRST_CHOICE;
        source <= VioletFogManager.LAST_CHOICE;
        ++source) {
      int[] exits = VioletFogManager.FogLocationExits[source - VioletFogManager.FIRST_CHOICE];
      for (int destination : exits) {
        int[] tuple = VioletFogManager.routingTuple(source, destination);
        tuple[0] = destination;
        tuple[1] = 1;
        --unfilled;
      }
    }

    // Now iterate over entire table calculating next hops and hopcounts
    while (unfilled > 0) {
      int filled = 0;

      for (int source = VioletFogManager.FIRST_CHOICE;
          source <= VioletFogManager.LAST_CHOICE;
          ++source) {
        for (int destination = VioletFogManager.FIRST_CHOICE + 1;
            destination <= VioletFogManager.LAST_CHOICE;
            ++destination) {
          int[] tuple = VioletFogManager.routingTuple(source, destination);

          // If we've calculated this tuple, skip it
          if (tuple[0] != 0) {
            continue;
          }

          // See which of our direct exits can get there fastest
          int nextHop = 0;
          int hopCount = Integer.MAX_VALUE;

          int[] exits = VioletFogManager.FogLocationExits[source - VioletFogManager.FIRST_CHOICE];
          for (int exit : exits) {
            int[] destTuple = VioletFogManager.routingTuple(exit, destination);
            if (destTuple[0] != 0 && destTuple[1] < hopCount) {
              nextHop = exit;
              hopCount = destTuple[1];
            }
          }

          // If we found a route, enter it into table
          if (nextHop != 0) {
            tuple[0] = nextHop;
            tuple[1] = hopCount + 1;
            ++filled;
          }
        }
      }

      if (filled == 0) {
        RequestLogger.printLine(
            "Internal error: " + unfilled + " unreachable nodes in violet fog routing table");
        break;
      }

      unfilled -= filled;
    }
  }

  // Range of choice numbers with a goal
  private static final int FIRST_GOAL_LOCATION = 62;
  public static final String[] FogGoals = {
    "escape from the fog", // 48-61
    "Cerebral Cloche", // 62
    "Cerebral Crossbow", // 63
    "Cerebral Culottes", // 64
    "Muscle Training", // 65
    "Mysticality Training", // 66
    "Moxie Training", // 67
    "ice stein", // 68
    "munchies pill", // 69
    "homeopathic healing powder", // 70
    "Boost Prime Stat",
    "Boost Lowest Stat"
  };
  private static final AdventureResult[] FogGoalItems = {
    null,
    ItemPool.get(ItemPool.C_CLOCHE, 1),
    ItemPool.get(ItemPool.C_CROSSBOW, 1),
    ItemPool.get(ItemPool.C_CULOTTES, 1),
    null,
    null,
    null,
    ItemPool.get(ItemPool.ICE_STEIN, 1),
    ItemPool.get(ItemPool.MUNCHIES_PILL, 1),
    ItemPool.get(ItemPool.HOMEOPATHIC, 1),
  };

  // The choice table.
  //
  // One row for each fog location (48 - 70)
  // Each row contains four values, corresponding to choices 1 - 4
  //
  // -1	The "goal"
  //  0	Unknown
  // xx	A destination

  private static final int[][] FogChoiceTable =
      new int[VioletFogManager.LAST_CHOICE - VioletFogManager.FIRST_CHOICE + 1][4];

  public static void reset() {
    // Reset what we've "learned" about the fog choices
    for (int i = VioletFogManager.FIRST_CHOICE; i <= VioletFogManager.LAST_CHOICE; ++i) {
      int[] choice = VioletFogManager.FogChoiceTable[i - VioletFogManager.FIRST_CHOICE];
      choice[0] = i < VioletFogManager.FIRST_GOAL_LOCATION ? 0 : -1;
      choice[1] = 0;
      choice[2] = 0;
      choice[3] = i < VioletFogManager.FIRST_GOAL_LOCATION ? -1 : 0;
    }

    int lastVioletFogAscension = Preferences.getInteger("lastVioletFogMap");
    if (lastVioletFogAscension != KoLCharacter.getAscensions()) {
      Preferences.setInteger("lastVioletFogMap", KoLCharacter.getAscensions());
      Preferences.setString("violetFogLayout", "");
    }

    String layout = Preferences.getString("violetFogLayout");
    if (layout.equals("")) {
      return;
    }

    int currentIndex = 0;
    String[] layoutSplit = layout.split(",");

    for (int i = 0; i < VioletFogManager.FogChoiceTable.length; ++i) {
      for (int j = 0; j < VioletFogManager.FogChoiceTable[i].length; ++j) {
        VioletFogManager.FogChoiceTable[i][j] =
            StringUtilities.parseInt(layoutSplit[currentIndex++]);
      }
    }
  }

  public static void saveMap() {
    StringBuilder map = new StringBuilder();

    for (int i = 0; i < VioletFogManager.FogChoiceTable.length; ++i) {
      for (int j = 0; j < VioletFogManager.FogChoiceTable[i].length; ++j) {
        if (i != 0 || j != 0) {
          map.append(',');
        }
        map.append(VioletFogManager.FogChoiceTable[i][j]);
      }
    }

    Preferences.setInteger("lastVioletFogMap", KoLCharacter.getAscensions());
    Preferences.setString("violetFogLayout", map.toString());
  }

  private static int parseGoal() {
    var goal =
        IntStream.range(0, VioletFogManager.FogGoalItems.length)
            .filter(
                i -> {
                  var item = VioletFogManager.FogGoalItems[i];
                  return item != null && GoalManager.hasGoal(item);
                })
            .findAny()
            .orElse(Preferences.getInteger("violetFogGoal"));

    if (goal < 0 || goal > 11) {
      return -1;
    }

    if (goal == 10) { // Boost Prime Stat
      return KoLCharacter.getPrimeIndex() + 4;
    } else if (goal == 11) { // Boost Lowest Stat
      long mus = KoLCharacter.getTotalMuscle();
      long mys = KoLCharacter.getTotalMysticality();
      long mox = KoLCharacter.getTotalMoxie();

      if (mus <= mys && mus <= mox) {
        return 4;
      } else if (mys <= mus && mys <= mox) {
        return 5;
      } else {
        return 6;
      }
    }

    return goal;
  }

  private static String currentGoalString() {
    var goal = parseGoal();

    if (goal < 0) {
      return "unknown";
    }

    return VioletFogManager.FogGoals[goal];
  }

  public static boolean fogChoice(final int choice) {
    return choice >= VioletFogManager.FIRST_CHOICE && choice <= VioletFogManager.LAST_CHOICE;
  }

  public static String handleChoice(final int source) {
    // We only handle Violet Fog choices
    if (!VioletFogManager.fogChoice(source)) {
      return "";
    }

    var goal = parseGoal();

    // If no goal, return "4".
    // - If we are not at a "goal" location, this will exit the fog
    // - If we are at a "goal" location, this will send us to a non-"goal" location
    if (goal == 0) {
      return "4";
    }

    // Find the location we must get to in order to achieve the goal
    int destination = VioletFogManager.FIRST_GOAL_LOCATION + goal - 1;
    if (!VioletFogManager.fogChoice(destination)) {
      return "";
    }

    // Are we there yet?
    if (source == destination) {
      // The first decision will get us the goal we seek
      return "1";
    }

    // We haven't reached the goal yet. Find the next hop.
    int nextHop = VioletFogManager.nextHop(source, destination);

    // Choose the path that will take us there
    int[] path = VioletFogManager.FogChoiceTable[source - VioletFogManager.FIRST_CHOICE];
    for (int i = 0; i < path.length; ++i) {
      if (path[i] == nextHop) {
        return String.valueOf(i + 1);
      }
    }

    // We don't know how to get there. Pick an unexplored path.
    for (int i = 0; i < path.length; ++i) {
      if (path[i] == 0) {
        // We don't know how to get to the Next Hop
        return String.valueOf(i + 1);
      }
    }

    // This shouldn't happen
    return "";
  }

  public static boolean mapChoice(final int lastChoice, final int lastDecision, final String text) {
    if (!VioletFogManager.fogChoice(lastChoice)) {
      return false;
    }

    // Punt if bogus decision
    if (lastDecision < 1 || lastDecision > 4) {
      return true;
    }

    // Return if we've already mapped this decision
    if (VioletFogManager.FogChoiceTable[lastChoice - VioletFogManager.FIRST_CHOICE][
            lastDecision - 1]
        != 0) {
      return true;
    }

    Matcher choiceMatcher = VioletFogManager.CHOICE_PATTERN.matcher(text);
    if (!choiceMatcher.find()) {
      return false;
    }

    int source = StringUtilities.parseInt(choiceMatcher.group(1));

    // We only handle Violet Fog choices
    if (!VioletFogManager.fogChoice(source)) {
      return false;
    }

    // Update the path table
    int[] choices = VioletFogManager.FogChoiceTable[lastChoice - VioletFogManager.FIRST_CHOICE];
    choices[lastDecision - 1] = source;
    VioletFogManager.saveMap();

    // See if exactly one exit is unknown
    int unknownIndex = -1;
    for (int i = 0; i < choices.length; ++i) {
      if (choices[i] != 0) {
        continue;
      }
      if (unknownIndex != -1) {
        return true;
      }
      unknownIndex = i;
    }

    // Done if all three destinations are known.
    if (unknownIndex == -1) {
      return true;
    }

    // Yes. Figure out which one it is
    for (int exit : FogLocationExits[lastChoice - FIRST_CHOICE]) {
      boolean found = false;
      for (int choice : choices) {
        if (exit == choice) {
          found = true;
          break;
        }
      }

      if (!found) {
        choices[unknownIndex] = exit;
        saveMap();
        return true;
      }
    }

    return true;
  }

  public static Spoilers choiceSpoilers(final int choice) {
    // We only handle Violet Fog choices
    if (!fogChoice(choice)) {
      return null;
    }

    String name = FogLocationNames[choice - FIRST_CHOICE];

    // An array of choice spoilers is the third element
    int[] choices = FogChoiceTable[choice - FIRST_CHOICE];
    var options =
        new Option[] {
          new Option(choiceName(choice, choices[0])),
          new Option(choiceName(choice, choices[1])),
          new Option(choiceName(choice, choices[2])),
          new Option(choiceName(choice, choices[3])),
        };

    return new Spoilers(choice, name, options);
  }

  private static String choiceName(final int choice, final int destination) {
    // If it's unknown, no name
    if (destination == 0) {
      return "";
    }

    // If it's the Goal, pick the goal
    if (destination == -1) {
      return choice < FIRST_GOAL_LOCATION
          ? FogGoals[0]
          : FogGoals[choice - FIRST_GOAL_LOCATION + 1];
    }

    // Otherwise, return the name of the destination
    return VioletFogManager.FogLocationNames[destination - VioletFogManager.FIRST_CHOICE];
  }

  public static void addGoalButton(final StringBuffer buffer) {
    String goal = VioletFogManager.currentGoalString();
    ChoiceManager.addGoalButton(buffer, goal);
  }

  private static String getWikiLink(int i) {
    var name = "Violet Fog" + (i == 0 ? "" : " (" + FogLocationNames[i] + ")");
    return WikiUtilities.getWikiLocation(name, WikiUtilities.ANY_TYPE, false);
  }

  private static final String[] EDGE_COLORS = {
    // Unmapped
    "black",
    // This way
    "blue",
    // That way
    "red",
    // The other way
    "purple"
  };

  public static String generateGraph() {
    var goal = parseGoal();
    var goalChoice = goal > 0 ? FIRST_GOAL_LOCATION + goal - 1 : -1;

    var dot = new StringBuilder();
    dot.append("digraph G {\n")
        // Add a legend
        .append(
            """
            graph [labelloc="b" label=<
              <TABLE BORDER="0"><TR>
                  <TD COLSPAN="2">This way <font color="blue">→</font> &nbsp; That way <font color="red">→</font> &nbsp; The other way <font color="purple">→</font></TD>
              </TR><TR>
                  <TD>
                      <TABLE BORDER="0"><TR>
                          <TD>You are here</TD>
                          <TD BGCOLOR="red" WIDTH="20px">
                              <TABLE BORDER="0"><TR><TD BGCOLOR="yellow"></TD></TR></TABLE>
                          </TD>
                      </TR></TABLE>
                  </TD>
                  <TD>
                      <TABLE BORDER="0"><TR>
                          <TD>Your goal</TD>
                          <TD BGCOLOR="green" WIDTH="20px">
                              <TABLE BORDER="0"><TR><TD BGCOLOR="yellow"></TD></TR></TABLE>
                          </TD>
                      </TR></TABLE>
                  </TD>
              </TR></TABLE>
            >]
          """)
        // And lay out with the circular engine (the only one that is readable!)
        .append("  layout=circo\n");
    for (int source = FIRST_CHOICE; source <= LAST_CHOICE; source++) {
      dot.append("  ")
          .append(source)
          .append(" [")
          // Fill the node so it can be clicked
          .append("style=\"filled\" ");

      // Color the node based on its relation to the player
      // These are just the colors Gemelli chose in his tool
      var youAreHere = ChoiceManager.lastChoice == source;
      var yourGoal = goalChoice == source;
      if (youAreHere) {
        dot.append("fillcolor=yellow color=red ");
      } else if (yourGoal) {
        dot.append("fillcolor=yellow color=green ");
      } else {
        dot.append("fillcolor=white ");
      }

      var i = source - FIRST_CHOICE;
      var url = getWikiLink(i);
      dot.append("href=\"")
          // When the player clicks on a node send them to the Wiki page
          .append(url)
          .append("\" ")
          .append("label=\"")
          .append(i)
          .append("\" ")
          // When the player hovers they'll see the name of the choice adventure
          .append("tooltip=\"")
          .append(FogLocationNames[i])
          .append("\"")
          .append("]\n");

      // For each exit from this location, draw an edge
      for (int destination : FogLocationExits[i]) {
        dot.append("  ").append(source).append(" -> ").append(destination);
        var paths = FogChoiceTable[i];
        for (int p = 0; p < paths.length; p++) {
          // If we've mapped this choice, highlight it on graph
          if (paths[p] == destination) {
            var result = source < FIRST_GOAL_LOCATION ? p + 1 : p;
            // Show a tooltip when hovering the edges
            dot.append(" [tooltip=\"")
                .append(
                    switch (result) {
                      case 1 -> "this way";
                      case 2 -> "that way";
                      case 3 -> "the other way";
                      default -> "";
                    })
                .append("\" ")
                .append("color=")
                .append(EDGE_COLORS[result])
                .append("]");
            break;
          }
        }
        dot.append("\n");
      }
    }
    dot.append("}");
    return dot.toString();
  }

  public static void addGraph(final StringBuffer buffer) {
    var graph = generateGraph();
    var index = buffer.lastIndexOf("</table>");
    if (index < 0) return;

    var graphRenderer =
        new StringBuilder()
            .append("<div id=\"violetFogGraph\" style=\"max-width: 95%\">Loading graph...</div>")
            .append(
                "<script src=\"https://cdn.jsdelivr.net/npm/@hpcc-js/wasm/dist/index.min.js\"></script>")
            .append("<script>")
            .append("var hpccWasm = window[\"@hpcc-js/wasm\"];")
            .append("const dot=`")
            .append(graph)
            .append("`;")
            .append("hpccWasm.graphviz.layout(dot, \"svg\", \"dot\").then(svg => {")
            .append("var el = document.getElementById(\"violetFogGraph\");")
            .append("el.innerHTML = svg;")
            .append("el.firstElementChild.removeAttribute(\"height\");")
            .append("el.firstElementChild.removeAttribute(\"width\");")
            .append("});")
            .append("</script>");
    buffer.insert(index + 8, graphRenderer);
  }
}
