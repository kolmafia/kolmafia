package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class EchoCommand extends AbstractCommand {
  public EchoCommand() {
    this.usage = " timestamp | <text> - include timestamp or text in the session log.";
  }

  @Override
  public void run(final String cmd, String parameters) {
    if (parameters.equalsIgnoreCase("timestamp")) {
      parameters = HolidayDatabase.getCalendarDayAsString();
    }

    parameters = StringUtilities.globalStringDelete(parameters, "\r");
    parameters = StringUtilities.globalStringDelete(parameters, "\n");

    RequestLogger.printLine(parameters);
    RequestLogger.getSessionStream().println(" > " + parameters);
  }
}
