package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class KiwiKwikiMartRequest extends CoinMasterRequest {
  public static final String master = "Kiwi Kwiki Mart";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "kiwi", KiwiKwikiMartRequest.class)
          .withNewShopRowFields(master, "kiwi")
          .withCanBuyItem(KiwiKwikiMartRequest::canBuyItem)
          .withNeedsPasswordHash(true);

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
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=" + DATA.getShopId())) {
      return;
    }

    if (!location.contains("ajax=1")) {
      Preferences.setBoolean(
          "_miniKiwiIntoxicatingSpiritsBought",
          !responseText.contains("mini kiwi intoxicating spirits"));
    }

    if (responseText.contains("Kingdom regulations prevent the purchase")) {
      Preferences.setBoolean("_miniKiwiIntoxicatingSpiritsBought", true);
    }

    CoinmasterData data = DATA;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=" + DATA.getShopId())) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DATA, urlString, true);
  }
}
