package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.Limitmode;

public class CanteenRequest extends CoinMasterRequest {
  public static final String master = "The Canteen";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(CanteenRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(CanteenRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(CanteenRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Coins-spiracy");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.COINSPIRACY, 1);
  public static final CoinmasterData CANTEEN =
      new CoinmasterData(
          CanteenRequest.master,
          "canteen",
          CanteenRequest.class,
          "Coinspiracy",
          null,
          false,
          CanteenRequest.TOKEN_PATTERN,
          CanteenRequest.COIN,
          null,
          CanteenRequest.itemRows,
          "shop.php?whichshop=si_shop2",
          "buyitem",
          CanteenRequest.buyItems,
          CanteenRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true);

  public CanteenRequest() {
    super(CanteenRequest.CANTEEN);
  }

  public CanteenRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CanteenRequest.CANTEEN, buying, attachments);
  }

  public CanteenRequest(final boolean buying, final AdventureResult attachment) {
    super(CanteenRequest.CANTEEN, buying, attachment);
  }

  public CanteenRequest(final boolean buying, final int itemId, final int quantity) {
    super(CanteenRequest.CANTEEN, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    CanteenRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=si_shop2")) {
      return;
    }

    CoinmasterData data = CanteenRequest.CANTEEN;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=si_shop2")) {
      return false;
    }

    CoinmasterData data = CanteenRequest.CANTEEN;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_spookyAirportToday")
        && !Preferences.getBoolean("spookyAirportAlways")) {
      return "You don't have access to Conspiracy Island";
    }
    if (Limitmode.limitZone("Conspiracy Island")) {
      return "You cannot currently access Conspiracy Island";
    }
    if (!Preferences.getBoolean("canteenUnlocked")) {
      return "The Canteen is locked";
    }
    return null;
  }
}
