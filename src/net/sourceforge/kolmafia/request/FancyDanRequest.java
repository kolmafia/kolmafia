package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class FancyDanRequest extends CoinMasterRequest {
  public static final String master = "Fancy Dan the Cocktail Man";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(FancyDanRequest.master);
  private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getNewMap();
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(FancyDanRequest.master);

  public static final AdventureResult MILK_CAP = ItemPool.get(ItemPool.MILK_CAP, 1);
  public static final AdventureResult DRINK_CHIT = ItemPool.get(ItemPool.DRINK_CHIT, 1);

  public static final CoinmasterData FANCY_DAN =
      new CoinmasterData(
          FancyDanRequest.master,
          "olivers",
          FancyDanRequest.class,
          null,
          null,
          false,
          null,
          null,
          null,
          FancyDanRequest.itemRows,
          "shop.php?whichshop=olivers",
          "buyitem",
          FancyDanRequest.buyItems,
          FancyDanRequest.buyPrices,
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
          true) {
        @Override
        public AdventureResult itemBuyPrice(final int itemId) {
          return FancyDanRequest.buyCosts.get(itemId);
        }
      };

  // Since there are two different currencies, we need to have a map from
  // itemId to item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts =
      new TreeMap<Integer, AdventureResult>();

  // Manually set up the map and change the currency, as need
  static {
    for (Entry<Integer, Integer> entry :
        CoinmastersDatabase.getBuyPrices(FancyDanRequest.master).entrySet()) {
      int itemId = entry.getKey().intValue();
      int price = entry.getValue().intValue();
      AdventureResult cost =
          switch (itemId) {
            case ItemPool.STRONG_SILENT_TYPE,
                ItemPool.MYSTERIOUS_STRANGER,
                ItemPool.CHAMPAGNE_SHIMMY -> MILK_CAP.getInstance(price);
            case ItemPool.CHARLESTON_CHOO_CHOO,
                ItemPool.VELVET_VEIL,
                ItemPool.MARLTINI -> DRINK_CHIT.getInstance(price);
              // Should not happen
            default -> null;
          };
      buyCosts.put(itemId, cost);
    }
  }

  public FancyDanRequest() {
    super(FancyDanRequest.FANCY_DAN);
  }

  public FancyDanRequest(final String action) {
    super(FancyDanRequest.FANCY_DAN, action);
  }

  public FancyDanRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FancyDanRequest.FANCY_DAN, buying, attachments);
  }

  public FancyDanRequest(final boolean buying, final AdventureResult attachment) {
    super(FancyDanRequest.FANCY_DAN, buying, attachment);
  }

  public FancyDanRequest(final boolean buying, final int itemId, final int quantity) {
    super(FancyDanRequest.FANCY_DAN, buying, itemId, quantity);
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
    FancyDanRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=olivers")) {
      return;
    }

    CoinmasterData data = FancyDanRequest.FANCY_DAN;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("ownsSpeakeasy")) {
      return "You don't own a speakeasy";
    }

    return null;
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=olivers")) {
      return false;
    }

    CoinmasterData data = FancyDanRequest.FANCY_DAN;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
