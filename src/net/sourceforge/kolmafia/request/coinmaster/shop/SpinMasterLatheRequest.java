package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class SpinMasterLatheRequest extends CoinMasterShopRequest {
  public static final String master = "Your SpinMaster&trade; lathe";
  public static String SHOPID = "lathe";

  // Since there are seven different currencies, we need to have a map from
  // itemId to item/count of currency; an AdventureResult.
  private static final Map<Integer, AdventureResult> buyCosts = new TreeMap<>();

  public static final CoinmasterData YOUR_SPINMASTER_LATHE =
      new CoinmasterData(master, "lathe", SpinMasterLatheRequest.class)
          .withShopRowFields(master, SHOPID)
          .withBuyPrices()
          .withItemBuyPrice(SpinMasterLatheRequest::itemBuyPrice)
          .withVisitShop(SpinMasterLatheRequest::visitShop)
          .withAccessible(SpinMasterLatheRequest::accessible);

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

  private static AdventureResult itemBuyPrice(final Integer itemId) {
    return buyCosts.get(itemId);
  }

  public static void visitShop(final String responseText) {
    Preferences.setBoolean("_spinmasterLatheVisited", true);
  }

  public static String accessible() {
    if (!InventoryManager.hasItem(SPINMASTER)) {
      return "You don't own a " + SPINMASTER.getName();
    }

    return null;
  }
}
