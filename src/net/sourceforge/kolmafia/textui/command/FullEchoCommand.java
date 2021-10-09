package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FullEchoCommand extends AbstractCommand {
  {
    this.usage = " <text> - include text in the session log.";
    this.flags = KoLmafiaCLI.FULL_LINE_CMD;
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = StringUtilities.globalStringDelete(parameters, "\r");
    parameters = StringUtilities.globalStringDelete(parameters, "\n");
    parameters = StringUtilities.globalStringReplace(parameters, "<", "&lt;");

    RequestLogger.printLine(parameters);
    RequestLogger.getSessionStream().println(" > " + parameters);
  }
}
