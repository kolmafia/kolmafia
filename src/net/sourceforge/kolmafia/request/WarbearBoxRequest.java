package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class WarbearBoxRequest extends CoinMasterRequest {
  public static final String master = "Warbear Black Box";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(WarbearBoxRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(WarbearBoxRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(WarbearBoxRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) warbear whosit");
  public static final AdventureResult WHOSIT = ItemPool.get(ItemPool.WARBEAR_WHOSIT, 1);
  public static final AdventureResult BLACKBOX = ItemPool.get(ItemPool.WARBEAR_BLACK_BOX, 1);
  public static final CoinmasterData WARBEARBOX =
      new CoinmasterData(
          WarbearBoxRequest.master,
          "warbear",
          WarbearBoxRequest.class,
          "warbear whosit",
          null,
          false,
          WarbearBoxRequest.TOKEN_PATTERN,
          WarbearBoxRequest.WHOSIT,
          null,
          WarbearBoxRequest.itemRows,
          "shop.php?whichshop=warbear",
          "buyitem",
          WarbearBoxRequest.buyItems,
          WarbearBoxRequest.buyPrices,
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

  public WarbearBoxRequest() {
    super(WarbearBoxRequest.WARBEARBOX);
  }

  public WarbearBoxRequest(final boolean buying, final AdventureResult[] attachments) {
    super(WarbearBoxRequest.WARBEARBOX, buying, attachments);
  }

  public WarbearBoxRequest(final boolean buying, final AdventureResult attachment) {
    super(WarbearBoxRequest.WARBEARBOX, buying, attachment);
  }

  public WarbearBoxRequest(final boolean buying, final int itemId, final int quantity) {
    super(WarbearBoxRequest.WARBEARBOX, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    WarbearBoxRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=warbear")) {
      return;
    }

    CoinmasterData data = WarbearBoxRequest.WARBEARBOX;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=warbear")) {
      return false;
    }

    CoinmasterData data = WarbearBoxRequest.WARBEARBOX;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    int wand = WarbearBoxRequest.BLACKBOX.getCount(KoLConstants.inventory);
    if (wand == 0) {
      return "You don't have a warbear black box";
    }
    return null;
  }
}
