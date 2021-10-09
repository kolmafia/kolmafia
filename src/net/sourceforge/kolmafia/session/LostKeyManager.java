package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class LostKeyManager {
  public static final String[] goals = {"Lost Glasses", "Lost Comb", "Lost Pill Bottle"};

  private static final String[] steps = {
    "121111", // glasses
    "131212", // comb
    "131113", // pill bottle
  };

  public static final String autoKey(
      String decision, final int stepCount, final String responseText) {
    int goal = StringUtilities.parseInt(decision) - 1;

    if ((goal < 0 || goal >= steps.length)
        || (stepCount < 0 || stepCount >= steps[goal].length())) {
      return "0";
    }

    decision = steps[goal].substring(stepCount, stepCount + 1);
    String action = ChoiceManager.findChoiceDecisionText(Integer.parseInt(decision), responseText);
    if (action != null) {
      logText("Action: " + action);
    }

    return decision;
  }

  private static void logText(final String text) {
    RequestLogger.printLine(text);
    RequestLogger.updateSessionLog(text);
  }

  private static String currentGoalString() {
    int goal = Preferences.getInteger("choiceAdventure594");

    if (goal < 1 || goal > goals.length) {
      return null;
    }

    return goals[goal - 1];
  }

  public static final void addGoalButton(final StringBuffer buffer) {
    // Only add the goal button to the first choice
    if (buffer.indexOf("hotel next door") != -1) {
      String goal = LostKeyManager.currentGoalString();
      if (goal != null) {
        ChoiceManager.addGoalButton(buffer, goal);
      }
    }
  }
}
