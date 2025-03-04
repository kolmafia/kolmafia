package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class SeptEmberCenserRequest extends CoinMasterShopRequest {
  public static final String master = "Sept-Ember Censer";
  public static final String SHOPID = "september";

  // <b>You have 8 Embers.</b>
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<b>You have ([\\d,]+) Ember");

  public static final CoinmasterData SEPTEMBER_CENSER =
      new CoinmasterData(master, "Sept-Ember Censer", SeptEmberCenserRequest.class)
          .withToken("Ember")
          .withTokenPattern(TOKEN_PATTERN)
          .withProperty("availableSeptEmbers")
          .withShopRowFields(master, SHOPID)
          .withVisitShop(SeptEmberCenserRequest::visitShop)
          .withAccessible(SeptEmberCenserRequest::accessible);

  public static CoinMasterShopRequest getRequest() {
    return CoinMasterShopRequest.getRequest(SEPTEMBER_CENSER);
  }

  public static void visitShop(final String responseText) {
    if (!Preferences.getBoolean("_septEmberBalanceChecked")) {
      // Parse current coin balances
      CoinMasterRequest.parseBalance(SEPTEMBER_CENSER, responseText);
      Preferences.setBoolean("_septEmberBalanceChecked", true);
    }
  }

  public static String accessible() {
    if (InventoryManager.hasItem(ItemPool.SEPTEMBER_CENSER)) {
      return null;
    }
    return "You need a Sept-Ember Censer in order to shop here.";
  }
}
