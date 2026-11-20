package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;

public class WindowOpenCommand extends AbstractCommand {
  public WindowOpenCommand() {
    this.usage = " - switch to tab or open window";
  }

  @Override
  public void run(final String command, final String parameters) {
    if (command.equals("chat")) {
      KoLmafia.updateDisplay("Initializing chat interface...");
      ChatManager.initialize();
      return;
    }

    if (command.startsWith("opt")) {
      UserInterfaceManager.get().showWindow("OptionsFrame");
      return;
    }

    if (command.equals("item")) {
      UserInterfaceManager.get().showWindow("ItemManageFrame");
      return;
    }

    if (command.equals("gear")) {
      UserInterfaceManager.get().showWindow("GearChangeFrame");
      return;
    }
  }
}
