package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.RaffleRequest;
import net.sourceforge.kolmafia.request.RaffleRequest.RaffleSource;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class RaffleCommand extends AbstractCommand {
  public RaffleCommand() {
    this.usage = " <ticketsToBuy> [ inventory | storage ] - buy raffle tickets";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!KoLCharacter.desertBeachAccessible() || KoLCharacter.inZombiecore()) {
      RequestLogger.printLine("You can't make it to the raffle house");
      return;
    }
    String[] split = parameters.split(" ");
    int count = StringUtilities.parseInt(split[0]);

    if (split.length == 1) {
      RequestThread.postRequest(new RaffleRequest(count));
      return;
    }

    RaffleSource source;

    if (split[1].equals("inventory")) {
      source = RaffleSource.INVENTORY;
    } else if (split[1].equals("storage")) {
      source = RaffleSource.STORAGE;
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You can only get meat from inventory or storage.");
      return;
    }

    RequestThread.postRequest(new RaffleRequest(count, source));
  }
}
