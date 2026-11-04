package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.ui.UserInterfaceManager;

public class WindowOpenCommand extends AbstractCommand {
  public WindowOpenCommand() {
    this.usage = " - switch to tab or open window";
  }

  @Override
  public void run(final String command, final String parameters) {
    if (command.equals("chat")) {
      UserInterfaceManager.getUserInterface().showWindow("ChatManager");
      return;
    }

    if (command.startsWith("opt")) {
      UserInterfaceManager.getUserInterface().showWindow("OptionsFrame");
      return;
    }

    if (command.equals("item")) {
      UserInterfaceManager.getUserInterface().showWindow("ItemManageFrame");
      return;
    }

    if (command.equals("gear")) {
      UserInterfaceManager.getUserInterface().showWindow("GearChangeFrame");
      return;
    }
  }
}
