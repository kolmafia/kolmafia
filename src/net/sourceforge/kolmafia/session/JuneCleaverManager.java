package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.preferences.Preferences;

public class JuneCleaverManager {

  private JuneCleaverManager() {}

  public static ArrayList<Integer> queue = new ArrayList<>();

  private static void updateQueue(int id) {
    String savedQueue = Preferences.getString("juneCleaverQueue");
    if (queue.isEmpty() && savedQueue.length() > 0) {
      for (String x : savedQueue.split(",")) {
        queue.add(Integer.parseInt(x));
      }
    }

    queue.add(id);

    while (queue.size() > 6) {
      queue.remove(0);
    }

    Preferences.setString(
        "juneCleaverQueue", queue.stream().map(x -> x.toString()).collect(Collectors.joining(",")));
  }

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
    Matcher choiceFinder = Pattern.compile("whichchoice=(?<choiceId>\\d+)").matcher(urlString);
    if (!choiceFinder.find()) {
      return;
    }
    int id = Integer.parseInt(choiceFinder.group("choiceId"));

    boolean correctChoice = false;
    for (int choice = 1467; choice <= 1475; choice++) {
      if (choice == id) {
        correctChoice = true;
        break;
      }
    }
    if (!correctChoice) return;

    Matcher optionFinder = Pattern.compile("option=(?<optionNumber>\\d+)").matcher(urlString);
    if (!optionFinder.find()) return;

    updateQueue(id);

    int option = Integer.parseInt(optionFinder.group("optionNumber"));

    if (option == 4) {
      Preferences.increment("_juneCleaverSkips");
      Preferences.setInteger("_juneCleaverFightsLeft", fightsLeft(true));
    } else if (option == 1 || option == 2 || option == 3) {
      Preferences.increment("_juneCleaverEncounters");
      Preferences.setInteger("_juneCleaverFightsLeft", fightsLeft(false));
    }
  }
}
