package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.request.ShrineRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HallOfLegendsCommand extends AbstractCommand {
  public HallOfLegendsCommand() {
    this.usage = " boris | mus | jarl | mys | pete | mox <amount> - donate in Hall of Legends.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    int heroId;
    int amount = -1;

    String[] parameterList = parameters.split(" ");

    if (parameterList[0].startsWith("boris") || parameterList[0].startsWith("mus")) {
      heroId = ShrineRequest.BORIS;
    } else if (parameterList[0].startsWith("jarl") || parameterList[0].startsWith("mys")) {
      heroId = ShrineRequest.JARLSBERG;
    } else if (parameterList[0].startsWith("pete") || parameterList[0].startsWith("mox")) {
      heroId = ShrineRequest.PETE;
    } else {
      KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " is not a statue.");
      return;
    }

    amount = StringUtilities.parseInt(parameterList[1]);
    KoLmafia.updateDisplay("Donating " + amount + " to the shrine...");
    RequestThread.postRequest(new ShrineRequest(heroId, amount));
  }
}
