package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.request.LoginRequest;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;

public class PripheaCommand extends AbstractCommand {
  public PripheaCommand() {
    this.usage = " - launch KoLmafia GUI.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (LoginRequest.completedLogin()) {
      UserInterfaceManager.getUserInterface().showMainWindows();
    } else {
      UserInterfaceManager.getUserInterface().showLoginWindow();
    }
  }
}
