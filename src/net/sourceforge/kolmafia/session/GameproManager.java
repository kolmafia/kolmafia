package net.sourceforge.kolmafia.session;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.preferences.Preferences;

public class GameproManager {
  private static final Pattern MAZE_PATTERN =
      Pattern.compile(
          "You will start out facing(?:\\s)?(\\w+)\\.(?:\\s)?You should go(?:\\s)?(\\w+),(?:\\s)?(\\w+),(?:\\s)?(\\w+),(?:\\s)?(\\w+),(?:\\s)?(\\w+),");

  private GameproManager() {}

  public static void parseGameproMagazine(String responseText) {
    if (!responseText.contains("Section I: LEGAL STUFF")) {
      return;
    }

    // How Does a Floating Platform Even Work?
    Preferences.setInteger(
        "choiceAdventure659",
        responseText.contains("moving away from you")
            ? 1
            : responseText.contains("is coming toward you")
                ? 2
                : responseText.contains("same height as the one you're on") ? 3 : 0);

    // It's a Place Where Books Are Free
    Preferences.setInteger(
        "choiceAdventure660",
        responseText.contains("bookcase")
            ? 1
            : responseText.contains("candlesticks")
                ? 2
                : responseText.contains("fireplace") ? 3 : 0);

    // Sphinx For the Memories
    Preferences.setInteger(
        "choiceAdventure661",
        responseText.contains("\"time\"")
            ? 1
            : responseText.contains("\"a mirror\"")
                ? 2
                : responseText.contains("\"hope\"") ? 3 : 0);

    // Think or Thwim
    Preferences.setInteger(
        "choiceAdventure662",
        responseText.contains("swim fins")
            ? 1
            : responseText.contains("make a raft")
                ? 2
                : responseText.contains("into the water") ? 3 : 0);

    // When You're a Stranger
    Preferences.setInteger(
        "choiceAdventure663",
        responseText.contains("first door")
            ? 1
            : responseText.contains("second door")
                ? 2
                : responseText.contains("third door") ? 3 : 0);

    StringBuilder mazePreference = new StringBuilder(9);

    Matcher mazeMatcher = GameproManager.MAZE_PATTERN.matcher(responseText);
    if (mazeMatcher.find()) {
      mazePreference.append(
          GameproManager.compareDirections(mazeMatcher.group(1), mazeMatcher.group(2)));
      mazePreference.append(",");
      mazePreference.append(
          GameproManager.compareDirections(mazeMatcher.group(2), mazeMatcher.group(3)));
      mazePreference.append(",");
      mazePreference.append(
          GameproManager.compareDirections(mazeMatcher.group(3), mazeMatcher.group(4)));
      mazePreference.append(",");
      mazePreference.append(
          GameproManager.compareDirections(mazeMatcher.group(4), mazeMatcher.group(5)));
      mazePreference.append(",");
      mazePreference.append(
          GameproManager.compareDirections(mazeMatcher.group(5), mazeMatcher.group(6)));

      Preferences.setString("choiceAdventure665", mazePreference.toString());
    }
    // Boss special power
    String bossPower = "Undetermined";
    if (responseText.contains("is totally immune to ice-element attacks")) {
      bossPower = "Cold immunity";
    } else if (responseText.contains("evil black heart deals ice damage just by being near")) {
      bossPower = "Cold aura";
    } else if (responseText.contains(
        "is 100 percent immune to fire, so don't even bother wasting your MP on that Flame Cloud")) {
      bossPower = "Hot immunity";
    } else if (responseText.contains("throne room is super hot inside, and")) {
      bossPower = "Hot aura";
    } else if (responseText.contains("because he ignores a lot of your armor")) {
      bossPower = "Ignores armor";
    } else if (responseText.contains("doesn't stand around passively")) {
      bossPower = "Blocks combat items";
    } else if (responseText.contains("is pretty strong against regular attacks, ")) {
      bossPower = "Reduced physical damage";
    } else if (responseText.contains("is resistant to spells, and takes less damage from them")) {
      bossPower = "Reduced damage from spells";
    } else if (responseText.contains(
        "is totally stun-resistant. Yeah, like, don't even bother trying it.")) {
      bossPower = "Stun resistance";
    } else if (responseText.contains("elemental alignment is totally neutral, so")) {
      bossPower = "Elemental Resistance";
    } else if (responseText.contains("spiky armor will do damage to you if you try to hit him i")) {
      bossPower = "Passive damage";
    }
    Preferences.setString("gameProBossSpecialPower", bossPower);
  }

  private static String compareDirections(String direction1, String direction2) {
    return switch (direction1) {
      case "north" -> switch (direction2) {
        case "west" -> "1";
        case "north" -> "2";
        case "east" -> "3";
        default -> "0";
      };
      case "east" -> switch (direction2) {
        case "north" -> "1";
        case "east" -> "2";
        case "south" -> "3";
        default -> "0";
      };
      case "south" -> switch (direction2) {
        case "east" -> "1";
        case "south" -> "2";
        case "west" -> "3";
        default -> "0";
      };
      case "west" -> switch (direction2) {
        case "south" -> "1";
        case "west" -> "2";
        case "north" -> "3";
        default -> "0";
      };
      default -> "0";
    };
  }

  public static String autoSolve(int stepCount) {
    String[] choices = Preferences.getString("choiceAdventure665").split(",");
    if (stepCount < 0 || stepCount > choices.length - 1) {
      // Something went wrong, hand it over for manual control
      return "0";
    }
    return choices[stepCount];
  }

  public static void addGoalButton(final StringBuffer buffer) {
    // Only add the goal button to the first choice
    if (buffer.indexOf("swim down the tube") != -1) {
      ChoiceManager.addGoalButton(buffer, "Exit the maze");
    }
  }
}
