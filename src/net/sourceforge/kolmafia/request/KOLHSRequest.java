package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;

public class KOLHSRequest extends CreateItemRequest {
  public static final boolean isKOLHSLocation(final int adventureId) {
    return switch (adventureId) {
      case AdventurePool.THE_HALLOWED_HALLS,
          AdventurePool.SHOP_CLASS,
          AdventurePool.CHEMISTRY_CLASS,
          AdventurePool.ART_CLASS -> true;
      default -> false;
    };
  }

  private static String getShopId(final Concoction conc) {
    return switch (conc.getMixingMethod()) {
      case CHEMCLASS -> "kolhs_chem";
      case ARTCLASS -> "kolhs_art";
      case SHOPCLASS -> "kolhs_shop";
      default -> "";
    };
  }

  private static String shopIDToClassName(final String shopID) {
    if (shopID == null) return null;
    return switch (shopID) {
      case "kolhs_chem" -> "Chemistry Class";
      case "kolhs_art" -> "Art Class";
      case "kolhs_shop" -> "Shop Class";
      default -> shopID;
    };
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
