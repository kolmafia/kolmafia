package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;

public class TryStatement extends ConditionalStatement {
  public TryStatement() {
    this.usage = " ; <commands> - do commands, and continue even if an error occurs.";
  }

  @Override
  public void run(final String command, final String parameters) {
    if (!parameters.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Condition not allowed for try.");
      return;
    }
    KoLmafiaCLI CLI = this.CLI;
    CLI.elseInvalid();
    CLI.executeLine(this.continuation);
    if (KoLmafia.permitsContinue()) {
      CLI.elseRuns(false);
    } else {
      KoLmafia.forceContinue();
      CLI.elseRuns(true);
    }
  }
}
