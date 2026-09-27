package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaCLI;

public class WhileStatement extends ConditionalStatement {
  public WhileStatement() {
    this.usage = " <condition>; <commands> - do commands repeatedly while condition is true.";
  }

  @Override
  public void run(final String command, final String parameters) {
    // must make local copies since the executed commands could overwrite these
    KoLmafiaCLI CLI = this.CLI;
    String continuation = this.continuation;

    CLI.elseRuns(true);
    while (ConditionalStatement.test(parameters)) {
      CLI.elseInvalid();
      CLI.executeLine(continuation);
      CLI.elseRuns(false);
    }
  }
}
