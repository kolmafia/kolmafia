package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;

public class VampOutManager {
  public static final ChoiceOption[] VampOutGoals = {
    // Visit Vlad's Boutique
    new ChoiceOption("Mistified"),
    new ChoiceOption("Bat Attitude"),
    new ChoiceOption("There Wolf"),

    // Visit Isabella's
    new ChoiceOption("Muscle"),
    new ChoiceOption("Mysticality"),
    new ChoiceOption("Moxie"),
    new ChoiceOption("111 Meat, lose 1-2 hp"),

    // Visit The Masquerade
    new ChoiceOption("Prince of Seaside Town and Sword of the Brouhaha Prince"),
    new ChoiceOption("Prince of Seaside Town and Sceptre of the Torremolinos Prince"),
    new ChoiceOption("Prince of Seaside Town and Medallion of the Ventrilo Prince"),
    new ChoiceOption("Prince of Seaside Town and Chalice of the Malkovich Prince"),
    new ChoiceOption("Pride of the Vampire and Interview With You (a Vampire)"),
    new ChoiceOption("your own black heart")
  };

  private static final String[] VampOutScript = {
    // Visit Vlad's Boutique
    "022121221", // Mistified
    "02212111", // Bat Attitude
    "02231111", // There Wolf

    // Visit Isabella's
    "011", // Gain 4 x <mainstat> Muscle substats, max 500
    "0131", // Gain 4 x <mainstat> Mysticality substats, max 500
    "01221", // Gain 4 x <mainstat> Moxie substats, max 500
    "01232", // Gain 111 meat, lose 1-2 hp

    // Visit The Masquerade
    "031241mtbv11", // Muscle - Prince of Seaside Town and Sword of the Brouhaha Prince
    "042112mvtb11", // Mysticality - Prince of Seaside Town and Sceptre of the Torremolinos Prince
    "014423vmbt11", // Moxie - Prince of Seaside Town and Medallion of the Ventrilo Prince
    "023334tvbm11", // All Stats - Prince of Seaside Town and Chalice of the Malkovich Prince
    "031241vmtb11", // Pride of the Vampire and Interview With You (a Vampire)
    "031241vbtm11" // your own black heart
  };

  private static final String BROUHAHA = "Brouhaha";
  private static final String MALKOVICH = "Malkovich";
  private static final String TORREMOLINOS = "Torremolinos";
  private static final String VENTRILO = "Ventrilo";

  private VampOutManager() {}

  public static final String autoVampOut(
      int vampOutGoal, final int stepCount, final String responseText) {
    vampOutGoal = vampOutGoal - 1;

    if ((vampOutGoal < 0 || vampOutGoal >= VampOutScript.length)
        || (stepCount < 0 || stepCount >= VampOutScript[vampOutGoal].length())) {
      return "0";
    }

    String decision = "0";

    if (stepCount == 0 && responseText.indexOf("Finally, the sun has set.") != -1) {
      boolean vladAvailable = responseText.indexOf("Visit Vlad's Boutique") != -1;
      boolean isabellaAvailable = responseText.indexOf("Visit Isabella's") != -1;
      boolean masqueradeAvailable = responseText.indexOf("Visit The Masquerade") != -1;

      Preferences.setBoolean("_interviewVlad", !vladAvailable);
      Preferences.setBoolean("_interviewIsabella", !isabellaAvailable);
      Preferences.setBoolean("_interviewMasquerade", !masqueradeAvailable);

      // If none of the selections are available, select the first option.
      // This only occurs when using an Interview With You (a Vampire) a 4th time.
      if (!vladAvailable && !isabellaAvailable && !masqueradeAvailable) {
        return "1";
      }

      int vladChoice = vladAvailable ? 1 : 0;
      int isabellaChoice = isabellaAvailable ? 2 - (vladAvailable ? 0 : 1) : 0;
      int masqueradeChoice =
          masqueradeAvailable ? 3 - (isabellaAvailable ? 0 : 1) - (vladAvailable ? 0 : 1) : 0;

      logText("Encounter: Interview With You - Goal " + VampOutGoals[vampOutGoal].getName());

      switch (vampOutGoal) {
        case 0, 1, 2 -> decision = Integer.toString(vladChoice);
        case 3, 4, 5, 6 -> decision = Integer.toString(isabellaChoice);
        case 7, 8, 9, 10, 11, 12 -> decision = Integer.toString(masqueradeChoice);
      }
    } else {
      decision = VampOutScript[vampOutGoal].substring(stepCount, stepCount + 1);

      decision =
          switch (decision.charAt(0)) {
            case 'b' -> ChoiceUtilities.findChoiceDecisionIndex(BROUHAHA, responseText);
            case 'm' -> ChoiceUtilities.findChoiceDecisionIndex(MALKOVICH, responseText);
            case 't' -> ChoiceUtilities.findChoiceDecisionIndex(TORREMOLINOS, responseText);
            case 'v' -> ChoiceUtilities.findChoiceDecisionIndex(VENTRILO, responseText);
            default -> decision;
          };
    }

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

  public static final void postChoiceVampOut(final String responseText) {
    // Make sure this is the final page, it won't have any choice selections
    if (responseText.indexOf("choiceform") == -1) {
      if (responseText.indexOf("famspirit.gif") != -1
          || responseText.indexOf("bat.gif") != -1
          || responseText.indexOf("wolfmask.gif") != -1) {
        Preferences.setBoolean("_interviewVlad", true);
      }

      if (responseText.indexOf("You gain ") != -1) {
        Preferences.setBoolean("_interviewIsabella", true);
      }

      if (responseText.indexOf("crown.gif") != -1
          || responseText.indexOf("vampirefangs.gif") != -1
          || responseText.indexOf("heart.gif") != -1) {
        Preferences.setBoolean("_interviewMasquerade", true);
      }
    }
  }

  private static String currentGoalString() {
    int goal = Preferences.getInteger("choiceAdventure546") - 1;

    if (goal < 0 || goal >= VampOutGoals.length) {
      return null;
    }

    return VampOutGoals[goal].getName();
  }

  public static final void addGoalButton(final StringBuffer buffer) {
    // Only add the goal button to the first choice
    if (buffer.indexOf("Finally, the sun has set.") > -1) {
      boolean vladAvailable = !Preferences.getBoolean("_interviewVlad");
      boolean isabellaAvailable = !Preferences.getBoolean("_interviewIsabella");
      boolean masqueradeAvailable = !Preferences.getBoolean("_interviewMasquerade");

      if (vladAvailable || isabellaAvailable || masqueradeAvailable) {
        String goal = VampOutManager.currentGoalString();
        if (goal != null) {
          ChoiceManager.addGoalButton(buffer, goal);
        }
      }
    }
  }
}
