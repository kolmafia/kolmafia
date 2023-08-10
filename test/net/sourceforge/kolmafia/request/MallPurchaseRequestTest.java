package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInFreepulls;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MallPurchaseRequestTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("");
    KoLCharacter.reset("mall purchase request");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("mall purchase request");
    MallPurchaseRequest.disabledStores.clear();
    MallPurchaseRequest.ignoringStores.clear();
  }

  private MallPurchaseRequest makeMallPurchaseRequest(AdventureResult item) {
    return new MallPurchaseRequest(item, 1, 1234, "shop", 100, 1, true);
  }

  @Nested
  class CurrentCount {
    @Test
    public void itCountsItemsInInventory() {
      AdventureResult item = ItemPool.get(ItemPool.SEAL_TOOTH, 1);
      var cleanups = new Cleanups(withInteractivity(true), withItem(item));
      try (cleanups) {
        MallPurchaseRequest request = makeMallPurchaseRequest(item);
        assertTrue(request.getCurrentCount() == 1);
      }
    }

    @Test
    public void itCountsItemsInStorage() {
      AdventureResult item = ItemPool.get(ItemPool.SEAL_TOOTH, 1);
      var cleanups = new Cleanups(withInteractivity(false), withItemInStorage(item));
      try (cleanups) {
        MallPurchaseRequest request = makeMallPurchaseRequest(item);
        assertTrue(request.getCurrentCount() == 1);
      }
    }

    @Test
    public void itCountsItemsInFreepulls() {
      AdventureResult item = ItemPool.get(ItemPool.TOILET_PAPER, 1);
      var cleanups = new Cleanups(withInteractivity(false), withItemInFreepulls(item));
      try (cleanups) {
        MallPurchaseRequest request = makeMallPurchaseRequest(item);
        assertTrue(request.getCurrentCount() == 1);
      }
    }
  }

  @Nested
  class Color {
    @Test
    public void disabledStoresAreGray() {
      AdventureResult item = ItemPool.get(ItemPool.SEAL_TOOTH, 1);
      var cleanups = new Cleanups(withInteractivity(true), withMeat(100));
      try (cleanups) {
        MallPurchaseRequest request = makeMallPurchaseRequest(item);
        assertEquals(null, request.color());
        MallPurchaseRequest.addDisabledStore(request.getShopId());
        assertEquals("gray", request.color());
      }
    }

    @Test
    public void ignoringStoresAreGray() {
      AdventureResult item = ItemPool.get(ItemPool.SEAL_TOOTH, 1);
      var cleanups = new Cleanups(withInteractivity(true), withMeat(100));
      try (cleanups) {
        MallPurchaseRequest request = makeMallPurchaseRequest(item);
        assertEquals(null, request.color());
        MallPurchaseRequest.addIgnoringStore(request.getShopId());
        assertEquals("gray", request.color());
      }
    }

    @Test
    public void forbiddenStoresAreRed() {
      AdventureResult item = ItemPool.get(ItemPool.SEAL_TOOTH, 1);
      var cleanups =
          new Cleanups(withInteractivity(true), withMeat(100), withProperty("forbiddenStores"));
      try (cleanups) {
        MallPurchaseRequest request = makeMallPurchaseRequest(item);
        assertEquals(null, request.color());
        MallPurchaseRequest.addForbiddenStore(request.getShopId());
        assertEquals("red", request.color());
      }
    }
  }
}
