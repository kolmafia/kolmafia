package net.sourceforge.kolmafia.session;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

public abstract class MallPriceManager {

  // Mall prices are timestamped. We use System.currentTimeMillis() to generate them.
  // This makes it difficult to write tests that allow us to inject the
  // timestamp of our choice.
  //
  // Solution: use a Java Clock object (introduced in Java 8) which can be referenced by:
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

  // KoL allows users to search the mall in several ways.
  //
  // - Single items with exact name in quotes
  // - Multiple items with a substring
  // - All the items in a category

  // Here are the values of "category" that KoL supports

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

  // This package makes MallSearchRequests and executes them.  This makes
  // testing difficult; we have testing infrastructure for testing
  // request classes, but that's not what we want to test here.
  //
  // Provide methods to create a MallSearchRequest which can be mocked.

  // A Mall search in which you supply:
  //
  // searchString - The string (including wildcards) for the item to be found
  // maximumResults - How many stores to show; non-positive number to show all
  // results -  The list in which to store the results
  //
  // The searchString can be a substring, resulting in results for multiple
  // items, or an exact name (in quotes), giving results for just one item
  //
  // The results list should initially be empty, but since KoL can return
  // multiple pages of results, KoLmafia will reuse the request to fetch all of
  // the pages and will add all of them to the list.

  public static MallSearchRequest newMallSearchRequest(
      String searchString, int maximumResults, List<PurchaseRequest> results) {
    return new MallSearchRequest(searchString, maximumResults, results);
  }

  // A Mall search in which you supply:
  //
  // category - The category of items to search for.
  // tiers - The set of quality "tiers" to include.
  //
  // We validate the category name, since, if invalid, it results in a search
  // for "allitems".
  //
  // We don't validate the "tiers" string. It's free format - although
  // comma-separated names works well. The following "tiers" are recognized:
  // crappy, decent, good, awesome, EPIC

  public static MallSearchRequest newMallSearchRequest(String category, String tiers) {
    return new MallSearchRequest(category, tiers);
  }

  // The data structures that this package "manages".

  // a Map from itemId -> current mall price (as visible to a scripter.)
  private static final Map<Integer, Integer> mallPrices = new HashMap<>();

  // a Map from itemId -> the most resent mall search results.
  private static final Map<Integer, List<PurchaseRequest>> mallSearches = new HashMap<>();

  // Constants controlling how we manage those data

  // In order to inhibit the writing of "mallbots" - processes which
  // repeatedly search for the cheapest offer of a valuable item (like a
  // Mr. Accessory) in order to snatch up an erroneous mispriced item -
  // mall_price() returns the price you'd pay for the Nth item of the
  // sort you could buy. In particular, the 5th cheapest.
  public static int NTH_CHEAPEST_PRICE = 5;

  // How many stores to request results for in a mall search.  It should be at
  // least NTH_CHEAPEST_PRICE, but stores can have limits and can ignore you
  // (or vice versa), it could be higher.
  public static int MALL_SEARCH_RESULTS = 5;

  // How many seconds before a before a "saved search" is "stale"
  public static int MALL_SEARCH_FRESHNESS = 15;

  // For testing
  public static void reset() {
    mallPrices.clear();
    mallSearches.clear();
  }

  private static boolean removeShopPurchaseRequest(
      int itemId, final int shopId, List<PurchaseRequest> search) {
    Iterator<PurchaseRequest> i = search.iterator();
    while (i.hasNext()) {
      PurchaseRequest purchase = i.next();
      if (purchase instanceof MallPurchaseRequest) {
        MallPurchaseRequest mallPurchase = (MallPurchaseRequest) purchase;
        if (shopId == mallPurchase.getShopId()) {
          i.remove();
          MallPriceManager.updateMallPrice(ItemPool.get(itemId), search);
          return true;
        }
      }
    }
    return false;
  }

  public static final void flushCache(final int itemId, final int shopId) {
    // Remove shop from search results for a single item
    if (itemId != -1) {
      List<PurchaseRequest> search = MallPriceManager.mallSearches.get(itemId);
      if (search != null && MallPriceManager.removeShopPurchaseRequest(itemId, shopId, search)) {
        if (search.size() == 0) {
          MallPriceManager.mallSearches.remove(itemId);
        }
      }
      return;
    }

    // Remove shop from search results for all items
    Iterator<Entry<Integer, List<PurchaseRequest>>> i =
        MallPriceManager.mallSearches.entrySet().iterator();
    while (i.hasNext()) {
      Entry<Integer, List<PurchaseRequest>> entry = i.next();
      int key = entry.getKey();
      List<PurchaseRequest> search = entry.getValue();
      if (MallPriceManager.removeShopPurchaseRequest(key, shopId, search)) {
        if (search.size() == 0) {
          i.remove();
        }
      }
    }
  }

