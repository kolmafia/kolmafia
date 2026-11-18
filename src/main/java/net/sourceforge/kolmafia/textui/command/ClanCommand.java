package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;

public class ClanCommand extends AbstractCommand {
  public ClanCommand() {
    this.usage = " [ snapshot | stashlog ] - clan management.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    switch (parameters) {
      case "" -> {
        UserInterfaceManager.getUserInterface().showWindow("ClanManageFrame");
        return;
      }
      case "snapshot" -> ClanManager.takeSnapshot(20, 10, 5, 0, false, true);
      case "stashlog" -> ClanManager.saveStashLog();
    }
  }
}
