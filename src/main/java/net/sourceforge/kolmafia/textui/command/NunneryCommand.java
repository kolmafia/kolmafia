package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.moods.ManaBurnManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.IslandRequest;

public class NunneryCommand extends AbstractCommand {
  public NunneryCommand() {
    this.usage = " [mp] - visit the Nunnery for restoration [but only if MP is restored].";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    NunneryCommand.visit(parameters);
  }

  /** Attempts to get HP or HP/MP restoration from the Nuns at Our Lady of Perpetual Indecision */
  public static void visit(final String parameters) {
    if (Preferences.getInteger("nunsVisits") >= 3) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Nun of the nuns are available right now.");
      return;
    }

    String side = Preferences.getString("sidequestNunsCompleted");
    if (!side.equals("fratboy") && !side.equals("hippy")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have not opened the Nunnery yet.");
      return;
    }

    if (side.equals("hippy") && parameters.equalsIgnoreCase("mp")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Only HP restoration is available from the nuns.");
      return;
    } else if (side.equals("fratboy")) {
      ManaBurnManager.burnMana(KoLCharacter.getMaximumMP() - 1000);
    }

    IslandRequest request = IslandRequest.getNunneryRequest();
    if (request == null) {
      return;
    }

    KoLmafia.updateDisplay("Get thee to a nunnery!");
    RequestThread.postRequest(request);
  }
}
