package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class DedigitizerRequest extends CoinMasterRequest {
  public static final String master = "The Dedigitizer";
  public static final String SHOPID = "cyber_dedigitizer";

  public static final AdventureResult SERVER_ROOM_KEY = ItemPool.get(ItemPool.SERVER_KEY, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "cyber_dedigitizer", DedigitizerRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(DedigitizerRequest::accessible);

  public DedigitizerRequest() {
    super(DATA);
  }

  public DedigitizerRequest(final ShopRow row, final int count) {
    super(DATA, row, count);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    if (Preferences.getBoolean("crAlways") || Preferences.getBoolean("_crToday")) {
      return null;
    }
    return "You can't access the server room.";
  }
}
