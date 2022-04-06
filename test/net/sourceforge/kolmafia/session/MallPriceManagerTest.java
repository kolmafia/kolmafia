package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.MallSearchRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class MallPriceManagerTest {

  private static int nextShopId = 1;

  // MallPriceManager, MallPriceDatabase, and MallPurchaseRequest timestamp the
  // result of mall searches. They used to use System.currentTimeMillis() to
  // generate that, but, to enable testing, they now use a Clock object in
  // MallPriceManager to simulate that.
  //
  // We will mock a Clock object and direct MallPriceManager to use our
  // clock, over which we have complete control.

  private static Clock clock = Mockito.mock(Clock.class);

  private static Cleanups mockClock() {
    var mocked = mockStatic(MallPriceManager.class, Mockito.CALLS_REAL_METHODS);
    mocked.when(MallPriceManager::getSystemClock).thenReturn(clock);
    return new Cleanups(mocked::close);
  }

  // MallPriceManager wants to use MallSearchRequest. We need a mock
  // version of that class which will not actually do network I/O.
  //
  // We need to be able to set its responseText and call processResults()
  // on it to convert the search results into a list of PurchaseRequests

  private class MockMallSearchRequest extends MallSearchRequest {

    private String[] responseTexts = null;

    public MockMallSearchRequest(final int storeId) {
      super(storeId);
    }

    public MockMallSearchRequest(final String searchString, final int cheapestCount) {
      super(searchString, cheapestCount);
    }

    public MockMallSearchRequest(
        final String searchString, final int cheapestCount, final List<PurchaseRequest> results) {
      super(searchString, cheapestCount, results);
    }

    public MockMallSearchRequest(final String category, final String tiers) {
      super(category, tiers);
    }

    @Override
    public void setResponseTexts(String... responseTexts) {
      this.responseTexts = responseTexts;
    }

    @Override
    public void run() {
      // If the mall search response has multiple pages, KoLmafia will fetch
      // and parse them all
      if (this.responseTexts != null) {
        for (String responseText : this.responseTexts) {
          this.responseText = responseText;
          this.processResults();
        }
        return;
      }

      // Perhaps there is only a single page response
      if (this.responseText != null) {
        this.processResults();
      }
    }
  }

  private static Cleanups mockMallSearchRequest(MallSearchRequest request) {
    var mocked = mockStatic(MallPriceManager.class, Mockito.CALLS_REAL_METHODS);
    mocked.when(MallPriceManager::getSystemClock).thenReturn(clock);
    mocked
        .when(() -> MallPriceManager.newMallSearchRequest(anyString(), anyInt(), anyList()))
        .thenAnswer(
            invocation -> {
              Object[] arguments = invocation.getArguments();
              String searchString = (String) arguments[0];
              int cheapestCount = (int) arguments[1];
              List<PurchaseRequest> results = (List<PurchaseRequest>) arguments[2];
              // Caller will supply empty results. If mocked request has
              // preloaded results, hand them over.
              results.addAll(request.getResults());
              request.setSearchString(searchString);
              request.setCheapestCount(cheapestCount);
              request.setResults(results);
              return request;
            });
    mocked
        .when(() -> MallPriceManager.newMallSearchRequest(anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              Object[] arguments = invocation.getArguments();
              String category = (String) arguments[0];
              String tiers = (String) arguments[1];
              request.setCategory(category);
              request.setTiers(tiers);
              return request;
            });
    return new Cleanups(mocked::close);
  }

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("mall price manager user");
    CharPaneRequest.setCanInteract(true);
    MallPriceDatabase.savePricesToFile = false;
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    MallPriceDatabase.savePricesToFile = true;
    Preferences.saveSettingsToFile = true;
  }

  @BeforeEach
  private void beforeEach() {
    // Stop requests from actually running
    GenericRequest.sessionId = null;
    KoLCharacter.setAvailableMeat(1_000_000);
    MallPriceManager.reset();
    nextShopId = 1;
  }

  private void addNPCStoreItem(int itemId, List<PurchaseRequest> list) {
    if (NPCStoreDatabase.contains(itemId, false)) {
      PurchaseRequest item = NPCStoreDatabase.getPurchaseRequest(itemId);
      list.add(item);
    }
  }

  private void addCoinMasterItem(int itemId, List<PurchaseRequest> list) {
    PurchaseRequest item = CoinmastersDatabase.getPurchaseRequest(itemId);
    if (item != null) {
      list.add(item);
    }
  }

  private PurchaseRequest makeMallItem(int itemId, int quantity, int price) {
    return makeMallItem(itemId, quantity, price, quantity);
  }

  private PurchaseRequest makeMallItem(int itemId, int quantity, int price, int limit) {
    return makeMallItem(itemId, quantity, price, limit, nextShopId++);
  }

  private PurchaseRequest makeMallItem(int itemId, int quantity, int price, int limit, int shopId) {
    String shopName = "shop " + String.valueOf(shopId);
    PurchaseRequest item =
        new MallPurchaseRequest(itemId, quantity, shopId, shopName, price, limit, true);
    return item;
  }

  private void addSearchResults(AdventureResult item, List<PurchaseRequest> results) {
    Collections.sort(results);
    MallPriceManager.saveMallSearch(item.getItemId(), results);
    MallPriceManager.updateMallPrice(item, results);
  }

  private List<PurchaseRequest> generateSearchResults(AdventureResult item, int[] prices) {
    int itemId = item.getItemId();
    List<PurchaseRequest> retval =
        Arrays.stream(prices)
            .mapToObj(price -> makeMallItem(itemId, 1, price))
            .collect(Collectors.toList());
    return retval;
  }

  private int[] getTestPrices() {
    // count  mall price    actual   current   better
    // -----  ----------  ---------   ------   ------
    //   1        100         100       500      500
    //   2        200         300      1000     1000
    //   3        300         600      1500     1500
    //   4        400        1000      2000     2000
    //   5        500        1500      2500     2500
    //   6       1000        2500      3000     3500
    //   7       1000        3500      3500     4500
    //   8       1000        4500      4000     5500
    //   9       1000        5500      4500     6500
    //  10       1000        6500      5000     7500
    //  11       5000       11500      5500    12500
    //  12       5000       16500      6000    17500
    //  13       5000       21500      6500    22500
    //  14       5000       26500      7000    27500
    //  15       5000       31500      7500    32500

    return new int[] {
      100, 200, 300, 400, 500, 1000, 1000, 1000, 1000, 1000, 5000, 5000, 5000, 5000, 5000
    };
  }

  @Test
  public void canFindNthCheapestPrice() {
    AdventureResult item = ItemPool.get(ItemPool.REAGENT);
    int[] prices = getTestPrices();
    List<PurchaseRequest> results = generateSearchResults(item, prices);
    assertEquals(15, results.size());
    addSearchResults(item, results);
    assertEquals(500, MallPriceManager.getMallPrice(item));
  }

  @Test
  public void canFindNPCPurchaseRequest() {
    try (var cleanups = mockClock()) {
      List<PurchaseRequest> results = new ArrayList<>();

      // This is available from an NPC store
      int itemId = ItemPool.DINGY_PLANKS;
      AdventureResult item = ItemPool.get(itemId, 1);

      // Add 5 Mall stores and an NPC store that carry the item
      long timestamp = 1_000_000;

      Mockito.when(clock.millis()).thenReturn(timestamp);
      results.add(makeMallItem(itemId, 3, 250, 1));
      results.add(makeMallItem(itemId, 1, 300));
      results.add(makeMallItem(itemId, 1, 350));
      results.add(makeMallItem(itemId, 1, 375));
      results.add(makeMallItem(itemId, 10, 500));
      addNPCStoreItem(itemId, results);

      // Sort the results and update the mall prices
      addSearchResults(item, results);

      // This item is a NPC item available for the 5th cheapest price
      int mallPrice = MallPriceManager.getMallPrice(item);
      assertEquals(mallPrice, 400);
    }
  }

  @Test
  public void canFlushSpecificShopPurchaseRequests() {
    try (var cleanups = mockClock()) {
      // Stock multiple stores with a single item

      List<PurchaseRequest> results = new ArrayList<>();

      // This is not available from an NPC store
      int itemId = ItemPool.REAGENT;
      AdventureResult item = ItemPool.get(itemId, 1);

      // Add 6 Mall stores that carry the item
      long timestamp = 1_000_000;

      // One of them is special
      int shopId = 100;

      Mockito.when(clock.millis()).thenReturn(timestamp);
      results.add(makeMallItem(itemId, 3, 250, 1, shopId));
      results.add(makeMallItem(itemId, 1, 300));
      results.add(makeMallItem(itemId, 1, 350));
      results.add(makeMallItem(itemId, 1, 375));
      results.add(makeMallItem(itemId, 1, 400));
      results.add(makeMallItem(itemId, 10, 500));

      // Sort the results and update the mall prices
      addSearchResults(item, results);

      // Find the fifth cheapest price
      int mallPrice = MallPriceManager.getMallPrice(item);
      assertEquals(mallPrice, 400);

      // Flush the first shop's PurchaseRequest
      MallPriceManager.flushCache(itemId, shopId);

      // Find the fifth cheapest price
      mallPrice = MallPriceManager.getMallPrice(item);
      assertEquals(mallPrice, 500);
    }
  }

  @Test
  public void canFlushMultipleItemShopPurchaseRequests() {
    // Stock multiple stores with multiple items

    int itemId1 = ItemPool.REAGENT;
    AdventureResult item1 = ItemPool.get(itemId1, 1);
    int itemId2 = ItemPool.DRY_NOODLES;
    AdventureResult item2 = ItemPool.get(itemId2, 1);
    int itemId3 = ItemPool.MUSHROOM_PIZZA;
    AdventureResult item3 = ItemPool.get(itemId3, 1);

    // A shop with no inventory
    int shopId0 = 1000;
    int shopId1 = 100;
    int shopId2 = 200;
    int shopId3 = 300;
    int shopId4 = 400;

    // Add Mall stores that carry the 3 items
    MallSearchRequest request = new MockMallSearchRequest("", 0);
    try (var cleanups = mockMallSearchRequest(request)) {
      long timestamp = 1_000_000;
      Mockito.when(clock.millis()).thenReturn(timestamp);

      List<PurchaseRequest> results1 = new ArrayList<>();
      results1.add(makeMallItem(itemId1, 10, 250, 10, shopId1));
      results1.add(makeMallItem(itemId1, 10, 300, 10, shopId2));
      addSearchResults(item1, results1);
      assertNotNull(MallPriceManager.getSavedSearch(itemId1, 1));

      List<PurchaseRequest> results2 = new ArrayList<>();
      results2.add(makeMallItem(itemId2, 10, 350, 10, shopId1));
      results2.add(makeMallItem(itemId2, 10, 400, 10, shopId3));
      addSearchResults(item2, results2);
      assertNotNull(MallPriceManager.getSavedSearch(itemId2, 1));

      List<PurchaseRequest> results3 = new ArrayList<>();
      results3.add(makeMallItem(itemId3, 10, 450, 10, shopId1));
      results3.add(makeMallItem(itemId3, 10, 500, 10, shopId4));
      addSearchResults(item3, results3);
      assertNotNull(MallPriceManager.getSavedSearch(itemId3, 1));

      // Verify that all items have a price
      assertEquals(250, MallPriceManager.getMallPrice(item1));
      assertEquals(350, MallPriceManager.getMallPrice(item2));
      assertEquals(450, MallPriceManager.getMallPrice(item3));

      // Flush non-stocked shop from a single item
      MallPriceManager.flushCache(itemId1, shopId0);

      // Verify that that item's price is unchanged
      assertNotNull(MallPriceManager.getSavedSearch(itemId1, 1));
      assertEquals(250, MallPriceManager.getMallPrice(item1));

      // Flush non-stocked shop from all items
      MallPriceManager.flushCache(-1, shopId0);

      // Verify that all items have a price
      assertEquals(250, MallPriceManager.getMallPrice(item1));
      assertEquals(350, MallPriceManager.getMallPrice(item2));
      assertEquals(450, MallPriceManager.getMallPrice(item3));

      // Test flushing last shop with a purchase request

      // Verify that item1 has a saved search
      assertNotNull(MallPriceManager.getSavedSearch(itemId1, 1));

      // Flush PurchaseRequests from both shops for item1
      MallPriceManager.flushCache(itemId1, shopId1);
      assertNotNull(MallPriceManager.getSavedSearch(itemId1, 1));
      assertEquals(300, MallPriceManager.getMallPrice(item1));

      MallPriceManager.flushCache(itemId1, shopId2);
      assertNull(MallPriceManager.getSavedSearch(itemId1, 1));

      // Test flushing all mall prices for a shop
      MallPriceManager.flushCache(-1, shopId1);

      // Verify that item2 and item3 have saved mall searches without shop1
      assertNotNull(MallPriceManager.getSavedSearch(itemId2, 1));
      assertEquals(400, MallPriceManager.getMallPrice(item2));

      assertNotNull(MallPriceManager.getSavedSearch(itemId3, 1));
      assertEquals(500, MallPriceManager.getMallPrice(item3));
    }
  }

  @Test
  public void canFlushPurchaseRequestsByItem() {
    List<PurchaseRequest> results = new ArrayList<>();
    MallSearchRequest request = new MockMallSearchRequest("", 0, results);

    try (var cleanups = mockMallSearchRequest(request)) {
      // Add 5 Mall stores that carry the item
      long timestamp = 1_000_000;
      Mockito.when(clock.millis()).thenReturn(timestamp);

      // This is not available from an NPC store
      int itemId = ItemPool.REAGENT;
      AdventureResult item = ItemPool.get(itemId, 1);

      results.add(makeMallItem(itemId, 1, 300));
      results.add(makeMallItem(itemId, 1, 350));
      results.add(makeMallItem(itemId, 1, 375));
      results.add(makeMallItem(itemId, 1, 400));
      results.add(makeMallItem(itemId, 10, 500));

      // Sort the results and update the mall prices
      addSearchResults(item, results);

      // Verify that there is a saved mall search
      // (1 means "we have to be able to afford 1 item")
      List<PurchaseRequest> search = MallPriceManager.getSavedSearch(itemId, 1);
      assertNotNull(search);

      // Verify that we have a saved "5th lowest" mall price
      int price = MallPriceManager.getMallPrice(item);
      assertEquals(500, price);

      // Flush the PurchaseRequests for this item
      MallPriceManager.flushCache(itemId);

      // Verify that there is no longer a saved mall search
      search = MallPriceManager.getSavedSearch(itemId, 0);
      assertNull(search);

      // Verify that asking for price will do another mall search
      price = MallPriceManager.getMallPrice(item);
      search = MallPriceManager.getSavedSearch(itemId, 0);
      assertNotNull(search);
      assertEquals(500, price);
    }
  }

  @Test
  public void canFlushStalePurchaseRequests() {
    try (var cleanups = mockClock()) {
      List<PurchaseRequest> results = new ArrayList<>();

      // This is not available from an NPC store
      int itemId = ItemPool.REAGENT;
      AdventureResult item = ItemPool.get(itemId, 1);

      // Add 5 Mall stores that carry the item
      long timestamp = 1_000_000;

      Mockito.when(clock.millis()).thenReturn(timestamp);
      results.add(makeMallItem(itemId, 1, 300));
      results.add(makeMallItem(itemId, 1, 350));
      results.add(makeMallItem(itemId, 1, 375));
      results.add(makeMallItem(itemId, 1, 400));
      results.add(makeMallItem(itemId, 10, 500));

      // Sort the results and update the mall prices
      addSearchResults(item, results);

      // Verify that there is a saved mall search
      // (1 means "we have to be able to afford 1 item")
      List<PurchaseRequest> search = MallPriceManager.getSavedSearch(itemId, 1);
      assertNotNull(search);

      // Spend all our Meat
      KoLCharacter.setAvailableMeat(0);

      // Verify that there is a saved mall search
      // (0 means "we don't care how many we can buy")
      search = MallPriceManager.getSavedSearch(itemId, 0);
      assertNotNull(search);

      // Verify that there is no search that we can afford
      // (1 means "we have to be able to afford 1 item")
      search = MallPriceManager.getSavedSearch(itemId, 1);
      assertNull(search);

      // Advance time such that the timestamp is now 5 seconds stale :)
      long now = timestamp + (MallPriceManager.MALL_SEARCH_FRESHNESS + 5) * 1000;
      Mockito.when(clock.millis()).thenReturn(now);

      // Verify that there is no longer a saved mall search
      search = MallPriceManager.getSavedSearch(itemId, 0);
      assertNull(search);
    }
  }

  @Test
  public void canHandleMallSearchesWithNoResults() {
    List<PurchaseRequest> results = new ArrayList<>();
    MallSearchRequest request = new MockMallSearchRequest("", 0, results);

    try (var cleanups = mockMallSearchRequest(request)) {
      long timestamp = 1_000_000;
      Mockito.when(clock.millis()).thenReturn(timestamp);

      // Any old item will do; our mocked MallSearchRequest will not find it.
      int itemId = ItemPool.REAGENT;
      AdventureResult item = ItemPool.get(itemId, 1);

      // Sort the results and update the mall prices
      addSearchResults(item, results);

      // Verify that there is no saved mall search
      // (1 means "we have to be able to afford 1 item")
      List<PurchaseRequest> search = MallPriceManager.getSavedSearch(itemId, 1);
      assertNull(search);

      // Verify that the saved price is -1, which means "not in the mall"
      int price = MallPriceManager.getMallPrice(item);
      assertEquals(-1, price);
    }
  }

  // *** Need tests for getMallPrice(AdventureResult item, float maxAge)

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  @Test
  public void canGetMallPricesViaMallSearch() throws IOException {

    // Test with Hell ramen.
    AdventureResult item = ItemPool.get(ItemPool.HELL_RAMEN);

    // Make a mocked MallSearchResponse with the responseText preloaded

    MallSearchRequest request = new MockMallSearchRequest("Hell ramen", 0);
    request.responseText = loadHTMLResponse("request/test_mall_search_hell_ramen.html");

    try (var cleanups = mockMallSearchRequest(request)) {
      long timestamp = 1_000_000;
      Mockito.when(clock.millis()).thenReturn(timestamp);

      List<PurchaseRequest> results = MallPriceManager.searchMall(item);

      // The MallSearchRequest found a bunch of PurchaseRequests from the responseText
      assertEquals(results.size(), 60);
    }
  }

  @Test
  public void canGetMallPricesByCategory() throws IOException {
    // Test with category = "unlockers" since that only has two pages of results
    MallSearchRequest request = new MockMallSearchRequest("unlockers", "");
    request.setResponseTexts(
        loadHTMLResponse("request/test_mall_search_unlockers_page_1.html"),
        loadHTMLResponse("request/test_mall_search_unlockers_page_2.html"));

    try (var cleanups = mockMallSearchRequest(request)) {
      long timestamp = 1_000_000;
      Mockito.when(clock.millis()).thenReturn(timestamp);

      // MallSearchRequest will accumulate all of the PurchaseRequests seen on
      // all responseTexts into the "results" field of the request.
      // It will then update prices and return how many
      int count = MallPriceManager.getMallPrices("unlockers", "");
      assertEquals(count, 32);
    }
  }

  @Test
  public void canSearchMallStore() throws IOException {
    // Not actually used in MallPriceManager, but may as well test the fourth
    // (last) form of a MallSearchRequest

    // This is Clerk's - one of the bigger stores. :)
    MallSearchRequest request = new MockMallSearchRequest(1053259);
    request.setResponseTexts(loadHTMLResponse("request/test_mall_search_store.html"));

    try (var cleanups = mockMallSearchRequest(request)) {
      long timestamp = 1_000_000;
      Mockito.when(clock.millis()).thenReturn(timestamp);

      // Process the response text into PurchaseRequests
      request.run();

      List<PurchaseRequest> results = request.getResults();
      assertEquals(4521, results.size());
    }
  }
}
