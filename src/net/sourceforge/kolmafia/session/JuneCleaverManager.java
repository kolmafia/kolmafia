package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.preferences.Preferences;

public class JuneCleaverManager {

  private JuneCleaverManager() {}

  public static final Pattern[] MESSAGES = {
    Pattern.compile(
        "As the battle ends, your cleaver flashes bright <span style=\"color: (?<color>[^\"]+)\""),
    Pattern.compile(
        "Out of the corner of your eye, you catch a glimpse of bright <span style=\"color: (?<color>[^\"]+)\""),
    Pattern.compile("You notice a glint of <span style=\"color: (?<color>[^\"]+)\""),
    Pattern.compile(
        "Your cleaver sparkles with a startling <span style=\"color: (?<color>[^\"]+)\"")
  };

  public static void updatePreferences(String responseText) {
    for (Pattern message : MESSAGES) {
      Matcher matcher = message.matcher(responseText);
      if (matcher.find()) {
        Preferences.decrement("_juneCleaverFightsLeft");
        switch (matcher.group("color")) {
          case "blue":
            Preferences.increment("_juneCleaverCold");
            break;
          case "blueviolet":
            Preferences.increment("_juneCleaverSleaze");
            break;
          case "gray":
            Preferences.increment("_juneCleaverSpooky");
            break;
          case "green":
            Preferences.increment("_juneCleaverStench");
            break;
          case "red":
            Preferences.increment("_juneCleaverHot");
            break;
        }
        return;
      }
    }
  }

  private static final int[] NORMAL_FIGHTS_TO_CHOICE = {1, 6, 10, 12, 15, 20, 30};
  private static final int[] RESET_FIGHTS_TO_CHOICE = {1, 2, 3, 3, 4, 5, 8};

  private static int fightsLeft(boolean skip) {
    int[] fights = skip ? RESET_FIGHTS_TO_CHOICE : NORMAL_FIGHTS_TO_CHOICE;
    int encounters = Preferences.getInteger("_juneCleaverEncounters");
    return fights[Math.min(encounters, fights.length - 1)];
  }

  public static void parseChoice(String urlString) {
    boolean correctChoice = false;
    for (int choice = 1467; choice <= 1475; choice++) {
      if (urlString.contains("whichchoice=" + choice)) {
        correctChoice = true;
        break;
      }
    }
    if (!correctChoice) {
      return;
    }

    if (urlString.contains("option=4")) {
      Preferences.increment("_juneCleaverSkips");
      Preferences.setInteger("_juneCleaverFightsLeft", fightsLeft(true));
    } else if (urlString.contains("option=")) {
      Preferences.increment("_juneCleaverEncounters");
      Preferences.setInteger("_juneCleaverFightsLeft", fightsLeft(false));
    }
  }
}
