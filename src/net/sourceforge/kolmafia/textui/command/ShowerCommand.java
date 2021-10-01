package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;

public class ShowerCommand extends AbstractCommand {
  public ShowerCommand() {
    this.usage = " type - take a shower in your clan's VIP lounge";
  }

  @Override
  public void run(final String cmd, String parameters) {
    parameters = parameters.trim();
    if (parameters.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "What temperature should your shower be?");
      return;
    }

    int option;
    option = ClanLoungeRequest.findShowerOption(parameters);
    if (option == 0) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what a '" + parameters + "' shower is.");
      return;
    }

    RequestThread.postRequest(new ClanLoungeRequest(ClanLoungeRequest.APRIL_SHOWER, option));
  }
}
