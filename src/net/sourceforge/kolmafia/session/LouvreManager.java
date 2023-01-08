package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceAdventures.Spoilers;

public abstract class LouvreManager {

  // Range of choice numbers within the LouvreManager
  private static final int FIRST_CHOICE = 904;
  private static final int LAST_CHOICE = 913;

  // There is a LouvreManager Map at:
  //
  //  http://i224.photobucket.com/albums/dd259/abeaS_oyR/kollouvre.jpg

  // The various locations within the LouvreManager
  private static final String[] LouvreLocationNames = {
    "Relativity", // 904
    "The Persistence of Memory: shoes/Dancin' Fool (buff), Venus, Piet Mondrian", // 905
    "Piet Mondrian: Scream, Moxie, Adam", // 906
    "The Scream: Relativity, Venus, Manetwich", // 907
    "The Birth of Venus: Swordholder (buff), Nighthawks, Adam", // 908
    "The Creation of Adam: Socrates, Muscle, Sunday Afternoon", // 909
    "The Death of Socrates: Relativity, Nighthawks, Vangoghbitussin", // 910
    "Nighthawks: Is This Your Card? (buff), Persistence of Memory, Sunday Afternoon", // 911
    "Sunday Afternoon on the Island of La Grande Jatte: The Last Supper, Mysticality, Piet Mondrian", // 912
    "The Last Supper: Relativity, Persistence of Memory, Pinot Renoir", // 913
  };

  private static final String[] LouvreShortLocationNames = {
    "Relativity", // 904
    "The Persistence of Memory", // 905
    "Piet Mondrian", // 906
    "The Scream", // 907
    "The Birth of Venus", // 908
    "The Creation of Adam", // 909
    "The Death of Socrates", // 910
    "Nighthawks", // 911
    "Sunday Afternoon on the Island of La Grande Jatte", // 912
    "The Last Supper", // 913
  };

  // 0 = 92, 93, 94, or 95

  private static final int[][] LouvreLocationExits = {
    {0, 0, 0}, // 904
    {7, 908, 906}, // 905
    {907, 6, 909}, // 906
    {904, 908, 1}, // 907
    {8, 911, 909}, // 908
    {910, 4, 912}, // 909
    {904, 911, 2}, // 910
    {9, 905, 912}, // 911
    {913, 5, 906}, // 912
    {904, 905, 3}, // 913
  };

  public static final String[] LouvreGoals = {
    "Manetwich",
    "bottle of Vangoghbitussin",
    "bottle of Pinot Renoir",
    "Muscle",
    "Mysticality",
    "Moxie",
    "Lady Spookyraven's shoes/Dancin' Fool (buff)",
    "Swordholder (buff)",
    "Is This Your Card? (buff)",
  };

  public static final AdventureResult[] LouvreGoalItems = {
    ItemPool.get(ItemPool.MANETWICH, 1),
    ItemPool.get(ItemPool.VANGOGHBITUSSIN, 1),
    ItemPool.get(ItemPool.PINOT_RENOIR, 1),
  };

  // Identifying strings from the response text
  public static final String[] LouvreGoalStrings = {
    "Manetwich",
    "bottle of Vangoghbitussin",
    "bottle of Pinot Renoir",
    "a pretty good workout.",
    "new insight as to the nature of the universe.",
    "Moxious!",
    "with a cane",
    "Swordholder",
    "Is This Your Card?",
  };

  // The choice table.
  //
  // One row for each LouvreManager location (92 - 104)
  // Each row contains three values, corresponding to choices 1 - 3
  //
  // 0		Unknown
  // 1 - 9	A goal
  // X		A destination

  private static int[] choiceTuple(final int source) {
    if (!LouvreManager.louvreChoice(source)) {
      return null;
    }
    return LouvreManager.LouvreLocationExits[source - LouvreManager.FIRST_CHOICE];
  }

  public static final boolean louvreChoice(final int choice) {
    return choice >= LouvreManager.FIRST_CHOICE && choice <= LouvreManager.LAST_CHOICE;
  }

  public static final void resetDecisions() {
    for (int i = 0; i < LouvreManager.LouvreGoalItems.length; ++i) {
      if (GoalManager.hasGoal(LouvreManager.LouvreGoalItems[i])) {
        Preferences.setInteger("louvreGoal", i + 1);
        return;
      }
    }

    int goal = Preferences.getInteger("louvreDesiredGoal");

    if (goal == LouvreManager.LouvreGoals.length + 1) {
      if (KoLCharacter.isMuscleClass()) {
        Preferences.setInteger("louvreGoal", 4);
      } else if (KoLCharacter.isMysticalityClass()) {
        Preferences.setInteger("louvreGoal", 5);
      } else {
        Preferences.setInteger("louvreGoal", 6);
      }
    } else if (goal == LouvreManager.LouvreGoals.length + 2) {
      // Compare total subpoints acquired, rather than the
      // non-raw, calculated value for comparing which stat
      // should be chosen next.

      long mus = KoLCharacter.getTotalMuscle();
      long mys = KoLCharacter.getTotalMysticality();
      long mox = KoLCharacter.getTotalMoxie();

      if (mus <= mys && mus <= mox) {
        Preferences.setInteger("louvreGoal", 4);
      } else if (mys <= mus && mys <= mox) {
        Preferences.setInteger("louvreGoal", 5);
      } else {
        Preferences.setInteger("louvreGoal", 6);
      }
    } else {
      Preferences.setInteger("louvreGoal", goal);
    }
  }

