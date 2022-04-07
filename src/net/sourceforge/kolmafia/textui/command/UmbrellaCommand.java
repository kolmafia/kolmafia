package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class UmbrellaCommand extends AbstractCommand {
  public UmbrellaCommand() {
    this.usage = "[ml | item | dr | weapon | spell | nc] - fold your Umbrella";
  }

  @Override
  public void run(final String cmd, String parameter) {
    if (!InventoryManager.hasItem(ItemPool.UNBREAKABLE_UMBRELLA)
        && !KoLCharacter.hasEquipped(ItemPool.UNBREAKABLE_UMBRELLA)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You need an Unbreakable Umbrella first.");
      return;
    }

    parameter = parameter.trim();

    if (parameter.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "What state do you want to fold your umbrella to?");
      return;
    }

    Integer umbrellaForm = null;
    if (parameter.equals("ml")) {
      umbrellaForm = 1;
    } else if (parameter.equals("dr")) {
      umbrellaForm = 2;
    } else if (parameter.equals("item")) {
      umbrellaForm = 3;
    } else if (parameter.equals("weapon")) {
      umbrellaForm = 4;
    } else if (parameter.equals("spell")) {
      umbrellaForm = 5;
    } else if (parameter.equals("nc")) {
      umbrellaForm = 6;
    }

    if (umbrellaForm == null) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what Umbrella form " + parameter + " is.");
      return;
    }

    KoLmafia.updateDisplay("Folding umbrella");
    GenericRequest request = new GenericRequest("inventory.php?action=useumbrella", false);
    RequestThread.postRequest(request);

    request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1466");
    request.addFormField("option", Integer.toString(umbrellaForm));
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);

    KoLCharacter.updateStatus();
  }
}
