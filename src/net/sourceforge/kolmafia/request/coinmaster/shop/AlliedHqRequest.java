package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.QuestManager;

public abstract class AlliedHqRequest extends CoinMasterShopRequest {
  public static final String master = "Allied HQ";
  public static final String SHOPID = "twitch_alliedhq";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "twitch_alliedhq", AlliedHqRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withVisitShop(AlliedHqRequest::visitShop)
          .withAccessible(AlliedHqRequest::accessible);

  public static void visitShop(final String responseText) {
    QuestManager.handleTimeTower(!responseText.contains("That store isn't there anymore."));
  }

  public static String accessible() {
    if (!Preferences.getBoolean("timeTowerAvailable")) {
      return "You can't get to the Allied HQ";
    }
    return null;
  }
}
