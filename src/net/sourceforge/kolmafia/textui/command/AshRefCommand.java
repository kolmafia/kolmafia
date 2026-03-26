package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;

public class AshRefCommand extends AbstractCommand {
  public AshRefCommand() {
    this.usage = " [<filter>] - summarize ASH built-in functions [matching filter].";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    KoLmafiaASH.Formatting.showFunctions(
        RuntimeLibrary.getFunctions(), parameters.toLowerCase(), true);
  }
}