  private static String currentGoalString() {
    LouvreManager.resetDecisions();
    int goal = Preferences.getInteger("louvreGoal");
    if (goal <= 0 || goal > LouvreManager.LouvreGoals.length) {
      return "unknown";
    }
    return LouvreManager.LouvreGoals[goal - 1];
  }

  public static final String handleChoice(final int source, final int stepCount) {
    // We only handle LouvreManager choices
    if (!LouvreManager.louvreChoice(source)) {
      return "";
    }

    String override = Preferences.getString("louvreOverride");
    if (override.contains(",")) {
      String[] options = override.split("\\s*,\\s*");
      if (options.length > stepCount) {
        if (options[stepCount].equalsIgnoreCase("up")) {
          return "1";
        } else if (options[stepCount].equalsIgnoreCase("down")) {
          return "2";
        } else {
          return "3";
        }
      }
    }

    // Get the routing tuple for this choice/goal
    LouvreManager.resetDecisions();
    int goal = Preferences.getInteger("louvreGoal");

    // Pick the best choice
    return LouvreManager.pickNewExit(source, goal);
  }

  // Node marking to prevent loops
  private static final boolean[] NodeMarks =
      new boolean[LouvreManager.LAST_CHOICE - LouvreManager.FIRST_CHOICE + 1];

  private static String pickNewExit(final int source, final int goal) {
    // Examine destinations and take shortest known path to goal
    int[] choices = LouvreManager.choiceTuple(source);
    int choice = 0;
    int hops = Integer.MAX_VALUE;

    for (int i = 0; i < choices.length; ++i) {
      // Clear marks on nodes
      Arrays.fill(LouvreManager.NodeMarks, false);

      // Mark this node
      LouvreManager.NodeMarks[source - LouvreManager.FIRST_CHOICE] = true;

      // Determine how far destination is from goal
      int destination = choices[i];
      int dist = LouvreManager.hopsTo(0, source, destination, goal);
      if (dist < hops) {
        choice = i;
        hops = dist;
      }
    }

    return String.valueOf(choice + 1);
  }

  private static int hopsTo(int hops, final int source, int destination, final int goal) {
    if (destination == 0) {
      // This only applies to the starting noncombat.  Add 20 hops to prefer not using it.
      // Set an arbitrary destination for prediction purposes
      hops += 20;
      destination = LouvreLocationExits[source - LouvreManager.FIRST_CHOICE][0];
    }

    // If destination is the goal, we're there
    if (destination == goal) {
      return hops;
    }

    // If destination is another goal, can't get there from here
    if (destination >= 1 && destination <= LouvreManager.LouvreGoals.length) {
      return Integer.MAX_VALUE;
    }

    // If destination is a predicted but unmapped Escher node (all other
    // possibilities should have been eliminated above), we can reach any
    // goal, but prefer a more direct route.
    if (!LouvreManager.louvreChoice(destination)) {
      return hops + 100;
    }

    // Known destination. If we've been here before, punt
    if (LouvreManager.NodeMarks[destination - LouvreManager.FIRST_CHOICE]) {
      return Integer.MAX_VALUE;
    }

    // Known destination visited for first time
    LouvreManager.NodeMarks[destination - LouvreManager.FIRST_CHOICE] = true;

    // Examine destinations and take shortest known path to goal
    int[] choices = LouvreManager.choiceTuple(destination);
    int nextHops = Integer.MAX_VALUE;

    for (int i = 0; i < choices.length; ++i) {
      // Determine how far destination is from goal
      int dist = LouvreManager.hopsTo(hops + 1, destination, choices[i], goal);
      if (dist < nextHops) {
        nextHops = dist;
      }
    }
    return nextHops;
  }

  public static final Spoilers choiceSpoilers(final int choice) {
    // We only handle LouvreManager choices
    if (!LouvreManager.louvreChoice(choice)) {
      return null;
    }

    String name = LouvreManager.LouvreLocationNames[choice - LouvreManager.FIRST_CHOICE];

    // An array of choice spoilers is the third element
    int[] choices = LouvreManager.choiceTuple(choice);
    ChoiceOption[] options = new ChoiceOption[3];
    options[0] = new ChoiceOption(LouvreManager.choiceName(choices[0]));
    options[1] = new ChoiceOption(LouvreManager.choiceName(choices[1]));
    options[2] = new ChoiceOption(LouvreManager.choiceName(choices[2]));

    return new Spoilers(choice, name, options);
  }

  public static final String encounterName(final int choice) {
    if (!louvreChoice(choice)) {
      return "";
    }

    String name = LouvreManager.LouvreShortLocationNames[choice - LouvreManager.FIRST_CHOICE];
    return "Louvre It or Leave It (" + name + ")";
  }

  private static String choiceName(final int destination) {
    return switch (destination) {
      case 0 -> "";
      case 1, 2, 3, 4, 5, 6, 7, 8, 9 -> LouvreManager.LouvreGoals[destination - 1];
      default -> LouvreManager.LouvreLocationNames[destination - LouvreManager.FIRST_CHOICE];
    };
  }

  public static final void addGoalButton(final StringBuffer buffer) {
    String goal = LouvreManager.currentGoalString();
    ChoiceManager.addGoalButton(buffer, goal);
  }
}
