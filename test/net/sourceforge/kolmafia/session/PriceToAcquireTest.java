package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.mockStatic;

import internal.helpers.Cleanups;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PriceToAcquireTest {

  // Unit tests for the following methods from InventoryManager:
  //
  // int itemValue(AdventureResult item, boolean exact)
  // int priceToAcquire(AdventureResult item, int quantity, int level, boolean exact,
  //                    boolean mallPriceOnly)
  // int priceToMake(AdventureResult item, int quantity, int level, boolean exact,
  //                 boolean mallPriceOnly)
  // boolean cheaperToBuy(AdventureResult item, int quantity)

  private static Cleanups mockGetMallPrice(Map<Integer, Integer> prices) {
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
              int price = prices.get(itemId);
              return price;
            });
    return new Cleanups(mocked::close);
  }

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("mall price manager user");
    MallPriceDatabase.savePricesToFile = false;
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    MallPriceDatabase.savePricesToFile = true;
    Preferences.saveSettingsToFile = true;
  }

  @BeforeEach
  private void beforeEach() {}

  private Map<Integer, Integer> makePriceMap() {
    // Test with this concoction
    //
    // drive-by shooting = piscatini + grapefruit
    // grapfruit = (NPC item)
    // piscatini = boxed wine + fish head
    // fish head = (mall item)
    // boxed wine (3) = fermenting powder	+ bunch of square grapes
    // fermenting powder = (NPC item)
    // bunch of square grapes = (mall item)

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

  @Test
  public void canmockGetMallPrice() {
    Map<Integer, Integer> priceMap = makePriceMap();
    Cleanups cleanups = mockGetMallPrice(priceMap);

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
    }
  }
}
