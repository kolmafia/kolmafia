package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.QuestManager;

public abstract class PrimordialSoupKitchenRequest extends CoinMasterShopRequest {
  public static final String master = "The Primordial Soup Kitchen";
  public static final String SHOPID = "twitchsoup";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "twitchsoup", PrimordialSoupKitchenRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withVisitShop(PrimordialSoupKitchenRequest::visitShop)
          .withAccessible(PrimordialSoupKitchenRequest::accessible);

  public static void visitShop(final String responseText) {
    QuestManager.handleTimeTower(!responseText.contains("That store isn't there anymore."));
  }

  public static String accessible() {
    if (!Preferences.getBoolean("timeTowerAvailable")) {
      return "You can't get to the Primordial Soup Kitchen";
    }
    return null;
  }
}
