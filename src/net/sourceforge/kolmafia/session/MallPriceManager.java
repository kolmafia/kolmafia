package net.sourceforge.kolmafia.session;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.request.CoinMasterPurchaseRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.utilities.IntegerArray;

public abstract class MallPriceManager {

  // Mall prices are timestamped. We use System.currentTimeMillis() to generate them.
  // Unfortunately, this makes it difficult to write tests that allow us to inject the timestamp of
  // our choice.
  //
  // The solution is to use a Java Clock object (introduced in Java 8) which can be referenced by:
  //
  // MallPriceManager
  // MallPriceDatabase
  // MallPurchaseRequest

  private static Clock clock = Clock.systemUTC();

  public static Clock getSystemClock() {
    return MallPriceManager.clock;
  }

  public static long currentTimeMillis() {
    return MallPriceManager.getSystemClock().millis();
  }

  private static final IntegerArray mallPrices = new IntegerArray();
  private static final LinkedHashMap<Integer, List<PurchaseRequest>> mallSearches =
      new LinkedHashMap<>();

  // For testing
  public static void reset() {
    // mallPrices.clear();
    mallSearches.clear();
  }

  public static final String[] CATEGORY_VALUES = {
    "allitems", // All Categories
    // Consumables
    "food", // Food and Beverages
    "booze", // Booze
    "othercon", // Other Consumables
    // Equipment
    "weapons", // Weapons
    "hats", // Hats
    "shirts", // Shirts
    "container", // Back Items
    "pants", // Pants
    "acc", // Accessories
    "offhand", // Off-hand Items
    "famequip", // Familiar Equipment
    // Usable
    "combat", // Combat Items
    "potions", // Potions
    "hprestore", // HP Restorers
    "mprestore", // MP Restorers
    "familiars", // Familiars
    // Miscellaneous
    "mrstore", // Mr. Store Items
    "unlockers", // Content Unlockers
    "new", // New Stuff
  };

  public static final Set<String> validCategories = new HashSet<>(Arrays.asList(CATEGORY_VALUES));

  public static final void flushCache(final int itemId, final int shopId) {
    Iterator<List<PurchaseRequest>> i1 = MallPriceManager.mallSearches.values().iterator();
    while (i1.hasNext()) {
      List<PurchaseRequest> search = i1.next();

      // Always remove empty searches
      if (search == null || search.size() == 0) {
        i1.remove();
        continue;
      }

      if (itemId != -1 && search.get(0).getItemId() != itemId) {
        continue;
      }

      Iterator<PurchaseRequest> i2 = search.iterator();
      while (i2.hasNext()) {
        PurchaseRequest purchase = i2.next();
        if (purchase instanceof MallPurchaseRequest
            && shopId == ((MallPurchaseRequest) purchase).getShopId()) {
          i2.remove();
          MallPriceManager.updateMallPrice(ItemPool.get(itemId), search);
          if (itemId != -1) {
            return;
          }
          break;
        }
      }
    }
  }

  public static final void flushCache(final int itemId) {
    Iterator<List<PurchaseRequest>> i = MallPriceManager.mallSearches.values().iterator();
    while (i.hasNext()) {
      List<PurchaseRequest> search = i.next();
      // Always remove empty searches
      if (search == null || search.size() == 0) {
        i.remove();
        continue;
      }
      int id = search.get(0).getItemId();
      if (itemId == id) {
        i.remove();
        MallPriceManager.updateMallPrice(ItemPool.get(itemId), search);
        return;
      }
      break;
    }
  }

  public static final void flushCache() {
    long t0, t1;
    t1 = MallPriceManager.currentTimeMillis();
    t0 = t1 - 15 * 1000;

    Iterator<List<PurchaseRequest>> i = MallPriceManager.mallSearches.values().iterator();
    while (i.hasNext()) {
      List<PurchaseRequest> search = i.next();
      if (search == null || search.size() == 0) {
        i.remove();
        continue;
      }
      long t = search.get(0).getTimestamp();
      if (t < t0 || t > t1) {
        i.remove();
        continue;
      }
      break;
    }
  }

  /** Utility method used to search the mall for a specific item. */
  private static List<PurchaseRequest> getSavedSearch(Integer id, final int needed) {
    // Remove search results that are too old
    MallPriceManager.flushCache();

    // See if we have a saved search for this id
    List<PurchaseRequest> results = MallPriceManager.mallSearches.get(id);

    if (results == null) {
      // Nothing saved
      return null;
    }

    if (results.size() == 0) {
      // Nothing found last time we looked
      return null;
    }

    // If we don't care how many are available, any saved search is
    // good enough
    if (needed == 0) {
      return results;
    }

    // See if the saved search will let you purchase enough of the item
    int available = 0;

    for (PurchaseRequest result : results) {
      // If we can't use this request, ignore it
      if (!result.canPurchase()) {
        continue;
      }

      int count = result.getQuantity();

      // If there is an unlimited number of this item
      // available (because this is an NPC store), that is
      // enough for anybody
      if (count == PurchaseRequest.MAX_QUANTITY) {
        return results;
      }

      // Accumulate available count
      available += count;

      // If we have found enough available items, this search
      // is good enough
      if (available >= needed) {
        return results;
      }
    }

    // Not enough
    return null;
  }

