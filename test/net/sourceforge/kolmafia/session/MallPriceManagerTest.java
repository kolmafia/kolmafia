package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
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

  @BeforeAll
  private static void beforeAll() {
    nextShopId = 1;
  }

  @BeforeEach
  private void beforeEach() {
    // Stop requests from actually running
    GenericRequest.sessionId = null;
    MallPriceManager.reset();
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

  private PurchaseRequest makeMallItem(int itemId, int quantity, int price, int limit) {
    int shopId = nextShopId++;
    String shopName = "shop " + String.valueOf(shopId);
    PurchaseRequest item =
        new MallPurchaseRequest(itemId, quantity, shopId, shopName, price, limit, true);
    return item;
  }

  private ArrayList<PurchaseRequest> createSearchResults(int itemId, long timestamp) {
    ArrayList<PurchaseRequest> results = new ArrayList<>();

    try (var cleanups = mockClock()) {
      Mockito.when(clock.millis()).thenReturn(timestamp);
      results.add(makeMallItem(itemId, 3, 250, 1));
      results.add(makeMallItem(itemId, 1, 300, 1));
      results.add(makeMallItem(itemId, 1, 350, 1));
      results.add(makeMallItem(itemId, 1, 375, 1));
      results.add(makeMallItem(itemId, 10, 500, 10));
      addNPCStoreItem(itemId, results);
    }

    Collections.sort(results);

    return results;
  }

  @Test
  public void canAddMallSearchResults() {
    int itemId = ItemPool.DINGY_PLANKS;
    AdventureResult item = ItemPool.get(itemId, 1);
    long timestamp = 1_000_000;
    ArrayList<PurchaseRequest> results = createSearchResults(itemId, timestamp);
    MallPriceManager.updateMallPrice(item, results);
    int mallPrice = MallPriceManager.getMallPrice(item);
    // This item is a NPC item available for the 5th cheapest price
    assertEquals(mallPrice, 400);
  }
}
