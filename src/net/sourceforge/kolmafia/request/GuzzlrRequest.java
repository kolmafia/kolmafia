package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.session.InventoryManager;

public class GuzzlrRequest extends CoinMasterRequest {
  public static final String master = "Guzzlr Company Store Website";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(GuzzlrRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(GuzzlrRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(GuzzlrRequest.master);
  private static final Pattern GUZZLR_PATTERN = Pattern.compile("([\\d,]+) Guzzlrbuck");
  public static final AdventureResult GUZZLRBUCK = ItemPool.get(ItemPool.GUZZLRBUCK, 1);

  public static final CoinmasterData GUZZLR =
      new CoinmasterData(
          GuzzlrRequest.master,
          "guzzlr",
          GuzzlrRequest.class,
          "Guzzlrbuck",
          null,
          false,
          GuzzlrRequest.GUZZLR_PATTERN,
          GuzzlrRequest.GUZZLRBUCK,
          null,
          GuzzlrRequest.itemRows,
          "shop.php?whichshop=guzzlr",
          "buyitem",
          GuzzlrRequest.buyItems,
          GuzzlrRequest.buyPrices,
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

  public GuzzlrRequest() {
    super(GuzzlrRequest.GUZZLR);
  }

  public GuzzlrRequest(final boolean buying, final AdventureResult[] attachments) {
    super(GuzzlrRequest.GUZZLR, buying, attachments);
  }

  public GuzzlrRequest(final boolean buying, final AdventureResult attachment) {
    super(GuzzlrRequest.GUZZLR, buying, attachment);
  }

  public GuzzlrRequest(final boolean buying, final int itemId, final int quantity) {
    super(GuzzlrRequest.GUZZLR, buying, itemId, quantity);
  }

  @Override
  public void run() {
    if (this.action != null) {
      this.addFormField("pwd");
    }

    super.run();
  }

  @Override
  public void processResults() {
    GuzzlrRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=guzzlr")) {
      return;
    }

    CoinmasterData data = GuzzlrRequest.GUZZLR;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return InventoryManager.getAccessibleCount(GuzzlrRequest.GUZZLRBUCK) > 0
        ? null
        : "Need access to your Getaway Campsite";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=guzzlr")) {
      return false;
    }

    CoinmasterData data = GuzzlrRequest.GUZZLR;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
