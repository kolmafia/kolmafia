package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.ShopRow;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CoinMasterPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LockableListFactory;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CoinmastersDatabase {

  private CoinmastersDatabase() {}

  // *** New style "shop.php" Coinmaster
  // buy (and sell) using shopRows

  // Map from String -> List<ShopRow>
  public static final Map<String, List<ShopRow>> shopRows = new TreeMap<>();

  // Map from Integer to ShopRow

  // *** Since I believe ROW numbers are unique, it would be nice to also
  // *** also register items in NPCstores and Concoctions
  // *** Put this into ShopRow.java?
  public static final Map<Integer, ShopRow> rowData = new TreeMap<>();

  // Map from Integer to String

  // *** Same comment
  public static final Map<Integer, String> rowShop = new TreeMap<>();

  public static final List<ShopRow> getShopRows(final String key) {
    return shopRows.get(key);
  }

  public static final ShopRow getRowData(final int row) {
    return rowData.get(row);
  }

  public static final String getRowShop(final int row) {
    return rowShop.get(row);
  }

  // *** Old style "shop.php" (and other) Coinmasters
  // buy using buyItems and buyPrices
  // sell using sellItems and sellPrices.

  // Map from Integer( itemId ) -> CoinMasterPurchaseRequest
  public static final Map<Integer, CoinMasterPurchaseRequest> COINMASTER_ITEMS = new HashMap<>();

  // Map from Integer( row ) -> CoinMasterPurchaseRequest
  public static final Map<Integer, CoinMasterPurchaseRequest> COINMASTER_ROWS = new HashMap<>();

  // Map from String -> LockableListModel
  public static final Map<String, List<AdventureResult>> buyItems = new TreeMap<>();

  // Map from String -> Map from Integer -> Integer
  public static final Map<String, Map<Integer, Integer>> buyPrices = new TreeMap<>();

  // Map from String -> LockableListModel
  public static final Map<String, List<AdventureResult>> sellItems = new TreeMap<>();

  // Map from String -> Map from Integer -> Integer
  public static final Map<String, Map<Integer, Integer>> sellPrices = new TreeMap<>();

  // Map from String -> Map from Integer -> Integer
  public static final Map<String, Map<Integer, Integer>> itemRows = new TreeMap<>();

  public static final List<AdventureResult> getBuyItems(final String key) {
    return buyItems.get(key);
  }

  public static final Map<Integer, Integer> getBuyPrices(final String key) {
    return buyPrices.get(key);
  }

  public static final List<AdventureResult> getSellItems(final String key) {
    return sellItems.get(key);
  }

  public static final Map<Integer, Integer> getSellPrices(final String key) {
    return sellPrices.get(key);
  }

  public static final Map<Integer, Integer> getRows(final String key) {
    return itemRows.get(key);
  }

  public static final Map<Integer, Integer> getOrMakeRows(final String key) {
    return getOrMakeMap(key, itemRows);
  }

  public static final List<AdventureResult> getNewList() {
    // Get a LockableListModel if we are running in a Swing environment,
    // since these lists will be the models for GUI elements
    return LockableListFactory.getInstance(AdventureResult.class);
  }

  public static final Map<Integer, Integer> getNewMap() {
    return new TreeMap<>();
  }

  private static List<AdventureResult> getOrMakeList(
      final String key, final Map<String, List<AdventureResult>> map) {
    List<AdventureResult> retval = map.get(key);
    if (retval == null) {
      retval = getNewList();
      map.put(key, retval);
    }
    return retval;
  }

  private static Map<Integer, Integer> getOrMakeMap(
      final String key, final Map<String, Map<Integer, Integer>> map) {
    return map.computeIfAbsent(key, k -> getNewMap());
  }

  public static final Map<Integer, Integer> invert(final Map<Integer, Integer> map) {
    Map<Integer, Integer> retval = new TreeMap<>();
    for (Entry<Integer, Integer> entry : map.entrySet()) {
      retval.put(entry.getValue(), entry.getKey());
    }
    return retval;
  }

  static {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("coinmasters.txt", KoLConstants.COINMASTERS_VERSION)) {
      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 4) {
          continue;
        }

        String master = data[0];

        // "type" is the second field.
        // If it is "buy", the Coinmaster uses buyItems and buyPrices
        // If it is "sell", the Coinmaster uses sellItems and sellPrices
        // If it starts with ROW, the Coinmaster uses shopRows

        String type = data[1];

        if (type.startsWith("ROW")) {
          ShopRow shopRow = ShopRow.fromData(data);
          if (shopRow == null) {
            // *** error
            continue;
          }
          int row = shopRow.getRow();
          List<ShopRow> rows = shopRows.get(master);
          if (rows == null) {
            // Get a LockableListModel if we are running in a Swing environment,
            // since these lists will be the models for GUI elements
            rows = LockableListFactory.getInstance(ShopRow.class);
            shopRows.put(master, rows);
          }
          rows.add(shopRow);
          rowData.put(row, shopRow);
          rowShop.put(row, master);
          continue;
        }

        int price = StringUtilities.parseInt(data[2]);
        Integer iprice = price;
        AdventureResult item = AdventureResult.parseItem(data[3], true);
        Integer iitemId = item.getItemId();
        Integer row = null;
        if (data.length > 4) {
          String[] extra = data[4].split("\\s,\\s");
          for (String extra1 : extra) {
            if (extra1.startsWith("ROW")) {
              row = StringUtilities.parseInt(data[4].substring(3));
              Map<Integer, Integer> rowMap = getOrMakeMap(master, itemRows);
              rowMap.put(iitemId, row);
            }
          }
        }

        if (type.equals("buy")) {
          List<AdventureResult> list = getOrMakeList(master, buyItems);
          list.add(item.getInstance(purchaseLimit(iitemId)));

          Map<Integer, Integer> map = getOrMakeMap(master, buyPrices);
          map.put(iitemId, iprice);
        } else if (type.equals("sell")) {
          List<AdventureResult> list = getOrMakeList(master, sellItems);
          list.add(item);

          Map<Integer, Integer> map = getOrMakeMap(master, sellPrices);
          map.put(iitemId, iprice);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  public static int purchaseLimit(final int itemId) {
    return switch (itemId) {
      case ItemPool.ZEPPELIN_TICKET,
          ItemPool.TALES_OF_DREAD,
          ItemPool.BRASS_DREAD_FLASK,
          ItemPool.SILVER_DREAD_FLASK -> 1;
      default -> PurchaseRequest.MAX_QUANTITY;
    };
  }

  public static final int getPrice(final int itemId, final Map<Integer, Integer> prices) {
    if (itemId == -1) {
      return 0;
    }
    Integer price = prices.get(itemId);
    return (price == null) ? 0 : price.intValue();
  }

  public static final void clearPurchaseRequests(CoinmasterData data) {
    // Clear all purchase requests for a particular Coin Master
    COINMASTER_ITEMS.values().removeIf(request -> request.getData() == data);
    COINMASTER_ROWS.values().removeIf(request -> request.getData() == data);
  }

  public static final void registerPurchaseRequest(
      final CoinmasterData data, final AdventureResult item, final AdventureResult price) {
    int itemId = item.getItemId();
    int quantity = item.getCount();

    // Register a purchase request
    CoinMasterPurchaseRequest request = new CoinMasterPurchaseRequest(data, item, price);
    COINMASTER_ITEMS.put(itemId, request);

    // Register this in the Concoction for the item

    // Special case: 11-leaf clovers are limited
    if (itemId == ItemPool.ELEVEN_LEAF_CLOVER) {
      return;
    }

    Concoction concoction = ConcoctionPool.get(itemId);
    if (concoction == null) {
      return;
    }

    // If we can create it any other way, prefer that method
    if (concoction.getMixingMethod() == CraftingType.NOCREATE) {
      concoction.setMixingMethod(CraftingType.COINMASTER);
      concoction.addIngredient(price);
    }

    // If we can create this only via a coin master trade, save request
    if (concoction.getMixingMethod() == CraftingType.COINMASTER) {
      concoction.setPurchaseRequest(request);
    }
  }

  public static final void registerPurchaseRequest(
      final CoinmasterData data, final ShopRow shopRow) {
    // Register a purchase request
    CoinMasterPurchaseRequest request = new CoinMasterPurchaseRequest(data, shopRow);
    COINMASTER_ROWS.put(shopRow.getRow(), request);

    AdventureResult item = shopRow.getItem();
    int itemId = item.getItemId();
    int count = item.getCount();
    COINMASTER_ITEMS.put(itemId, request);

    // Register this in the Concoction for the item

    // *** Only register if this is like a "buy"
    Concoction concoction = ConcoctionPool.get(itemId);
    if (concoction == null) {
      return;
    }

    // If we can create it any other way, prefer that method
    if (concoction.getMixingMethod() == CraftingType.NOCREATE) {
      concoction.setMixingMethod(CraftingType.COINMASTER);
      for (AdventureResult ingredient : shopRow.getCosts()) {
        concoction.addIngredient(ingredient);
      }
    }

    // If we can create this only via a coin master trade, save request
    if (concoction.getMixingMethod() == CraftingType.COINMASTER) {
      concoction.setPurchaseRequest(request);
    }
  }

  public static final CoinMasterPurchaseRequest getPurchaseRequest(final int itemId) {
    return getPurchaseRequest(COINMASTER_ITEMS.get(itemId));
  }

  public static final CoinMasterPurchaseRequest getPurchaseRequest(final ShopRow shopRow) {
    return getPurchaseRequest(COINMASTER_ROWS.get(shopRow));
  }

  public static final CoinMasterPurchaseRequest getPurchaseRequest(
      CoinMasterPurchaseRequest request) {
    if (request == null) {
      return null;
    }

    // *** For testing
    if (request.getData().isDisabled()) {
      return null;
    }

    request.setLimit(request.affordableCount());
    request.setCanPurchase();

    return request;
  }

  public static final boolean contains(final int itemId) {
    return contains(itemId, true);
  }

  public static final boolean contains(final int itemId, boolean validate) {
    CoinMasterPurchaseRequest item = getPurchaseRequest(itemId);
    return item != null && (!validate || item.availableItem());
  }

  // *** For testing
  public static final CoinMasterPurchaseRequest findPurchaseRequest(final AdventureResult item) {
    return COINMASTER_ITEMS.get(item.getItemId());
  }

  public static final void addPurchaseRequest(
      final AdventureResult item, final CoinMasterPurchaseRequest request) {
    COINMASTER_ITEMS.put(item.getItemId(), request);
  }

  public static final void removePurchaseRequest(final AdventureResult item) {
    COINMASTER_ITEMS.remove(item.getItemId());
  }
}