  public static final List<PurchaseRequest> searchMall(final AdventureResult item) {
    int itemId = item.getItemId();
    int needed = item.getCount();

    if (itemId <= 0) {
      // This should not happen.
      return new ArrayList<PurchaseRequest>();
    }

    Integer id = IntegerPool.get(itemId);
    String name = ItemDatabase.getItemDataName(id);

    List<PurchaseRequest> results = MallPriceManager.getSavedSearch(id, needed);
    if (results != null) {
      KoLmafia.updateDisplay("Using cached search results for " + name + "...");
      return results;
    }

    results = MallPriceManager.searchMall("\"" + name + "\"", 0);

    // Flush CoinMasterPurchaseRequests
    results.removeIf(purchaseRequest -> purchaseRequest instanceof CoinMasterPurchaseRequest);

    if (KoLmafia.permitsContinue()) {
      MallPriceManager.mallSearches.put(id, results);
    }

    return results;
  }

  public static final List<PurchaseRequest> searchOnlyMall(final AdventureResult item) {
    // Get a potentially cached list of search request from both PC and NPC stores,
    // Coinmaster Requests have already been filtered out
    List<PurchaseRequest> allResults = MallPriceManager.searchMall(item);

    // Filter out NPC stores
    List<PurchaseRequest> results = new ArrayList<>();

    for (PurchaseRequest result : allResults) {
      if (result.isMallStore) {
        results.add(result);
      }
    }

    return results;
  }

  public static final List<PurchaseRequest> searchNPCs(final AdventureResult item) {
    List<PurchaseRequest> results = new ArrayList<>();

    int itemId = item.getItemId();
    if (itemId <= 0) {
      // This should not happen.
      return results;
    }

    PurchaseRequest request = NPCStoreDatabase.getPurchaseRequest(itemId);

    if (request != null) {
      results.add(request);
    }

    return results;
  }

  /** Utility method used to search the mall for a search string */
  public static final List<PurchaseRequest> searchMall(
      final String searchString, final int maximumResults) {
    List<PurchaseRequest> results = new ArrayList<>();

    if (searchString == null) {
      return results;
    }

    if (GenericRequest.abortIfInFightOrChoice()) {
      return results;
    }

    // Format the search string
    String formatted = MallSearchRequest.getSearchString(searchString);

    // Issue the search request
    MallSearchRequest request = new MallSearchRequest(formatted, maximumResults, results, true);
    RequestThread.postRequest(request);

    // Sort the results by price, so that NPC stores are in the
    // appropriate place
    Collections.sort(results);

    return results;
  }

  public static final void searchMall(
      final String searchString, final int maximumResults, final List<String> resultSummary) {
    resultSummary.clear();

    if (searchString == null) {
      return;
    }

    List<PurchaseRequest> results = MallPriceManager.searchMall(searchString, maximumResults);
    PurchaseRequest[] resultsArray = results.toArray(new PurchaseRequest[0]);
    TreeMap<Integer, Integer> prices = new TreeMap<>();

    for (int i = 0; i < resultsArray.length; ++i) {
      PurchaseRequest result = resultsArray[i];
      if (result instanceof CoinMasterPurchaseRequest) {
        continue;
      }

      Integer currentPrice = IntegerPool.get(result.getPrice());
      Integer currentQuantity = prices.get(currentPrice);

      if (currentQuantity == null) {
        prices.put(currentPrice, IntegerPool.get(resultsArray[i].getLimit()));
      } else {
        prices.put(
            currentPrice, IntegerPool.get(currentQuantity.intValue() + resultsArray[i].getLimit()));
      }
    }

    Integer[] priceArray = new Integer[prices.size()];
    prices.keySet().toArray(priceArray);

    for (int i = 0; i < priceArray.length; ++i) {
      resultSummary.add(
          "  "
              + KoLConstants.COMMA_FORMAT.format(prices.get(priceArray[i]).intValue())
              + " @ "
              + KoLConstants.COMMA_FORMAT.format(priceArray[i].intValue())
              + " meat");
    }
  }

  public static final void maybeUpdateMallPrice(
      final AdventureResult item, final List<PurchaseRequest> results) {
    if (MallPriceManager.mallPrices.get(item.getItemId()) == 0) {
      MallPriceManager.updateMallPrice(item, results);
    }
  }

  public static final int updateMallPrice(
      final AdventureResult item, final List<PurchaseRequest> results) {
    return MallPriceManager.updateMallPrice(item, results, false);
  }

