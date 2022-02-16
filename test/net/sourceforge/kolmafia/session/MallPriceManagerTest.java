package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.request.CharPaneRequest;
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
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("mall price manager user");
    CharPaneRequest.setCanInteract(true);
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
  public void canFlushPurchaseRequestsByItem() {
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

      // Flush the PurchaseRequests for this item
      MallPriceManager.flushCache(itemId);

      // Verify that there is no longer a saved mall search
      search = MallPriceManager.getSavedSearch(itemId, 0);
      assertNull(search);
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

      // Advance time by 20 seconds :)
      long now = timestamp + (20 * 1000);
      Mockito.when(clock.millis()).thenReturn(now);

      // Flush the PurchaseRequests that are more than 15 seconds old
      MallPriceManager.flushCache();

      // Verify that there is no longer a saved mall search
      search = MallPriceManager.getSavedSearch(itemId, 0);
      assertNull(search);
    }
  }
}
