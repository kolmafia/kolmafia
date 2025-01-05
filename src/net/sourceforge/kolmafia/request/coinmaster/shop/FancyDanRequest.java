package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class FancyDanRequest extends CoinMasterRequest {
  public static final String master = "Fancy Dan the Cocktail Man";

  // Since there are two different currencies, we need to have a map from
  // itemId to item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

  // Manually set up the map and change the currency, as need

  public static final AdventureResult MILK_CAP = ItemPool.get(ItemPool.MILK_CAP, 1);
  public static final AdventureResult DRINK_CHIT = ItemPool.get(ItemPool.DRINK_CHIT, 1);

  static {
    for (Entry<Integer, Integer> entry : CoinmastersDatabase.getBuyPrices(master).entrySet()) {
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

  public static final CoinmasterData FANCY_DAN =
      new CoinmasterData(master, "Speakeasy", FancyDanRequest.class)
          .withShopRowFields(master, "olivers")
          .withBuyPrices()
          .withItemBuyPrice(FancyDanRequest::itemBuyPrice)
          .withNeedsPasswordHash(true);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    return buyCosts.get(itemId);
  }

  public FancyDanRequest() {
    super(FANCY_DAN);
  }

  public FancyDanRequest(final String action) {
    super(FANCY_DAN, action);
  }

  public FancyDanRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FANCY_DAN, buying, attachments);
  }

  public FancyDanRequest(final boolean buying, final AdventureResult attachment) {
    super(FANCY_DAN, buying, attachment);
  }

  public FancyDanRequest(final boolean buying, final int itemId, final int quantity) {
    super(FANCY_DAN, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=olivers")) {
      return;
    }

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(FANCY_DAN, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(FANCY_DAN, responseText);
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

    return CoinMasterRequest.registerRequest(FANCY_DAN, urlString, true);
  }
}
