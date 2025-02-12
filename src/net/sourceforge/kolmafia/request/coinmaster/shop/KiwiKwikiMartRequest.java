package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class KiwiKwikiMartRequest extends CoinMasterShopRequest {
  public static final String master = "Kiwi Kwiki Mart";
  public static final String SHOPID = "kiwi";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "kiwi", KiwiKwikiMartRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withCanBuyItem(KiwiKwikiMartRequest::canBuyItem)
          .withVisitShop(KiwiKwikiMartRequest::visitShop)
          .withPurchasedItem(KiwiKwikiMartRequest::purchasedItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.MINI_KIWI_INTOXICATING_SPIRITS -> !Preferences.getBoolean(
          "_miniKiwiIntoxicatingSpiritsBought");
      default -> DATA.availableItem(itemId);
    };
  }

  public static void visitShop(final String responseText) {
    Preferences.setBoolean(
        "_miniKiwiIntoxicatingSpiritsBought",
        !responseText.contains("mini kiwi intoxicating spirits"));
  }

  public static void purchasedItem(final AdventureResult item, final Boolean storage) {
    // Purchasing certain items makes them unavailable
    if (item.getItemId() == ItemPool.MINI_KIWI_INTOXICATING_SPIRITS) {
      Preferences.setBoolean("_miniKiwiIntoxicatingSpiritsBought", true);
    }
  }
}
