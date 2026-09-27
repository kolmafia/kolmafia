package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.moods.RecoveryManager;

public class ManaRestoreCountCommand extends AbstractCommand {
  public ManaRestoreCountCommand() {
    this.usage = " - counts MP restoratives in inventory.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int restores = RecoveryManager.getRestoreCount();
    RequestLogger.printLine(restores + " mana restores remaining.");
  }
}
