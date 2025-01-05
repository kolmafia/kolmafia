package net.sourceforge.kolmafia.request.concoction.shop;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;

public class KiwiKwikiMartRequest extends CreateItemRequest {
  public KiwiKwikiMartRequest(final Concoction conc) {
    super("shop.php", conc);

    this.addFormField("whichshop", "kiwi");
    this.addFormField("action", "buyitem");
    int row = ConcoctionPool.idToRow(this.getItemId());
    this.addFormField("whichrow", String.valueOf(row));
  }

  @Override
  public void run() {
    // Attempt to retrieve the ingredients
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
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, "Buying from Kiwi Kwiki Mart was unsuccessful.");
      return;
    }

    KiwiKwikiMartRequest.parseResponse(urlString, responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=kiwi")) {
      return;
    }

    NPCPurchaseRequest.parseShopRowResponse(urlString, responseText);

    if (!urlString.contains("ajax=1")) {
      Preferences.setBoolean(
          "_miniKiwiIntoxicatingSpiritsBought",
          !responseText.contains("mini kiwi intoxicating spirits"));
    }
    if (responseText.contains("Kingdom regulations prevent the purchase")) {
      Preferences.setBoolean("_miniKiwiIntoxicatingSpiritsBought", true);
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=kiwi")) {
      return false;
    }

    return NPCPurchaseRequest.registerShopRowRequest(urlString);
  }
}
