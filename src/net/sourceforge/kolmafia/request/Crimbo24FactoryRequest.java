package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class Crimbo24FactoryRequest extends CoinMasterRequest {
  public static final String master = "Crimbo24 Factory";

  // Since there are five different currencies, we need to have a map from
  // itemId to item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo24_factory", Crimbo24FactoryRequest.class)
          .withShopRowFields(master, "crimbo24_factory")
          .withBuyPrices()
          .withItemBuyPrice(Crimbo24FactoryRequest::itemBuyPrice)
          .withNeedsPasswordHash(true);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    return buyCosts.get(itemId);
  }

  public static final AdventureResult SPIRIT_OF_EASTER = ItemPool.get(ItemPool.SPIRIT_OF_EASTER, 1);
  public static final AdventureResult SPIRIT_OF_ST_PATRICKS_DAY =
      ItemPool.get(ItemPool.SPIRIT_OF_ST_PATRICKS_DAY, 1);
  public static final AdventureResult SPIRIT_OF_VETERANS_DAY =
      ItemPool.get(ItemPool.SPIRIT_OF_VETERANS_DAY, 1);
  public static final AdventureResult SPIRIT_OF_THANKSGIVING =
      ItemPool.get(ItemPool.SPIRIT_OF_THANKSGIVING, 1);
  public static final AdventureResult SPIRIT_OF_CHRISTMAS =
      ItemPool.get(ItemPool.SPIRIT_OF_CHRISTMAS, 1);

  // Manually set up the map and change the currency, as need
  /*
  static {
    Map<Integer, Integer> map = CoinmastersDatabase.getBuyPrices(master);
    for (Entry<Integer, Integer> entry : CoinmastersDatabase.getBuyPrices(master).entrySet()) {
      int itemId = entry.getKey().intValue();
      int price = entry.getValue().intValue();
      AdventureResult cost =
          switch (itemId) {
            default -> null;
            case ItemPool.LUCKY_MOAI_STATUETTE -> SPIRIT_OF_EASTER.getInstance(price);
            case ItemPool.SECRETS_OF_THE_MASTER_EGG_HUNTERS -> SPIRIT_OF_EASTER.getInstance(price);
            case ItemPool.HOW_TO_LOSE_FRIENDS_AND_ATTRACT_SNAKES -> SPIRIT_OF_ST_PATRICKS_DAY
                .getInstance(price);
          };
      buyCosts.put(itemId, cost);
    }
  }
  */

  public Crimbo24FactoryRequest() {
    super(DATA);
  }

  public Crimbo24FactoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DATA, buying, attachments);
  }

  public Crimbo24FactoryRequest(final boolean buying, final AdventureResult attachment) {
    super(DATA, buying, attachment);
  }

  public Crimbo24FactoryRequest(final boolean buying, final int itemId, final int quantity) {
    super(DATA, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=" + DATA.getNickname())) {
      return;
    }

    CoinmasterData data = DATA;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return "";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php")
        || !urlString.contains("whichshop=" + DATA.getNickname())) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DATA, urlString, true);
  }
}
