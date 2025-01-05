package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class CosmicRaysBazaarRequest extends CoinMasterRequest {
  public static final String master = "Cosmic Ray's Bazaar";

  public static final AdventureResult RARE_MEAT_ISOTOPE =
      ItemPool.get(ItemPool.RARE_MEAT_ISOTOPE, 1);
  public static final AdventureResult WHITE_PIXEL = ItemPool.get(ItemPool.WHITE_PIXEL, 1);
  public static final AdventureResult FAT_LOOT_TOKEN = ItemPool.get(ItemPool.FAT_LOOT_TOKEN, 1);

  public static final CoinmasterData COSMIC_RAYS_BAZAAR =
      new CoinmasterData(master, "exploathing", CosmicRaysBazaarRequest.class)
          .withShopRowFields(master, "exploathing")
          .withBuyPrices()
          .withItemBuyPrice(CosmicRaysBazaarRequest::itemBuyPrice)
          .withNeedsPasswordHash(true);

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

  public CosmicRaysBazaarRequest() {
    super(COSMIC_RAYS_BAZAAR);
  }

  public CosmicRaysBazaarRequest(final String action) {
    super(COSMIC_RAYS_BAZAAR, action);
  }

  public CosmicRaysBazaarRequest(final boolean buying, final AdventureResult[] attachments) {
    super(COSMIC_RAYS_BAZAAR, buying, attachments);
  }

  public CosmicRaysBazaarRequest(final boolean buying, final AdventureResult attachment) {
    super(COSMIC_RAYS_BAZAAR, buying, attachment);
  }

  public CosmicRaysBazaarRequest(final boolean buying, final int itemId, final int quantity) {
    super(COSMIC_RAYS_BAZAAR, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=exploathing")) {
      return;
    }

    CoinmasterData data = COSMIC_RAYS_BAZAAR;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (!KoLCharacter.isKingdomOfExploathing()) {
      return "The Kingdom is not Exploathing";
    }

    return null;
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=exploathing")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(COSMIC_RAYS_BAZAAR, urlString, true);
  }
}
