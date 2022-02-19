package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PriceToAcquireTest {

  // Unit tests for the following methods from InventoryManager:
  //
  // int itemValue(AdventureResult item, boolean exact)
  // int priceToAcquire(AdventureResult item, int quantity, boolean exact)
  // int priceToMake(AdventureResult item, int quantity, boolean exact)
  // boolean cheaperToBuy(AdventureResult item, int quantity)
  //
  // And the following variation, used exactly once in retrieveItem:
  // (or should I say, "called exactly once with mallPriceOnly = true)
  //
  // int priceToMake(AdventureResult item, int quantity, boolean exact, boolean mallPriceOnly)

  private static Cleanups mockGetMallPrice(
      Map<Integer, Integer> prices, Map<Integer, Integer> oldPrices) {
    var mocked = mockStatic(MallPriceManager.class);
    mocked
        .when(() -> MallPriceManager.getMallPrice(any(AdventureResult.class)))
        .thenAnswer(
            invocation -> {
              Object[] arguments = invocation.getArguments();
              AdventureResult item = (AdventureResult) arguments[0];
              int itemId = item.getItemId();
              int count = item.getCount();
              int price = prices.get(itemId);
              return price;
            });
    mocked
        .when(() -> MallPriceManager.getMallPrice(any(AdventureResult.class), anyFloat()))
        .thenAnswer(
            invocation -> {
              Object[] arguments = invocation.getArguments();
              AdventureResult item = (AdventureResult) arguments[0];
              float maxAge = (Float) arguments[1];
              int itemId = item.getItemId();
              int count = item.getCount();
              int price = maxAge > 0.0 ? oldPrices.get(itemId) : prices.get(itemId);
              return price;
            });
    return new Cleanups(mocked::close);
  }

  private static Cleanups mockIsPermittedMethod(Set<Integer> unpermitted) {
    var mocked = mockStatic(ConcoctionDatabase.class, Mockito.CALLS_REAL_METHODS);
    mocked
        .when(() -> ConcoctionDatabase.isPermittedMethod(any(AdventureResult.class)))
        .thenAnswer(
            invocation -> {
              Object[] arguments = invocation.getArguments();
              AdventureResult item = (AdventureResult) arguments[0];
              if (unpermitted.contains(item.getItemId())) {
                return false;
              }
              Concoction conc = ConcoctionPool.get(item);
              CraftingType method = conc.getMixingMethod();
              return (method != CraftingType.NOCREATE);
            });
    return new Cleanups(mocked::close);
  }

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("price to acquire user");

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
    Preferences.setFloat("valueOfInventory", 1.8f);

    CharPaneRequest.setCanInteract(true);
  }

  @AfterEach
  private void afterEach() {
    // Prevent leakage; reset to initial state

    Preferences.setBoolean("autoSatisfyWithStorage", true);
    Preferences.setBoolean("autoSatisfyWithCloset", false);
    Preferences.setBoolean("autoSatisfyWithStash", false);

    KoLConstants.inventory.clear();
    KoLConstants.storage.clear();
    KoLConstants.closet.clear();
  }

  // Test with this concoction
  //
  // drive-by shooting = piscatini + grapefruit
  // grapfruit = (NPC item)
  // piscatini = boxed wine + fish head
  // fish head = (mall item)
  // boxed wine (3) = fermenting powder + bunch of square grapes
  // fermenting powder = (NPC item)
  // bunch of square grapes = (mall item)

  private Map<Integer, Integer> makePriceMap() {
    // Make a map from itemid -> mall price
    // These are actual mall prices from 18 Feb 2022

    Map<Integer, Integer> priceMap = new HashMap<>();
    priceMap.put(ItemPool.DRIVE_BY_SHOOTING, 18850);
    priceMap.put(ItemPool.GRAPEFRUIT, 70);
    priceMap.put(ItemPool.PISCATINI, 21800);
    priceMap.put(ItemPool.FISH_HEAD, 22000);
    priceMap.put(ItemPool.BOXED_WINE, 100);
    priceMap.put(ItemPool.FERMENTING_POWDER, 70);
    priceMap.put(ItemPool.BUNCH_OF_SQUARE_GRAPES, 130);

    return priceMap;
  }

  private Map<Integer, Integer> makeOldPriceMap() {
    // Make a map from itemid -> mall price
    // These are completely made up - except for NPC items

    Map<Integer, Integer> priceMap = new HashMap<>();
    priceMap.put(ItemPool.DRIVE_BY_SHOOTING, 12500);
    priceMap.put(ItemPool.GRAPEFRUIT, 70);
    priceMap.put(ItemPool.PISCATINI, 12000);
    priceMap.put(ItemPool.FISH_HEAD, 10000);
    priceMap.put(ItemPool.BOXED_WINE, 100);
    priceMap.put(ItemPool.FERMENTING_POWDER, 70);
    priceMap.put(ItemPool.BUNCH_OF_SQUARE_GRAPES, 150);

    return priceMap;
  }

  // *** Test for basic test infrastructure: we can mock getting a mall price
  // *** from each of the possible ways provided by MallPriceManager
  //
  // getMallPrice(item, 7.0f) - price must be less than 7 days old
  // getMallPrice(item, 0.0f) - price must be less than 0 days old. I.e., current
  // getMallPrice(item) - something. How is this different than 0.0f? ***TBD***

  @Test
  public void canMockGetMallPrice() {
    Map<Integer, Integer> priceMap = makePriceMap();
    Map<Integer, Integer> oldPriceMap = makeOldPriceMap();
    Cleanups cleanups = mockGetMallPrice(priceMap, oldPriceMap);

    try (cleanups) {
      for (Entry<Integer, Integer> entry : priceMap.entrySet()) {
        int itemid = entry.getKey();
        AdventureResult item = ItemPool.get(itemid, 1);
        int price = entry.getValue();
        int test1 = MallPriceManager.getMallPrice(item);
        assertEquals(price, test1);
        int test2 = MallPriceManager.getMallPrice(item, 0.0f);
        assertEquals(price, test2);
      }
      for (Entry<Integer, Integer> entry : oldPriceMap.entrySet()) {
        int itemid = entry.getKey();
        AdventureResult item = ItemPool.get(itemid, 1);
        int price = entry.getValue();
        int test = MallPriceManager.getMallPrice(item, 7.0f);
        assertEquals(price, test);
      }
    }
  }

  @Test
  public void canMockIsPermittedMethod() {
    AdventureResult item = ItemPool.get(ItemPool.DRIVE_BY_SHOOTING, 0);
    Set<Integer> unpermitted = new HashSet<>();
    try (var cleanups = mockIsPermittedMethod(unpermitted)) {
      assertTrue(ConcoctionDatabase.isPermittedMethod(item));
    }
    unpermitted.add(item.getItemId());
    try (var cleanups = mockIsPermittedMethod(unpermitted)) {
      assertFalse(ConcoctionDatabase.isPermittedMethod(item));
    }
  }

  // *** Tests for itemValue(item, exact)
  //
  // Dependencies:
  //
  // ItemDatabase.getPriceById(itemId) - autosell price
  // MallPriceManager.getMallPrice(item) - current mall price
  // MallPriceManager.getMallPrice(item, 7.0f) - not "too old" mall price
  // Preferences.getFloat("valueOfInventory");

  // *** Tests for priceToAcquire(item, quantity, exact, mallPriceOnly)
  //
  // Dependencies:
  //
  // InventoryManager.getAccessibleCount(item)
  // inventory - source of "on hand" items
  // storage, closet, stash - depend on settings.
  //
  // InventoryManager.itemValue(item, exact)
  // MallPriceManager.getMallPrice(item) - current mall price
  // MallPriceManager.getMallPrice(item, 7.0f) - not "too old" mall price
  // InventoryManager.priceToMake(item, quantity, exact, mallPriceOnly)

  @Test
  public void canFindItemsAnywhere() {
    AdventureResult item = ItemPool.get(ItemPool.DRIVE_BY_SHOOTING, 2);
    Map<Integer, Integer> priceMap = makePriceMap();
    Set<Integer> unpermitted = new HashSet<>();

    Cleanups cleanups =
        new Cleanups(mockGetMallPrice(priceMap, priceMap), mockIsPermittedMethod(unpermitted));
    try (cleanups) {
      // We want to test that items in an "accessible" source will
      // account for (some of) the cost to acquire N of an item.
      //
      // To make this easy, we'll treat the value of such items as autosell price.

      Preferences.setFloat("valueOfInventory", 0.0f);

      // Test that acquiring an item with no items costs Meat
      int one = InventoryManager.priceToAcquire(item, 1, false);
      assertTrue(one > 0);
      int two = InventoryManager.priceToAcquire(item, 2, false);
      assertEquals(two, 2 * one);

      // Put enough of the item into inventory.
      AdventureResult.addResultToList(KoLConstants.inventory, item);
      int price = InventoryManager.priceToAcquire(item, 2, false);
      assertEquals(price, 0);

      // Move the items to the closet.
      AdventureResult.addResultToList(KoLConstants.inventory, item.getNegation());
      AdventureResult.addResultToList(KoLConstants.closet, item);
      Preferences.setBoolean("autoSatisfyWithCloset", false);
      price = InventoryManager.priceToAcquire(item, 2, false);
      assertTrue(price > 0);

      // Move the items to storage.
      AdventureResult.addResultToList(KoLConstants.closet, item.getNegation());
      AdventureResult.addResultToList(KoLConstants.storage, item);
      Preferences.setBoolean("autoSatisfyWithStorage", true);
      price = InventoryManager.priceToAcquire(item, 2, false);
      assertTrue(price == 0);

      // Test that asking for an extra item accounts for available items
      price = InventoryManager.priceToAcquire(item, 3, false);
      assertEquals(price, one);
    }
  }

  // *** Tests for priceToMake(item, quantity, exact, mallPriceOnly)
  //
  // Dependencies:
  //
  // ConcoctionDatabase.isPermittedMethod(item)
  // (mixing method and prerequisites must be available)
  // InventoryManager.priceToAcquire(item, quantity, exact, mallPriceOnly)

  // *** Tests for cheaperToBuy(item, quantity)
  //
  // Dependencies:
  //
  // MallPriceManager.getMallPrice(item, 7.0f)
  // InventoryManager.priceToMake(item, quantity, false)
  // MallPriceManager.getMallPrice(item)
  // InventoryManager.priceToMake(item, quantity, true)
}
