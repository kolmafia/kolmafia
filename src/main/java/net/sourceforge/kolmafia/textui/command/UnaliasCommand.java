package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.persistence.Aliases;

public class UnaliasCommand extends AbstractCommand {
  public UnaliasCommand() {
    this.usage = " <word> - remove a CLI abbreviation.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    Aliases.remove(parameters);
  }
}
