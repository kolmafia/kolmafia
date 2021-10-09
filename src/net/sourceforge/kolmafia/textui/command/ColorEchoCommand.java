package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ColorEchoCommand extends AbstractCommand {
  public ColorEchoCommand() {
    this.usage = " <color> <text> - show text using color (specified by name or #RRGGBB).";
  }

  @Override
  public void run(final String cmd, String parameters) {
    int spaceIndex = parameters.indexOf(" ");
    String color = "#000000";

    if (spaceIndex != -1) {
      color = parameters.substring(0, spaceIndex).replaceAll("[\">]", "");
    }

    parameters = parameters.substring(spaceIndex + 1);
    StringUtilities.globalStringReplace(parameters, "<", "&lt;");
    RequestLogger.printLine("<font color=\"" + color + "\">" + parameters + "</font>");
  }
}
