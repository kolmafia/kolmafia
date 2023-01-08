package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class SafetyShelterManager {
  public static final ChoiceOption[] RonaldGoals = {
    new ChoiceOption("E.M.U. rocket thrusters"),
    new ChoiceOption("Spell Transfer Complete"),
    new ChoiceOption("E.M.U. joystick"),
    new ChoiceOption("elven medi-pack & magi-pack"),
    new ChoiceOption("Overstimulated"),
    new ChoiceOption("Simulation Stimulation"),
  };

  private static final String[] RonaldScript = {
    "11211", // E.M.U. rocket thrusters
    "1122", // Spell Transfer Complete
    "12211", // E.M.U. joystick
    "12221", // elven medi-pack & magi-pack
    "1321", // Overstimulated
    "1322", // Simulation Stimulation
  };

  public static final ChoiceOption[] GrimaceGoals = {
    new ChoiceOption("distention pill"),
    new ChoiceOption("synthetic dog hair pill"),
    new ChoiceOption("Heal Thy Nanoself"),
    new ChoiceOption("E.M.U. harness"),
    new ChoiceOption("elven hardtack & squeeze"),
    new ChoiceOption("E.M.U. Helmet"),
  };

  private static final String[] GrimaceScript = {
    "1121", // distention pill
    "1122", // synthetic dog hair pill
    "1211", // Heal Thy Nanoself
    "12121", // E.M.U. harness
    "13211", // elven hardtack & squeeze
    "12221", // E.M.U. Helmet
  };

  private SafetyShelterManager() {}

  public static final String autoRonald(
      final String decision, final int stepCount, final String responseText) {
    return SafetyShelterManager.autoShelter(decision, stepCount, responseText, RonaldScript);
  }

  public static final String autoGrimace(
      final String decision, final int stepCount, final String responseText) {
    return SafetyShelterManager.autoShelter(decision, stepCount, responseText, GrimaceScript);
  }

  public static final String autoShelter(
      String decision, final int stepCount, final String responseText, final String[] script) {
    int goal = StringUtilities.parseInt(decision) - 1;

    if ((goal < 0 || goal >= script.length)
        || (stepCount < 0 || stepCount >= script[goal].length())) {
      return "0";
    }

    decision = script[goal].substring(stepCount, stepCount + 1);
    String action =
        ChoiceUtilities.findChoiceDecisionText(Integer.parseInt(decision), responseText);
    if (action != null) {
      logText("Action: " + action);
    }

    return decision;
  }

  private static void logText(final String text) {
    RequestLogger.printLine(text);
    RequestLogger.updateSessionLog(text);
  }

  public static final void addRonaldGoalButton(final StringBuffer buffer) {
    // Only add the goal button to the first choice
    if (buffer.indexOf("Take a Look Around") == -1) {
      return;
    }

    int goal = Preferences.getInteger("choiceAdventure535");
    if (goal < 1 || goal > RonaldGoals.length) {
      return;
    }

    ChoiceManager.addGoalButton(buffer, RonaldGoals[goal - 1].getName());
  }

  public static final void addGrimaceGoalButton(final StringBuffer buffer) {
    // Only add the goal button to the first choice
    if (buffer.indexOf("Down the Hatch!") == -1) {
      return;
    }

    int goal = Preferences.getInteger("choiceAdventure536");
    if (goal < 1 || goal > GrimaceGoals.length) {
      return;
    }

    ChoiceManager.addGoalButton(buffer, GrimaceGoals[goal - 1].getName());
  }
}
