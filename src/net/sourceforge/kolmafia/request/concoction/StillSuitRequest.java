package net.sourceforge.kolmafia.request.concoction;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class StillSuitRequest extends CreateItemRequest {
  private static Concoction DISTILLATE = ConcoctionPool.get(1);

  public StillSuitRequest() {
    super("choice.php", DISTILLATE);
  }

  @Override
  public boolean noCreation() {
    return true;
  }

  @Override
  public void run() {
    if (!canMake()) {
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, lastReason);
      return;
    }

    KoLmafia.updateDisplay("Creating 1 stillsuit distillate...");

    RequestThread.postRequest(new GenericRequest("inventory.php?action=distill&pwd", true));
    RequestThread.postRequest(new GenericRequest("choice.php?whichchoice=1476&option=1&pwd"));
    ConcoctionDatabase.refreshConcoctions(false);
  }

  private static String lastReason = null;

  public static boolean canMake() {
    if (!InventoryManager.hasItem(ItemPool.STILLSUIT)
        && !KoLCharacter.hasEquipped(ItemPool.STILLSUIT)) {
      lastReason = "You don't have a tiny stillsuit";
      return false;
    }

    if (Preferences.getInteger("familiarSweat") < 10) {
      lastReason =
          "You need at least 10 drams of familiar sweat to drink the delicious distillate.";
      return false;
    }

    return true;
  }

  public static boolean isDistillate(final String name) {
    return name.equals("stillsuit distillate");
  }
}
