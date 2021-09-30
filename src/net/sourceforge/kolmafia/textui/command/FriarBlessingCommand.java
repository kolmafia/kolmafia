package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.FriarRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FriarBlessingCommand extends AbstractCommand {
  public FriarBlessingCommand() {
    this.usage = " [blessing] food | familiar | booze - get daily blessing.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] split = parameters.split(" ");
    String command = null;

    if (split.length == 2 && split[0].equals("blessing")) {
      command = split[1];
    } else if (split.length == 1 && !split[0].equals("")) {
      command = split[0];
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Syntax: friars [blessing] food|familiar|booze");
      return;
    }

    int action = 0;

    if (Character.isDigit(command.charAt(0))) {
      action = StringUtilities.parseInt(command);
    } else {
      for (int i = 0; i < FriarRequest.BLESSINGS.length; ++i) {
        if (command.equalsIgnoreCase(FriarRequest.BLESSINGS[i])) {
          action = i + 1;
          break;
        }
      }
    }

    if (action < 1 || action > 3) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Syntax: friars [blessing] food|familiar|booze");
      return;
    }

    RequestThread.postRequest(new FriarRequest(action));
  }
}
