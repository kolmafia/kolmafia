package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

public class BacktraceCommand extends AbstractCommand {
  public BacktraceCommand() {
    this.usage =
        " <text> | off - dump stack when a gCLI message or page URL matches text (case-sensitive).";
  }

  @Override
  public void run(final String command, final String parameters) {
    if (parameters.length() > 3) {
      RequestLogger.printLine("Backtrace trigger set.");
      StaticEntity.backtraceTrigger = parameters;
    } else {
      StaticEntity.backtraceTrigger = null;
      RequestLogger.printLine("Backtrace trigger cleared.");
    }
  }
}
