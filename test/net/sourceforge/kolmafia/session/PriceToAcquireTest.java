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
import net.sourceforge.kolmafia.persistence.ItemDatabase;
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
    CharPaneRequest.setCanInteract(true);
  }

  @AfterEach
  private void afterEach() {
    // Prevent leakage; reset to initial state

    Preferences.setFloat("valueOfInventory", 1.8f);
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
  // getMallPrice(item, InventoryManager.MALL_PRICE_AGE) - price must be less than 7 days old
  // getMallPrice(item) - Current mall price. How is this different than maxAge = 0.0f?

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
        int test = MallPriceManager.getMallPrice(item, InventoryManager.MALL_PRICE_AGE);
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
  // The only documentation for this is Jason Harper's commit message from 2011:
  //
  // r9806 | jasonharper | 2011-09-05 00:04:24 -0400 (Mon, 05 Sep 2011) | 29 lines
  //
  // The decision to buy a completed item rather than creating it from ingredients
  // already in inventory requires assigning a value to those ingredients, which
  // really depends on play style.  Not everyone is going to put in the effort
  // needed to maximize their Mall profits; they might use only autosell to
  // dispose of excess items, or just hoard them.  Therefore, a new float
  // preference "valueOfInventory" allows players to indicate the worth of items,
  // with these key values:
  //
  // 0.0 - Items already in inventory are considered free.
  // 1.0 - Items are valued at their autosell price.
  // 2.0 - Items are valued at current Mall price, unless they are min-priced.
  // 3.0 - Items are always valued at Mall price (not really realistic).
  //
  // Intermediate values interpolate between integral values.  The default is 1.8,
  // reflecting the fact that items won't sell immediately in the Mall without
  // undercutting or advertising.  This preference, and several previously hidden
  // prefs affecting create vs. buy decisions, are now exposed on a new Creatable
  // -> Fine Tuning page in the Item Manager.
  //
  // "Items already in inventory are considered free."
  // This method is only called for items "already in inventory". It does not check.
  //
  // "Items are valued at current Mall price, unless they are min-priced."
  // And what if they are min-priced? Examination of the code tells me this:
  //
  // 0.0 - Items are considered free.
  // 1.0 - Items are valued at autosell price.
  // 2.0 - Items are valued at autosell price if min-priced in Mall.
  // 2.0 - Items are valued at current Mall price, if not min-priced.
  // 3.0 - Items are always valued at Mall price (not really realistic).
  //
  // "Intermediate values interpolate between integral values"
  //

  private float setValueOfInventory(float valueOfInventory) {
    Preferences.setFloat("valueOfInventory", valueOfInventory);
    return valueOfInventory;
  }

  private int getAutosellPrice(AdventureResult item) {
    return ItemDatabase.getPriceById(item.getItemId());
  }

  private int getMallPrice(AdventureResult item, boolean exact) {
    // *** Note that MallPriceManager must be mocked by caller.
    return exact
        ? MallPriceManager.getMallPrice(item)
        : MallPriceManager.getMallPrice(item, InventoryManager.MALL_PRICE_AGE);
  }

  // *** Simulation of InventoryManager.itemValue
  // "factor" is the "valueOfInventory" property
  // MallPriceManager must be mocked by caller

  private int interpolate(AdventureResult item, boolean exact, float factor) {
    int retval =
        (factor <= 0.0f)
            ? 0
            : (factor <= 1.0f)
                ? interpolate1(item, factor)
                : (factor <= 2.0f)
                    ? interpolate2(item, exact, factor - 1.0f)
                    : interpolate3(item, exact, factor - 2.0f);
    return retval;
  }

  private int interpolate1(AdventureResult item, float factor) {
    // 0.0 < factor <= 1.0
    //   fraction of autosell price
    int autosell = getAutosellPrice(item);
    return (0 + (int) ((autosell - 0) * factor));
  }

  private int interpolate2(AdventureResult item, boolean exact, float factor) {
    // 1.0 < factor <= 2.0:
    //   if mall price <= mall min: autosell price
    //   (can be < if from NPC store)
    //   if mall price > mall min: autosell + fraction between autosell and mall)
    int autosell = getAutosellPrice(item);
    int mallmin = Math.max(100, 2 * autosell);
    int mall = getMallPrice(item, exact);
    return (mall <= mallmin) ? autosell : (autosell + (int) ((mall - autosell) * factor));
  }

  private int interpolate3(AdventureResult item, boolean exact, float factor) {
    // 2.0 < factor
    //   autosell + fraction between autosell and mall)
    int autosell = getAutosellPrice(item);
    int mall = getMallPrice(item, exact);
    return (autosell + (int) ((mall - autosell) * factor));
  }

  @Test
  public void canValueItems() {

    // NPC item; autosell = 35
    AdventureResult GRAPEFRUIT = ItemPool.get(ItemPool.GRAPEFRUIT, 1);
    // Mall minimum; autosell = 35
    AdventureResult BOXED_WINE = ItemPool.get(ItemPool.BOXED_WINE, 1);
    // Mall minimum; autosell = 65
    AdventureResult BUNCH_OF_SQUARE_GRAPES = ItemPool.get(ItemPool.BUNCH_OF_SQUARE_GRAPES, 1);
    // Expensive mall item; autosell = 5
    AdventureResult FISH_HEAD = ItemPool.get(ItemPool.FISH_HEAD, 1);

    // Price map for current ("exact") mall prices
    Map<Integer, Integer> priceMap = new HashMap<>();
    priceMap.put(ItemPool.GRAPEFRUIT, 70);
    priceMap.put(ItemPool.BOXED_WINE, 100);
    priceMap.put(ItemPool.BUNCH_OF_SQUARE_GRAPES, 130);
    priceMap.put(ItemPool.FISH_HEAD, 1000);

    // Price map for old ("not exact") mall prices
    Map<Integer, Integer> oldPriceMap = new HashMap<>();
    oldPriceMap.put(ItemPool.GRAPEFRUIT, 70);
    oldPriceMap.put(ItemPool.BOXED_WINE, 105);
    oldPriceMap.put(ItemPool.BUNCH_OF_SQUARE_GRAPES, 130);
    oldPriceMap.put(ItemPool.FISH_HEAD, 2000);

    Cleanups cleanups = new Cleanups(mockGetMallPrice(priceMap, oldPriceMap));
    try (cleanups) {
      // valueOfInventory = 0; everything is free
      float factor = setValueOfInventory(0.0f);
      boolean exact = false; // use "old" mall prices

      assertEquals(0, InventoryManager.itemValue(GRAPEFRUIT, false));
      assertEquals(0, InventoryManager.itemValue(BOXED_WINE, false));
      assertEquals(0, InventoryManager.itemValue(BUNCH_OF_SQUARE_GRAPES, false));
      assertEquals(0, InventoryManager.itemValue(FISH_HEAD, false));

      // valueOfInventory = 1; everything is at autosell price
      factor = setValueOfInventory(1.0f);

      // Test my simulated calculation
      assertEquals(35, interpolate(GRAPEFRUIT, false, factor));
      assertEquals(35, interpolate(BOXED_WINE, false, factor));
      assertEquals(65, interpolate(BUNCH_OF_SQUARE_GRAPES, false, factor));
      assertEquals(5, interpolate(FISH_HEAD, false, factor));

      assertEquals(35, InventoryManager.itemValue(GRAPEFRUIT, false));
      assertEquals(35, InventoryManager.itemValue(BOXED_WINE, false));
      assertEquals(65, InventoryManager.itemValue(BUNCH_OF_SQUARE_GRAPES, false));
      assertEquals(5, InventoryManager.itemValue(FISH_HEAD, false));

      // valueOfInventory = 1.8 (the default); Like 2.0, but items priced above
      // mall minimum "interpolate" between autosell price and mall price
      // Which is to say: autosell + (int)((mall - autosell) * factor)

      factor = setValueOfInventory(1.8f);

      exact = false; // use "old" mall prices

      // Test my simulated calculation
      assertEquals(35, interpolate(GRAPEFRUIT, exact, factor));
      assertEquals(90, interpolate(BOXED_WINE, exact, factor));
      assertEquals(65, interpolate(BUNCH_OF_SQUARE_GRAPES, exact, factor));
      assertEquals(1600, interpolate(FISH_HEAD, exact, factor));

      assertEquals(35, InventoryManager.itemValue(GRAPEFRUIT, exact));
      assertEquals(90, InventoryManager.itemValue(BOXED_WINE, exact));
      assertEquals(65, InventoryManager.itemValue(BUNCH_OF_SQUARE_GRAPES, exact));
      assertEquals(1600, InventoryManager.itemValue(FISH_HEAD, exact));

      exact = true; // use "current" mall prices

      // Test my simulated calculation
      assertEquals(35, interpolate(GRAPEFRUIT, exact, factor));
      assertEquals(35, interpolate(BOXED_WINE, exact, factor));
      assertEquals(65, interpolate(BUNCH_OF_SQUARE_GRAPES, exact, factor));
      assertEquals(800, interpolate(FISH_HEAD, exact, factor));

      assertEquals(35, InventoryManager.itemValue(GRAPEFRUIT, true));
      assertEquals(35, InventoryManager.itemValue(BOXED_WINE, true));
      assertEquals(65, InventoryManager.itemValue(BUNCH_OF_SQUARE_GRAPES, true));
      assertEquals(800, InventoryManager.itemValue(FISH_HEAD, true));

      // valueOfInventory = 2; everything is at autosell price unless mall price above mall minimum
      factor = setValueOfInventory(2.0f);

      exact = false; // use "old" mall prices

      // Test my simulated calculation
      assertEquals(35, interpolate(GRAPEFRUIT, exact, factor));
      assertEquals(105, interpolate(BOXED_WINE, exact, factor));
      assertEquals(65, interpolate(BUNCH_OF_SQUARE_GRAPES, exact, factor));
      assertEquals(2000, interpolate(FISH_HEAD, exact, factor));

      assertEquals(35, InventoryManager.itemValue(GRAPEFRUIT, exact));
      assertEquals(105, InventoryManager.itemValue(BOXED_WINE, exact));
      assertEquals(65, InventoryManager.itemValue(BUNCH_OF_SQUARE_GRAPES, exact));
      assertEquals(2000, InventoryManager.itemValue(FISH_HEAD, exact));

      exact = true; // use "current" mall prices

      // Test my simulated calculation
      assertEquals(35, interpolate(GRAPEFRUIT, exact, factor));
      assertEquals(35, interpolate(BOXED_WINE, exact, factor));
      assertEquals(65, interpolate(BUNCH_OF_SQUARE_GRAPES, exact, factor));
      assertEquals(1000, interpolate(FISH_HEAD, exact, factor));

      assertEquals(35, InventoryManager.itemValue(GRAPEFRUIT, true));
      assertEquals(35, InventoryManager.itemValue(BOXED_WINE, true));
      assertEquals(65, InventoryManager.itemValue(BUNCH_OF_SQUARE_GRAPES, true));
      assertEquals(1000, InventoryManager.itemValue(FISH_HEAD, true));

      // valueOfInventory = 3; everything is at mall price
      factor = setValueOfInventory(3.0f);

      exact = false; // use "old" mall prices

      // Test my simulated calculation
      assertEquals(70, interpolate(GRAPEFRUIT, exact, factor));
      assertEquals(105, interpolate(BOXED_WINE, exact, factor));
      assertEquals(130, interpolate(BUNCH_OF_SQUARE_GRAPES, exact, factor));
      assertEquals(2000, interpolate(FISH_HEAD, exact, factor));

      assertEquals(70, InventoryManager.itemValue(GRAPEFRUIT, exact));
      assertEquals(105, InventoryManager.itemValue(BOXED_WINE, exact));
      assertEquals(130, InventoryManager.itemValue(BUNCH_OF_SQUARE_GRAPES, exact));
      assertEquals(2000, InventoryManager.itemValue(FISH_HEAD, exact));

      exact = true; // use "current" mall prices

      // Test my simulated calculation
      assertEquals(70, interpolate(GRAPEFRUIT, exact, factor));
      assertEquals(100, interpolate(BOXED_WINE, exact, factor));
      assertEquals(130, interpolate(BUNCH_OF_SQUARE_GRAPES, exact, factor));
      assertEquals(1000, interpolate(FISH_HEAD, exact, factor));

      assertEquals(70, InventoryManager.itemValue(GRAPEFRUIT, exact));
      assertEquals(100, InventoryManager.itemValue(BOXED_WINE, exact));
      assertEquals(130, InventoryManager.itemValue(BUNCH_OF_SQUARE_GRAPES, exact));
      assertEquals(1000, InventoryManager.itemValue(FISH_HEAD, exact));
    }
  }

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
  // MallPriceManager.getMallPrice(item, InventoryManager.MALL_PRICE_AGE) - not "too old" mall price
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
  // MallPriceManager.getMallPrice(item, InventoryManager.MALL_PRICE_AGE)
  // InventoryManager.priceToMake(item, quantity, false)
  // MallPriceManager.getMallPrice(item)
  // InventoryManager.priceToMake(item, quantity, true)
}
