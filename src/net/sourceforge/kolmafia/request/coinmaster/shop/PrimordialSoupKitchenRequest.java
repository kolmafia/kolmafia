package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class PrimordialSoupKitchenRequest extends CoinMasterRequest {
  public static final String master = "The Primordial Soup Kitchen";
  public static final String SHOPID = "twitchsoup";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "twitchsoup", PrimordialSoupKitchenRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withVisitShop(PrimordialSoupKitchenRequest::visitShop)
          .withAccessible(PrimordialSoupKitchenRequest::accessible);

  public PrimordialSoupKitchenRequest() {
    super(DATA);
  }

  public PrimordialSoupKitchenRequest(final ShopRow row, final int count) {
    super(DATA, row, count);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void visitShop(final String responseText) {
    QuestManager.handleTimeTower(!responseText.contains("That store isn't there anymore."));
  }

  public static String accessible() {
    if (!Preferences.getBoolean("timeTowerAvailable")) {
      return "You can't get to the Primordial Soup Kitchen";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=" + DATA.getShopId())) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DATA, urlString, true);
  }
}
