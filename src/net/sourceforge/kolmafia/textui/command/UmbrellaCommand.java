package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.UmbrellaRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class UmbrellaCommand extends AbstractCommand {
  public UmbrellaCommand() {
    this.usage =
        "[ml | item | dr | weapon | spell | nc | broken | forward | bucket | pitchfork | twirling | cocoon] - fold your Umbrella";
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

    Integer umbrellaState = null;
    if (parameter.equals("ml") || "broken".startsWith(parameter)) {
      umbrellaState = UmbrellaRequest.Form.BROKEN.id;
    } else if (parameter.equals("dr") || "forward".startsWith(parameter)) {
      umbrellaState = UmbrellaRequest.Form.FORWARD.id;
    } else if (parameter.equals("item") || "bucket".startsWith(parameter)) {
      umbrellaState = UmbrellaRequest.Form.BUCKET.id;
    } else if (parameter.equals("weapon") || "pitchfork".startsWith(parameter)) {
      umbrellaState = UmbrellaRequest.Form.PITCHFORK.id;
    } else if (parameter.equals("spell") || "twirling".startsWith(parameter)) {
      umbrellaState = UmbrellaRequest.Form.TWIRL.id;
    } else if (parameter.equals("nc") || "cocoon".startsWith(parameter)) {
      umbrellaState = UmbrellaRequest.Form.COCOON.id;
    }

    if (umbrellaState == null) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "I don't understand what Umbrella form " + parameter + " is.");
      return;
    }

    KoLmafia.updateDisplay("Folding umbrella");
    GenericRequest request = new GenericRequest("inventory.php?action=useumbrella", false);
    RequestThread.postRequest(request);

    request = new GenericRequest("choice.php");
    request.addFormField("whichchoice", "1466");
    request.addFormField("option", Integer.toString(umbrellaState));
    request.addFormField("pwd", GenericRequest.passwordHash);
    RequestThread.postRequest(request);

    KoLCharacter.updateStatus();
  }
}
