package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CountersCommand extends AbstractCommand {
  public CountersCommand() {
    this.usage =
        " [ clear | add <number> [<title> <img>] | stop <title> | warn <title> | nowarn <title> ] - show, clear, or add to current turn counters,"
            + " or set an existing counter to (not) warn you when it expires.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (parameters.equalsIgnoreCase("clear")) {
      TurnCounter.clearCounters();
      return;
    }

    if (parameters.startsWith("deletehash ")) {
      TurnCounter.deleteByHash(StringUtilities.parseInt(parameters.substring(11)));
      return;
    }

    if (parameters.startsWith("add ")) {
      String title = "Manual";
      String image = "watch.gif";
      parameters = parameters.substring(4).trim();
      if (parameters.endsWith(".gif")) {
        int lastSpace = parameters.lastIndexOf(" ");
        image = parameters.substring(lastSpace + 1);
        parameters = parameters.substring(0, lastSpace + 1).trim();
      }
      int spacePos = parameters.indexOf(" ");
      if (spacePos != -1) {
        title = parameters.substring(spacePos + 1);
        parameters = parameters.substring(0, spacePos).trim();
      }

      TurnCounter.startCounting(StringUtilities.parseInt(parameters), title, image);
    } else if (parameters.startsWith("stop ")) {
      parameters = parameters.substring(5).trim();
      TurnCounter.stopCounting(parameters);
    } else if (parameters.startsWith("warn ")) {
      parameters = parameters.substring(5).trim();
      TurnCounter.addWarning(parameters);
    } else if (parameters.startsWith("nowarn ")) {
      parameters = parameters.substring(7).trim();
      TurnCounter.removeWarning(parameters);
    }

    ShowDataCommand.show("counters");
  }
}
