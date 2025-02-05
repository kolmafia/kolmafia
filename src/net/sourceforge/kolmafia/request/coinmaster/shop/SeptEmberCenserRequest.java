package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class SeptEmberCenserRequest extends CoinMasterRequest {
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

  public SeptEmberCenserRequest() {
    super(SEPTEMBER_CENSER);
  }

  public SeptEmberCenserRequest(final boolean buying, final AdventureResult[] attachments) {
    super(SEPTEMBER_CENSER, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void visitShop(final String responseText) {
    // Parse current coin balances
    CoinMasterRequest.parseBalance(SEPTEMBER_CENSER, responseText);
    Preferences.setBoolean("_septEmberBalanceChecked", true);
  }

  public static String accessible() {
    if (InventoryManager.hasItem(ItemPool.SEPTEMBER_CENSER)) {
      return null;
    }
    return "You need a Sept-Ember Censer in order to shop here.";
  }
}
