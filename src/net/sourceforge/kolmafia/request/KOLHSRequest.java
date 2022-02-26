package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;

public class KOLHSRequest extends CreateItemRequest {
  public static final boolean isKOLHSLocation(final int adventureId) {
    switch (adventureId) {
      case AdventurePool.THE_HALLOWED_HALLS:
      case AdventurePool.SHOP_CLASS:
      case AdventurePool.CHEMISTRY_CLASS:
      case AdventurePool.ART_CLASS:
        return true;
    }
    return false;
  }

  private static String getShopId(final Concoction conc) {
    switch (conc.getMixingMethod()) {
      case CHEMCLASS:
        return "kolhs_chem";
      case ARTCLASS:
        return "kolhs_art";
      case SHOPCLASS:
        return "kolhs_shop";
      default:
        return "";
    }
  }

  private static String shopIDToClassName(final String shopID) {
    if (shopID == null) return null;
    switch (shopID) {
      case "kolhs_chem":
        return "Chemistry Class";
      case "kolhs_art":
        return "Art Class";
      case "kolhs_shop":
        return "Shop Class";
      default:
        return shopID;
    }
  }

  public KOLHSRequest(final Concoction conc) {
    super("shop.php", conc);
    this.addFormField("whichshop", KOLHSRequest.getShopId(conc));
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

    String shopID = NPCPurchaseRequest.getShopId(this.getURLString());
    String className = KOLHSRequest.shopIDToClassName(shopID);
    KoLmafia.updateDisplay("Visit the " + className + " after school to make that.");
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    if (urlString.contains("action=buyitem") && !responseText.contains("You acquire")) {
      String shopID = NPCPurchaseRequest.getShopId(urlString);
      String className = KOLHSRequest.shopIDToClassName(shopID);
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, className + " creation was unsuccessful.");
      return;
    }

    KOLHSRequest.parseResponse(urlString, responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=kolhs_")) {
      return;
    }

    NPCPurchaseRequest.parseShopRowResponse(urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=kolhs_")) {
      return false;
    }

    return NPCPurchaseRequest.registerShopRowRequest(urlString);
  }
}
