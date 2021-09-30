package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public class SausageOMaticRequest extends CreateItemRequest {
  public SausageOMaticRequest(final Concoction conc) {
    super("choice.php", conc);
  }

  @Override
  public void run() {
    if (!KoLmafia.permitsContinue() || this.getQuantityNeeded() <= 0) {
      return;
    }

    String creation = this.getName();

    if (!creation.equals("magical sausage")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Cannot create " + creation);
      return;
    }

    int quantityNeeded = this.getQuantityNeeded();
    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + creation + "...");

    int meatNeeded = 0;
    int count = 0;
    int currentSausages = this.createdItem.getCount(KoLConstants.inventory);
    int sausagesMade = Preferences.getInteger("_sausagesMade");
    int grinderUnits = Preferences.getInteger("sausageGrinderUnits");

    if (((sausagesMade + quantityNeeded) > 23) && (this.confirmSausages() == false)) {
      return;
    }

    // Work out total meat cost to make requested sausages
    while (count < quantityNeeded && KoLmafia.permitsContinue()) {
      meatNeeded = meatNeeded + (sausagesMade + 1 + count) * 111;
      count++;
    }

    // Work out meat stacks needed to make requested sausages
    int denseStacksNeeded = (int) Math.floor((meatNeeded - grinderUnits) / 1000);
    int stacksNeeded =
        (int) Math.floor((meatNeeded - grinderUnits - denseStacksNeeded * 1000) / 100);
    int pasteNeeded =
        (int)
            Math.ceil(
                (double) (meatNeeded - grinderUnits - denseStacksNeeded * 1000 - stacksNeeded * 100)
                    / 10);

    KoLmafia.updateDisplay(
        "Meat needed: "
            + Math.max(meatNeeded - grinderUnits, 0)
            + ", Dense: "
            + Math.max(denseStacksNeeded, 0)
            + ", Stacks: "
            + Math.max(stacksNeeded, 0)
            + ", Paste: "
            + Math.max(pasteNeeded, 0));

    InventoryManager.retrieveItem(ItemPool.DENSE_STACK, denseStacksNeeded);
    InventoryManager.retrieveItem(ItemPool.MEAT_STACK, stacksNeeded);
    InventoryManager.retrieveItem(ItemPool.MEAT_PASTE, pasteNeeded);
    InventoryManager.retrieveItem(ItemPool.MAGICAL_SAUSAGE_CASING, quantityNeeded);

    GenericRequest request = new GenericRequest("inventory.php?action=grind");
    RequestThread.postRequest(request);

    if (denseStacksNeeded > 0) {
      String url =
          "choice.php?whichchoice=1339&option=1&qty="
              + denseStacksNeeded
              + "&iid="
              + ItemPool.DENSE_STACK;
      request.constructURLString(url);
      RequestThread.postRequest(request);
    }
    if (stacksNeeded > 0) {
      String url =
          "choice.php?whichchoice=1339&option=1&qty="
              + stacksNeeded
              + "&iid="
              + ItemPool.MEAT_STACK;
      request.constructURLString(url);
      RequestThread.postRequest(request);
    }
    if (pasteNeeded > 0) {
      String url =
          "choice.php?whichchoice=1339&option=1&qty=" + pasteNeeded + "&iid=" + ItemPool.MEAT_PASTE;
      request.constructURLString(url);
      RequestThread.postRequest(request);
    }
    while (this.getQuantityNeeded() > 0) {
      this.beforeQuantity = this.createdItem.getCount(KoLConstants.inventory);
      String url = "choice.php?whichchoice=1339&option=2";
      request.constructURLString(url);
      RequestThread.postRequest(request);
      int createdQuantity = this.createdItem.getCount(KoLConstants.inventory) - this.beforeQuantity;
      if (createdQuantity == 0) {
        if (KoLmafia.permitsContinue()) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Creation failed, no results detected.");
        }

        return;
      }
      this.quantityNeeded -= createdQuantity;
    }
  }

  private final boolean confirmSausages() {
    if (!GenericFrame.instanceExists()) {
      return true;
    }

    return InputFieldUtilities.confirm(
        "Are you sure you want to make more than 23 sausages in one day?");
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("choice.php") || !urlString.contains("whichchoice=1339")) {
      return false;
    }

    return true;
  }
}