  public static final int updateMallPrice(
      final AdventureResult item, final List<PurchaseRequest> results, final boolean deferred) {
    if (item.getItemId() < 1) {
      return 0;
    }
    int price = -1;
    int qty = 5;
    for (PurchaseRequest req : results) {
      if (req instanceof CoinMasterPurchaseRequest || !req.canPurchaseIgnoringMeat()) {
        continue;
      }
      price = req.getPrice();
      qty -= req.getLimit();
      if (qty <= 0) {
        break;
      }
    }
    MallPriceManager.mallPrices.set(item.getItemId(), price);
    if (price > 0) {
      MallPriceDatabase.recordPrice(item.getItemId(), price, deferred);
    }

    return price;
  }

  public static final synchronized int getMallPrice(final AdventureResult item) {
    MallPriceManager.flushCache();
    int itemId = item.getItemId();
    if (itemId < 1
        || (!ItemDatabase.isTradeable(itemId) && !NPCStoreDatabase.contains(itemId, true))) {
      return 0;
    }
    if (MallPriceManager.mallPrices.get(itemId) == 0) {
      List<PurchaseRequest> results = MallPriceManager.searchMall(item.getInstance(5));
      MallPriceManager.updateMallPrice(item, results);
    }
    return MallPriceManager.mallPrices.get(itemId);
  }

  public static int getMallPrice(AdventureResult item, float maxAge) {
    int id = item.getItemId();
    int price = MallPriceDatabase.getPrice(id);
    if (MallPriceDatabase.getAge(id) > maxAge) {
      MallPriceManager.flushCache(id);
      MallPriceManager.mallPrices.set(id, 0);
      price = 0;
    }
    if (price <= 0) {
      price = MallPriceManager.getMallPrice(item);
    }
    return price;
  }

  public static int getMallPrices(AdventureResult[] items, float maxAge) {
    // Count how many items we retrieved
    int count = 0;

    try {
      for (AdventureResult item : items) {
        int itemId = item.getItemId();
        if (itemId < 1
            || (!ItemDatabase.isTradeable(itemId) && !NPCStoreDatabase.contains(itemId, true))) {
          continue;
        }
        int price = MallPriceDatabase.getPrice(itemId);
        if (price > 0 && MallPriceDatabase.getAge(itemId) <= maxAge) {
          continue;
        }
        if (MallPriceManager.mallPrices.get(itemId) == 0) {
          List<PurchaseRequest> results = MallPriceManager.searchMall(item.getInstance(5));
          MallPriceManager.flushCache(itemId);
          MallPriceManager.updateMallPrice(item, results, true);
          MallPriceManager.mallSearches.put(itemId, results);
          ++count;
        }
      }
    } finally {
      RequestLogger.printLine("Updating mallprices.txt with " + count + " prices.");
      MallPriceDatabase.writePrices();
    }

    return count;
  }

  public static int getMallPrices(String category) {
    return getMallPrices(category, "");
  }

  public static int getMallPrices(String category, String tiers) {
    // Validate the category. KoL will accept any category, but unknown categories are the same as
    // "allItems"
    // That takes a LONG time - and if the caller really wants it, so be it - but don't do it for
    // typos
    if (!MallPriceManager.validCategories.contains(category)) {
      return 0;
    }

    if (GenericRequest.abortIfInFightOrChoice()) {
      return 0;
    }

    // Issue the search request
    MallSearchRequest request = new MallSearchRequest(category, tiers);
    RequestThread.postRequest(request);

    List<PurchaseRequest> results = request.getResults();
    if (results.size() == 0) {
      // None found
      return 0;
    }

    // Count how many items we retrieved
    int count = 0;

    try {
      // Iterate over results and handle by item
      int itemId = -1;
      List<PurchaseRequest> itemResults = null;

      for (PurchaseRequest pr : results) {
        if (pr instanceof CoinMasterPurchaseRequest) {
          continue;
        }

        int newItemId = pr.getItemId();
        if (itemId != newItemId) {
          // Handle previous item, if any
          if (itemResults != null) {
            MallPriceManager.flushCache(itemId);
            Collections.sort(itemResults);
            MallPriceManager.updateMallPrice(ItemPool.get(itemId), itemResults, true);
            MallPriceManager.mallSearches.put(itemId, itemResults);
            ++count;
          }

          // Setup for new item
          itemId = newItemId;
          itemResults = new ArrayList<>();
        }

        itemResults.add(pr);
      }

      // Handle final item
      if (itemResults != null) {
        MallPriceManager.flushCache(itemId);
        Collections.sort(itemResults);
        MallPriceManager.updateMallPrice(ItemPool.get(itemId), itemResults, true);
        MallPriceManager.mallSearches.put(itemId, itemResults);
        ++count;
      }
    } finally {
      RequestLogger.printLine("Updating mallprices.txt with " + count + " prices.");
      MallPriceDatabase.writePrices();
    }

    return count;
  }
}
