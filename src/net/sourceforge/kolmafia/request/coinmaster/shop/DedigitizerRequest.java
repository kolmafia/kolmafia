package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class DedigitizerRequest extends CoinMasterRequest {
  public static final String master = "The Dedigitizer";
  public static final AdventureResult SERVER_ROOM_KEY = ItemPool.get(ItemPool.SERVER_KEY, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "cyber_dedigitizer", DedigitizerRequest.class)
          .withNewShopRowFields(master, "cyber_dedigitizer")
          .withNeedsPasswordHash(true);

  public DedigitizerRequest() {
    super(DATA);
  }

  public DedigitizerRequest(final ShopRow row, final int count) {
    super(DATA, row, count);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=" + DATA.getNickname())) {
      return;
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
    int serverRoomKey = SERVER_ROOM_KEY.getCount(KoLConstants.inventory);
    if (serverRoomKey == 0) {
      return "You don't have a server room key in inventory.";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php")
        || !urlString.contains("whichshop=" + DATA.getNickname())) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DATA, urlString, true);
  }
}
