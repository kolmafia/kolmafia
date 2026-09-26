package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.QuestManager;

public abstract class RenaissanceGiftShopRequest extends CoinMasterShopRequest {
  public static final String master = "Renaissance Gift Shop";
  public static final String SHOPID = "twitch_jousting";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "twitch_jousting", RenaissanceGiftShopRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withVisitShop(RenaissanceGiftShopRequest::visitShop)
          .withAccessible(RenaissanceGiftShopRequest::accessible);

  public static void visitShop(final String responseText) {
    QuestManager.handleTimeTower(!responseText.contains("That store isn't there anymore."));
  }

  public static String accessible() {
    if (!Preferences.getBoolean("timeTowerAvailable")) {
      return "You can't get to the Renaissance Gift Shop";
    }
    return null;
  }
}
