package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class KiwiKwikiMartRequest extends CoinMasterRequest {
  public static final String master = "Kiwi Kwiki Mart";
  public static final String SHOPID = "kiwi";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "kiwi", KiwiKwikiMartRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withCanBuyItem(KiwiKwikiMartRequest::canBuyItem)
          .withVisitShop(KiwiKwikiMartRequest::visitShop)
          .withPurchasedItem(KiwiKwikiMartRequest::purchasedItem);

  public KiwiKwikiMartRequest() {
    super(DATA);
  }

  public KiwiKwikiMartRequest(final ShopRow row, final int count) {
    super(DATA, row, count);
  }

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.MINI_KIWI_INTOXICATING_SPIRITS -> !Preferences.getBoolean(
          "_miniKiwiIntoxicatingSpiritsBought");
      default -> DATA.availableItem(itemId);
    };
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
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
