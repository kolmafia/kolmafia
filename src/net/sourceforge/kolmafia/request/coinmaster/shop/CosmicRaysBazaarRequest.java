package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public abstract class CosmicRaysBazaarRequest extends CoinMasterShopRequest {
  public static final String master = "Cosmic Ray's Bazaar";
  public static final String SHOPID = "exploathing";

  public static final AdventureResult RARE_MEAT_ISOTOPE =
      ItemPool.get(ItemPool.RARE_MEAT_ISOTOPE, 1);
  public static final AdventureResult WHITE_PIXEL = ItemPool.get(ItemPool.WHITE_PIXEL, 1);
  public static final AdventureResult FAT_LOOT_TOKEN = ItemPool.get(ItemPool.FAT_LOOT_TOKEN, 1);

  public static final CoinmasterData COSMIC_RAYS_BAZAAR =
      new CoinmasterData(master, "exploathing", CosmicRaysBazaarRequest.class)
          .withShopRowFields(master, SHOPID)
          .withBuyPrices()
          .withItemBuyPrice(CosmicRaysBazaarRequest::itemBuyPrice)
          .withAccessible(CosmicRaysBazaarRequest::accessible);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    return buyCosts.get(itemId);
  }

  // Since there are four different currencies, we need to have a map from
  // itemId to item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

  // Manually set up the map and change the currency, as need
  static {
    for (Entry<Integer, Integer> entry : CoinmastersDatabase.getBuyPrices(master).entrySet()) {
      int itemId = entry.getKey().intValue();
      int price = entry.getValue().intValue();
      AdventureResult cost =
          switch (itemId) {
            default -> RARE_MEAT_ISOTOPE.getInstance(price);
            case ItemPool.DIGITAL_KEY -> WHITE_PIXEL.getInstance(price);
            case ItemPool.BORIS_KEY,
                ItemPool.JARLSBERG_KEY,
                ItemPool.SNEAKY_PETE_KEY -> FAT_LOOT_TOKEN.getInstance(price);
            case ItemPool.RARE_MEAT_ISOTOPE -> CoinmasterData.MEAT.getInstance(price);
          };
      buyCosts.put(itemId, cost);
    }
  }

  public static String accessible() {
    if (!KoLCharacter.isKingdomOfExploathing()) {
      return "The Kingdom is not Exploathing";
    }

    return null;
  }
}