  public static final void flushCache(final int itemId) {
    List<PurchaseRequest> search = MallPriceManager.mallSearches.get(itemId);
    if (search != null) {
      MallPriceManager.mallSearches.remove(itemId);
      MallPriceManager.mallPrices.put(itemId, 0);
    }
  }

  public static boolean searchIsTooOld(List<PurchaseRequest> search) {
    if (search == null || search.size() == 0) {
      return true;
    }
    long now = MallPriceManager.currentTimeMillis();
    long freshnessLimit = now - MALL_SEARCH_FRESHNESS * 1000;
    long t = search.get(0).getTimestamp();
    return (t < freshnessLimit);
  }

  // For testing
  public static final void saveMallSearch(int itemId, List<PurchaseRequest> results) {
    MallPriceManager.mallSearches.put(itemId, results);
  }

  /** Utility method used to search the mall for a specific item. */
  public static List<PurchaseRequest> getSavedSearch(Integer id, final int needed) {
    // See if we have a saved search for this id
    List<PurchaseRequest> results = MallPriceManager.mallSearches.get(id);

    if (results == null) {
      // Nothing saved
      return null;
    }

    if (results.size() == 0 || MallPriceManager.searchIsTooOld(results)) {
      // Not current
      MallPriceManager.mallSearches.remove(id);
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
    MallSearchRequest request = newMallSearchRequest(formatted, maximumResults, results);
    RequestThread.postRequest(request);

    // Sort the results by price within name, so that NPC stores are in the
    // appropriate place
    Collections.sort(results, PurchaseRequest.nameComparator);

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

  public static final int updateMallPrice(
      final AdventureResult item, final List<PurchaseRequest> results) {
    return MallPriceManager.updateMallPrice(item, results, false);
  }

  public static final int updateMallPrice(
      final AdventureResult item, final List<PurchaseRequest> results, final boolean deferred) {
    if (item.getItemId() < 1) {
      return 0;
    }
    int price = 0;
    int qty = NTH_CHEAPEST_PRICE;
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

    // Note that if qty > 0, we went through the entire list of results but did
    // not find 5 items for sale. We'll save the highest price we saw, if so.
    MallPriceManager.mallPrices.put(item.getItemId(), price);
    if (price > 0) {
      MallPriceDatabase.recordPrice(item.getItemId(), price, deferred);
    }

    return price;
  }

  public static final synchronized int getMallPrice(final AdventureResult item) {
    int itemId = item.getItemId();
    if (itemId < 1
        || (!ItemDatabase.isTradeable(itemId) && !NPCStoreDatabase.contains(itemId, true))) {
      return 0;
    }
    if (MallPriceManager.mallPrices.getOrDefault(itemId, 0) == 0) {
      List<PurchaseRequest> results =
          MallPriceManager.searchMall(item.getInstance(NTH_CHEAPEST_PRICE));
      MallPriceManager.updateMallPrice(item, results);
    }
    return MallPriceManager.mallPrices.getOrDefault(itemId, 0);
  }

  public static int getMallPrice(AdventureResult item, float maxAge) {
    int itemId = item.getItemId();
    int price = MallPriceDatabase.getPrice(itemId);
    if (MallPriceDatabase.getAge(itemId) > maxAge) {
      MallPriceManager.flushCache(itemId);
      MallPriceManager.mallPrices.remove(itemId);
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
        if (MallPriceManager.mallPrices.getOrDefault(itemId, 0) == 0) {
          List<PurchaseRequest> results =
              MallPriceManager.searchMall(item.getInstance(NTH_CHEAPEST_PRICE));
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
    // Validate the category. KoL will accept any category, but unknown
    // categories are the same as "allItems"
    // That takes a long time - and if the caller really wants it, so be it -
    // but don't do it for typos
    if (!MallPriceManager.validCategories.contains(category)) {
      return 0;
    }

    if (GenericRequest.abortIfInFightOrChoice()) {
      return 0;
    }

    // Issue the search request
    MallSearchRequest request = newMallSearchRequest(category, tiers);
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
            Collections.sort(itemResults, PurchaseRequest.priceComparator);
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
        Collections.sort(itemResults, PurchaseRequest.priceComparator);
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
