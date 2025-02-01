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
import net.sourceforge.kolmafia.session.InventoryManager;

public class SpinMasterLatheRequest extends CoinMasterRequest {
  public static final String master = "Your SpinMaster&trade; lathe";

  // Since there are seven different currencies, we need to have a map from
  // itemId to item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

  public static final CoinmasterData YOUR_SPINMASTER_LATHE =
      new CoinmasterData(master, "lathe", SpinMasterLatheRequest.class)
          .withShopRowFields(master, "lathe")
          .withBuyPrices()
          .withItemBuyPrice(SpinMasterLatheRequest::itemBuyPrice)
          .withAccessible(SpinMasterLatheRequest::accessible);

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    return buyCosts.get(itemId);
  }

  public static final AdventureResult SPINMASTER = ItemPool.get(ItemPool.SPINMASTER, 1);
  public static final AdventureResult FLIMSY_HARDWOOD_SCRAPS =
      ItemPool.get(ItemPool.FLIMSY_HARDWOOD_SCRAPS, 1);
  public static final AdventureResult DREADSYLVANIAN_HEMLOCK =
      ItemPool.get(ItemPool.DREADSYLVANIAN_HEMLOCK, 1);
  public static final AdventureResult SWEATY_BALSAM = ItemPool.get(ItemPool.SWEATY_BALSAM, 1);
  public static final AdventureResult ANCIENT_REDWOOD = ItemPool.get(ItemPool.ANCIENT_REDWOOD, 1);
  public static final AdventureResult PURPLEHEART_LOGS = ItemPool.get(ItemPool.PURPLEHEART_LOGS, 1);
  public static final AdventureResult WORMWOOD_STICK = ItemPool.get(ItemPool.WORMWOOD_STICK, 1);
  public static final AdventureResult DRIPWOOD_SLAB = ItemPool.get(ItemPool.DRIPWOOD_SLAB, 1);

  // Manually set up the map and change the currency, as need
  static {
    Map<Integer, Integer> map = CoinmastersDatabase.getBuyPrices(master);
    for (Entry<Integer, Integer> entry : CoinmastersDatabase.getBuyPrices(master).entrySet()) {
      int itemId = entry.getKey().intValue();
      int price = entry.getValue().intValue();
      AdventureResult cost =
          switch (itemId) {
            default -> FLIMSY_HARDWOOD_SCRAPS.getInstance(price);
            case ItemPool.HEMLOCK_HELM -> DREADSYLVANIAN_HEMLOCK.getInstance(price);
            case ItemPool.BALSAM_BARREL -> SWEATY_BALSAM.getInstance(price);
            case ItemPool.REDWOOD_RAIN_STICK -> ANCIENT_REDWOOD.getInstance(price);
            case ItemPool.PURPLEHEART_PANTS -> PURPLEHEART_LOGS.getInstance(price);
            case ItemPool.WORMWOOD_WEDDING_RING -> WORMWOOD_STICK.getInstance(price);
            case ItemPool.DRIPPY_DIADEM -> DRIPWOOD_SLAB.getInstance(price);
          };
      buyCosts.put(itemId, cost);
    }
  }

  public SpinMasterLatheRequest() {
    super(YOUR_SPINMASTER_LATHE);
  }

  public SpinMasterLatheRequest(final String action) {
    super(YOUR_SPINMASTER_LATHE, action);
  }

  public SpinMasterLatheRequest(final boolean buying, final AdventureResult[] attachments) {
    super(YOUR_SPINMASTER_LATHE, buying, attachments);
  }

  public SpinMasterLatheRequest(final boolean buying, final AdventureResult attachment) {
    super(YOUR_SPINMASTER_LATHE, buying, attachment);
  }

  public SpinMasterLatheRequest(final boolean buying, final int itemId, final int quantity) {
    super(YOUR_SPINMASTER_LATHE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=lathe")) {
      return;
    }

    CoinmasterData data = YOUR_SPINMASTER_LATHE;
    Preferences.setBoolean("_spinmasterLatheVisited", true);

    String action = GenericRequest.getAction(location);
    if (action == null) {
      // Parse current coin balances
      CoinMasterRequest.parseBalance(data, responseText);
      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static String accessible() {
    if (!InventoryManager.hasItem(SPINMASTER)) {
      return "You don't own a " + SPINMASTER.getName();
    }

    return null;
  }
}
