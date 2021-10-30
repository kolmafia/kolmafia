package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class SaberCommand extends AbstractCommand {
  public SaberCommand() {
    this.usage = " [mp | ml | resistance | familiar] - upgrade your saber";
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!InventoryManager.hasItem(ItemPool.FOURTH_SABER)
        && !KoLCharacter.hasEquipped(ItemPool.FOURTH_SABER)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need a Fourth of May Cosplay Saber first.");
      return;
    }

    if (Preferences.getInteger("_saberMod") != 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have already upgraded your saber today.");
      return;
    }

    parameter = parameter.trim();

    if (parameter.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Which upgrade do you want to make?");
      return;
    }

    Integer upgrade = null;
    if (parameter.equals("mp")) {
      upgrade = 1;
    } else if (parameter.equals("ml")) {
      upgrade = 2;
    } else if ("resistance".startsWith(parameter)) {
      upgrade = 3;
    } else if ("familiar".startsWith(parameter)) {
      upgrade = 4;
    }
    if (upgrade == null) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what upgrade " + parameter + " is.");
      return;
    }

    KoLmafia.updateDisplay("Upgrading saber");

    GenericRequest request = new GenericRequest("main.php?action=may4", false);
    RequestThread.postRequest(request);

    request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1386");
    request.addFormField("option", Integer.toString(upgrade));
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);

    KoLCharacter.updateStatus();
  }
}
