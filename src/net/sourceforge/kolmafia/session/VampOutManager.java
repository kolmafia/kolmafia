package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;

public class VampOutManager {
  public static final String[] VampOutGoals = {
    // Visit Vlad's Boutique
    "Mistified",
    "Bat Attitude",
    "There Wolf",

    // Visit Isabella's
    "Muscle",
    "Mysticality",
    "Moxie",
    "111 Meat, lose 1-2 hp",

    // Visit The Masquerade
    "Prince of Seaside Town and Sword of the Brouhaha Prince",
    "Prince of Seaside Town and Sceptre of the Torremolinos Prince",
    "Prince of Seaside Town and Medallion of the Ventrilo Prince",
    "Prince of Seaside Town and Chalice of the Malkovich Prince",
    "Pride of the Vampire and Interview With You (a Vampire)",
    "your own black heart"
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

      logText("Encounter: Interview With You - Goal " + VampOutGoals[vampOutGoal]);

      switch (vampOutGoal) {
        case 0:
        case 1:
        case 2:
          decision = Integer.toString(vladChoice);
          break;
        case 3:
        case 4:
        case 5:
        case 6:
          decision = Integer.toString(isabellaChoice);
          break;
        case 7:
        case 8:
        case 9:
        case 10:
        case 11:
        case 12:
          decision = Integer.toString(masqueradeChoice);
          break;
      }
    } else {
      decision = VampOutScript[vampOutGoal].substring(stepCount, stepCount + 1);

      switch (decision.charAt(0)) {
        case 'b':
          decision = ChoiceManager.findChoiceDecisionIndex(BROUHAHA, responseText);
          break;
        case 'm':
          decision = ChoiceManager.findChoiceDecisionIndex(MALKOVICH, responseText);
          break;
        case 't':
          decision = ChoiceManager.findChoiceDecisionIndex(TORREMOLINOS, responseText);
          break;
        case 'v':
          decision = ChoiceManager.findChoiceDecisionIndex(VENTRILO, responseText);
          break;
      }
    }

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

    return VampOutGoals[goal];
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
