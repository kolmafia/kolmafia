package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class DedigitizerRequest extends CoinMasterShopRequest {
  public static final String master = "The Dedigitizer";
  public static final String SHOPID = "cyber_dedigitizer";

  public static final AdventureResult SERVER_ROOM_KEY = ItemPool.get(ItemPool.SERVER_KEY, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "cyber_dedigitizer", DedigitizerRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(DedigitizerRequest::accessible);

  public static String accessible() {
    if (Preferences.getBoolean("crAlways") || Preferences.getBoolean("_crToday")) {
      return null;
    }
    return "You can't access the server room.";
  }
}
