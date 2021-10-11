package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants;

public class ClearBufferCommand extends AbstractCommand {
  public ClearBufferCommand() {
    this.usage = " - clear CLI window.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    KoLConstants.commandBuffer.clear();
  }
}
