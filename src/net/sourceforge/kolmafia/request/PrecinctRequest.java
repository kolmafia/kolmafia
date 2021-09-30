package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class PrecinctRequest extends CoinMasterRequest {
  public static final String master = "Precinct Materiel Division";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(PrecinctRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(PrecinctRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(PrecinctRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) cop dollar");
  public static final AdventureResult DOLLAR = ItemPool.get(ItemPool.COP_DOLLAR, 1);
  public static final CoinmasterData PRECINCT =
      new CoinmasterData(
          PrecinctRequest.master,
          "Precinct Materiel Division",
          PrecinctRequest.class,
          "cop dollar",
          null,
          false,
          PrecinctRequest.TOKEN_PATTERN,
          PrecinctRequest.DOLLAR,
          null,
          PrecinctRequest.itemRows,
          "shop.php?whichshop=detective",
          "buyitem",
          PrecinctRequest.buyItems,
          PrecinctRequest.buyPrices,
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

  public PrecinctRequest() {
    super(PrecinctRequest.PRECINCT);
  }

  public PrecinctRequest(final boolean buying, final AdventureResult[] attachments) {
    super(PrecinctRequest.PRECINCT, buying, attachments);
  }

  public PrecinctRequest(final boolean buying, final AdventureResult attachment) {
    super(PrecinctRequest.PRECINCT, buying, attachment);
  }

  public PrecinctRequest(final boolean buying, final int itemId, final int quantity) {
    super(PrecinctRequest.PRECINCT, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    PrecinctRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=detective")) {
      return;
    }

    CoinmasterData data = PrecinctRequest.PRECINCT;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=detective")) {
      return false;
    }

    CoinmasterData data = PrecinctRequest.PRECINCT;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    return null;
  }
}
