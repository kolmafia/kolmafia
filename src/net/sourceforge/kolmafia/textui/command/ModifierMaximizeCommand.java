package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.maximizer.Maximizer;

public class ModifierMaximizeCommand extends AbstractCommand {
  public ModifierMaximizeCommand() {
    this.usage = "[?] [+|-|<weight>] <keyword>, ... - run the Modifier Maximizer.";
  }

  @Override
  public void run(final String command, final String parameters) {
    boolean isSpeculateOnly = KoLmafiaCLI.isExecutingCheckOnlyCommand;

    if (!isSpeculateOnly) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(command + " " + parameters);
    }

    if (!Maximizer.maximize(parameters, 0, 0, isSpeculateOnly) && !isSpeculateOnly) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "Unable to meet all requirements via equipment changes.");
      RequestLogger.printLine("See the Modifier Maximizer for further suggestions.");
    }
  }
}
