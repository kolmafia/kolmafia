package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.WitchessRequest;
import net.sourceforge.kolmafia.session.WitchessManager;

public class WitchessCommand extends AbstractCommand {
  public WitchessCommand() {
    this.usage = " [buff] | solve - Get the Witchess buff or attempt to solve your daily puzzles";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] split = parameters.split(" ");
    String command = split[0];

    if (!(KoLCharacter.inLegacyOfLoathing()
            && Preferences.getBoolean("replicaWitchessSetAvailable"))
        && !StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Witchess Set")) {
      return;
    }
    if (Preferences.getBoolean("_witchessBuff")) {
      KoLmafia.updateDisplay("You already got your Witchess buff today.");
      return;
    }

    switch (command) {
      case "":
      case "buff":
        if (Preferences.getInteger("puzzleChampBonus") != 20) {
          KoLmafia.updateDisplay(
              "You cannot automatically get a Witchess buff until all puzzles are solved.");
          return;
        }
        RequestThread.postRequest(new WitchessRequest());
        break;
      case "solve":
        WitchessManager.solveDailyPuzzles();
        break;
      default:
        KoLmafia.updateDisplay("I'm not sure what you want me to do with your Witchess Set.");
    }
  }
}
