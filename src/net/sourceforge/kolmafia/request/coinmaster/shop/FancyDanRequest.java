package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class FancyDanRequest extends CoinMasterShopRequest {
  public static final String master = "Fancy Dan the Cocktail Man";
  public static final String SHOPID = "olivers";

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
          .withShopRowFields(master, SHOPID)
          .withBuyPrices()
          .withItemBuyPrice(FancyDanRequest::itemBuyPrice)
          .withAccessible(FancyDanRequest::accessible);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    return buyCosts.get(itemId);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("ownsSpeakeasy")) {
      return "You don't own a speakeasy";
    }

    return null;
  }
}
