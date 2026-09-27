package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.StaticEntity;

public class EnableCommand extends AbstractCommand {
  public EnableCommand() {
    this.usage = " all | <command> [, <command>]... - allow/deny CLI commands.";
  }

  @Override
  public void run(final String command, final String parameters) {
    if (command.equals("enable")) {
      StaticEntity.enable(parameters.toLowerCase());
      return;
    }

    if (command.equals("disable")) {
      StaticEntity.disable(parameters.toLowerCase());
      return;
    }
  }
}
