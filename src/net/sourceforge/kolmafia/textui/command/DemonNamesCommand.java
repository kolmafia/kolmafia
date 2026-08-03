package net.sourceforge.kolmafia.textui.command;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.DemonName14Manager;

public class DemonNamesCommand extends AbstractCommand {
  {
    this.usage =
        " <blank> | solve14 - With no param, list the demon names you know. With solve14, attempt to solve the 14th demon.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.trim().equals("solve14")) {
      var prefValue = Preferences.getString("demonName14Segments");

      if (prefValue.isEmpty()) {
        RequestLogger.printLine(
            "You need to make bad requests with your Allied Radio Backpack to find segments of your demon name");
        return;
      }

      var segments =
          Arrays.stream(prefValue.split(","))
              .map(s -> s.split(":", 2))
              .map(arr -> Map.entry(arr[0], Integer.parseInt(arr.length > 1 ? arr[1] : "1")))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
              .keySet();

      RequestLogger.printLine(
          "Attempting to solve your demon name with "
              + segments.size()
              + " segment(s). This may take a while...");

      var solutions = DemonName14Manager.solve(segments);

      if (solutions.isEmpty()) {
        RequestLogger.printLine("Sorry, you do not have enough segments to solve your demon name.");
        return;
      }

      if (segments.size() < 10) {
        RequestLogger.printLine(
            "Unless you have a really unfortunate demon name, you might want to try to find more segments before solving it. The solution set may be artificially small.");
        RequestLogger.printLine();
      }

      RequestLogger.printLine(solutions.size() + " solution(s) found:");

      var i = 1;
      for (var solution : solutions) {
        RequestLogger.printLine(i++ + ": " + solution);
      }

      RequestLogger.printLine();
      RequestLogger.printLine("Done! If none of these are correct, try finding more segments.");

      return;
    }

    for (int i = 0; i < KoLAdventure.DEMON_TYPES.length; ++i) {
      String index = String.valueOf(i + 1);

      RequestLogger.printLine(index + ": " + Preferences.getString("demonName" + index));
      if (KoLAdventure.DEMON_TYPES[i][0] != null) {
        RequestLogger.printLine(" => Found in the " + KoLAdventure.DEMON_TYPES[i][0]);
      }
      RequestLogger.printLine(" => Gives " + KoLAdventure.DEMON_TYPES[i][1]);
    }
  }
}
