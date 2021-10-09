package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.session.ClanManager;

public class ClanCommand extends AbstractCommand {
  public ClanCommand() {
    this.usage = " [ snapshot | stashlog ] - clan management.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.equals("")) {
      KoLmafiaGUI.constructFrame("ClanManageFrame");
      return;
    } else if (parameters.equals("snapshot")) {
      ClanManager.takeSnapshot(20, 10, 5, 0, false, true);
    } else if (parameters.equals("stashlog")) {
      ClanManager.saveStashLog();
    }
  }
}
