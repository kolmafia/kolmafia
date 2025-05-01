package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public class ForeseeRequest extends GenericRequest {
  public ForeseeRequest() {
    super("inventory.php?action=foresee&pwd=" + GenericRequest.passwordHash, false);
  }

  public ForeseeRequest(final String target) {
    super("choice.php");
    this.addFormField("whichchoice", "1558");
    this.addFormField("option", "1");
    this.addFormField("who", target);
  }

  public static void throwAt(final String target) {
    RequestThread.postRequest(new ForeseeRequest());
    RequestThread.postRequest(new ForeseeRequest(target));
  }

  @Override
  protected boolean shouldFollowRedirect() {
    return true;
  }

  @Override
  public void run() {
    if (InventoryManager.getAccessibleCount(ItemPool.PERIDOT_OF_PERIL) == 0) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, "You do not own a Peridot of Peril.");
      return;
    }

    if (UseItemRequest.maximumUses(ItemPool.PERIDOT_OF_PERIL) < 1) {
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "You can only foresee peril thrice daily.");
      return;
    }

    // Ensure the Peridot is equipped or in inventory.
    if (InventoryManager.getCount(ItemPool.PERIDOT_OF_PERIL) == 0
        && !KoLCharacter.hasEquipped(ItemPool.PERIDOT_OF_PERIL)) {
      if (!InventoryManager.retrieveItem(ItemPool.COMBAT_LOVERS_LOCKET, 1)) {
        KoLmafia.updateDisplay(
            KoLConstants.MafiaState.ERROR, "Internal error: failed to retrieve Peridot of Peril");
        return;
      }
    }

    if (this.getBasePath().equals("choice.php")) {
      KoLmafia.updateDisplay("Foreseeing peril for " + this.getFormField("who") + "...");
    }

    super.run();
  }
}
