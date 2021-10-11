package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public class PixelRequest extends CreateItemRequest {
  public PixelRequest(final Concoction conc) {
    super("shop.php", conc);

    this.addFormField("whichshop", "mystic");
    this.addFormField("action", "buyitem");
    int row = ConcoctionPool.idToRow(this.getItemId());
    this.addFormField("whichrow", String.valueOf(row));
  }

  @Override
  public void run() {
    int itemId = this.createdItem.getItemId();
    if (itemId == ItemPool.DIGITAL_KEY && InventoryManager.hasItem(itemId)) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR, "You can only have one digital key and you already have one.");
      return;
    }

    // Attempting to make the ingredients will pull the
    // needed items from the closet if they are missing.
    // In this case, it will also create the needed white
    // pixels if they are not currently available.

    if (!this.makeIngredients()) {
      return;
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + this.getName() + "...");
    this.addFormField("quantity", String.valueOf(this.getQuantityNeeded()));
    super.run();
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    if (urlString.contains("action=buyitem") && !responseText.contains("You acquire")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Mystic shopping was unsuccessful.");
      return;
    }

    PixelRequest.parseResponse(urlString, responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=mystic")) {
      return;
    }

    NPCPurchaseRequest.parseShopRowResponse(urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=mystic")) {
      return false;
    }

    return NPCPurchaseRequest.registerShopRowRequest(urlString);
  }
}
