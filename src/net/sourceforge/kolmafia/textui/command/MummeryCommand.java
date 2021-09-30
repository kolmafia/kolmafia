package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MummeryRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class MummeryCommand extends AbstractCommand {
  public MummeryCommand() {
    this.usage =
        " [muscle | myst | moxie | hp | mp | item | meat | # ] - put the indicated costume on your familiar";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (!KoLConstants.inventory.contains(ItemPool.get(ItemPool.MUMMING_TRUNK, 1))) {
      KoLmafia.updateDisplay("You need a mumming trunk first.");
      return;
    }
    if (KoLCharacter.currentFamiliar == FamiliarData.NO_FAMILIAR) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need to have a familiar to put a costume on.");
      return;
    }
    int choice = StringUtilities.parseInt(parameters);
    if (choice < 1 || choice > 7) {
      if (parameters.contains("meat")) {
        choice = 1;
      } else if (parameters.contains("mp")) {
        choice = 2;
      } else if (parameters.contains("mus")) {
        choice = 3;
      } else if (parameters.contains("item")) {
        choice = 4;
      } else if (parameters.contains("mys")) {
        choice = 5;
      } else if (parameters.contains("hp")) {
        choice = 6;
      } else if (parameters.contains("mox")) {
        choice = 7;
      }
    }
    if (choice < 1 || choice > 7) {
      KoLmafia.updateDisplay(MafiaState.ERROR, parameters + " is not a valid option.");
      return;
    }
    if (Preferences.getString("_mummeryUses").contains(String.valueOf(choice))) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You have already applied the " + parameters + " costume today.");
      return;
    }
    RequestThread.postRequest(new MummeryRequest(choice));
  }
}
