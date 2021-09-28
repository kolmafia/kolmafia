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

public class WalMartRequest extends CoinMasterRequest {
  public static final String master = "Wal-Mart";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(WalMartRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(WalMartRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(WalMartRequest.master);

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) Wal-Mart gift certificates");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.WALMART_GIFT_CERTIFICATE, 1);
  public static final CoinmasterData WALMART =
      new CoinmasterData(
          WalMartRequest.master,
          "Wal-Mart",
          WalMartRequest.class,
          "Wal-Mart gift certificate",
          null,
          false,
          WalMartRequest.TOKEN_PATTERN,
          WalMartRequest.COIN,
          null,
          WalMartRequest.itemRows,
          "shop.php?whichshop=glaciest",
          "buyitem",
          WalMartRequest.buyItems,
          WalMartRequest.buyPrices,
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

  public WalMartRequest() {
    super(WalMartRequest.WALMART);
  }

  public WalMartRequest(final boolean buying, final AdventureResult[] attachments) {
    super(WalMartRequest.WALMART, buying, attachments);
  }

  public WalMartRequest(final boolean buying, final AdventureResult attachment) {
    super(WalMartRequest.WALMART, buying, attachment);
  }

  public WalMartRequest(final boolean buying, final int itemId, final int quantity) {
    super(WalMartRequest.WALMART, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    WalMartRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=glaciest")) {
      return;
    }

    CoinmasterData data = WalMartRequest.WALMART;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=glaciest")) {
      return false;
    }

    CoinmasterData data = WalMartRequest.WALMART;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_coldAirportToday")
        && !Preferences.getBoolean("coldAirportAlways")) {
      return "You don't have access to The Glaciest";
    }
    if (Limitmode.limitZone("The Glaciest")) {
      return "You cannot currently access The Glaciest";
    }
    return null;
  }
}
