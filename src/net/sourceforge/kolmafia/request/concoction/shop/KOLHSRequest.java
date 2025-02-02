package net.sourceforge.kolmafia.request.concoction.shop;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

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

  private final String shopId;

  public KOLHSRequest(final Concoction conc) {
    super("shop.php", conc);
    this.shopId = KOLHSRequest.getShopId(conc);
    this.addFormField("whichshop", this.shopId);
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

    String className = shopIDToClassName(this.shopId);
    KoLmafia.updateDisplay("Visit the " + className + " after school to make that.");
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    if (urlString.contains("action=buyitem") && !responseText.contains("You acquire")) {
      String className = shopIDToClassName(this.shopId);
      KoLmafia.updateDisplay(
          KoLConstants.MafiaState.ERROR, className + " creation was unsuccessful.");
      return;
    }

    ShopRequest.parseResponse(urlString, responseText);
  }
}
