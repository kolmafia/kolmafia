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
        Preferences.increment("_juneCleaverCharge");
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
}
