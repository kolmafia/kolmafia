package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.request.BasementRequest;

public class BasementCommand extends AbstractCommand {
  public BasementCommand() {
    this.usage = " - check Fernswarthy's Basement status.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    BasementRequest.checkBasement();
  }
}
