package net.sourceforge.kolmafia.swingui.panel;

import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MallSearchResultsPanelTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("mallSearchResults");
  }

  @BeforeEach
  public void beforeEach() {
    NamedListenerRegistry.reset();
    PreferenceListenerRegistry.reset();
    Preferences.reset("mallSearchResults");
    MallPurchaseRequest.disabledStores.clear();
    MallPurchaseRequest.ignoringStores.clear();
  }

  private MallPurchaseRequest makeMallPurchaseRequest(AdventureResult item, int shopId, int price) {
    return new MallPurchaseRequest(item, item.getCount(), shopId, "shop" + shopId, price, 1, true);
  }

  @Nested
  class DisabledStore {
    @Test
    public void disabledStoresAreVisibleButGray() {
      var cleanups = new Cleanups(withInteractivity(true), withMeat(1000));
      try (cleanups) {
        LockableListModel<PurchaseRequest> results = new LockableListModel<>();
        var request1 = makeMallPurchaseRequest(ItemPool.get(ItemPool.SEAL_TOOTH, 5), 123, 100);
        results.add(request1);
        var request2 = makeMallPurchaseRequest(ItemPool.get(ItemPool.SEAL_TOOTH, 5), 456, 200);
        results.add(request2);

        var panel = new MallSearchResultsPanel(results);

        // Both results are visible and uncolored
        assertEquals(0, results.getIndexOf(request1));
        assertEquals(null, request1.color());

        assertEquals(1, results.getIndexOf(request2));
        assertEquals(null, request2.color());

        // Disable the first store
        MallPurchaseRequest.addDisabledStore(request1.getShopId());

        // It is still visible but is now gray
        assertEquals(0, results.getIndexOf(request1));
        assertEquals("gray", request1.color());
      }
    }
  }

  @Nested
  class IgnoringStore {
    @Test
    public void ignoringStoresAreNotVisible() {
      var cleanups =
          new Cleanups(
              withInteractivity(true),
              withMeat(1000),
              withProperty("showIgnoringStorePrices", false));
      try (cleanups) {
        LockableListModel<PurchaseRequest> results = new LockableListModel<>();
        var request1 = makeMallPurchaseRequest(ItemPool.get(ItemPool.SEAL_TOOTH, 5), 123, 100);
        results.add(request1);

        var panel = new MallSearchResultsPanel(results);

        // Result is visible and uncolored
        assertEquals(0, results.getIndexOf(request1));
        assertEquals(null, request1.color());

        // Mark the store as ignoring
        MallPurchaseRequest.addIgnoringStore(request1.getShopId());

        // It is not currently visible
        // *** How to test this?
        // assertEquals(-1, results.getIndexOf(request1));
        assertEquals("gray", request1.color());
      }
    }

    @Test
    public void ignoringStoresCanBeVisible() {
      var cleanups =
          new Cleanups(
              withInteractivity(true),
              withMeat(1000),
              withProperty("showIgnoringStorePrices", true));
      try (cleanups) {
        LockableListModel<PurchaseRequest> results = new LockableListModel<>();
        var request1 = makeMallPurchaseRequest(ItemPool.get(ItemPool.SEAL_TOOTH, 5), 123, 100);
        results.add(request1);

        var panel = new MallSearchResultsPanel(results);

        // Result is visible and uncolored
        assertEquals(0, results.getIndexOf(request1));
        assertEquals(null, request1.color());

        // Mark the store as ignoring
        MallPurchaseRequest.addIgnoringStore(request1.getShopId());

        // It is still visible but is now gray
        assertEquals(0, results.getIndexOf(request1));
        assertEquals("gray", request1.color());
      }
    }
  }

  @Nested
  class ForbiddenStore {
    @Test
    public void forbiddenStoresAreNotVisible() {
      var cleanups =
          new Cleanups(
              withInteractivity(true),
              withMeat(1000),
              withProperty("forbiddenStores"),
              withProperty("showForbiddenStores", false));
      try (cleanups) {
        LockableListModel<PurchaseRequest> results = new LockableListModel<>();
        var request1 = makeMallPurchaseRequest(ItemPool.get(ItemPool.SEAL_TOOTH, 5), 123, 100);
        results.add(request1);

        var panel = new MallSearchResultsPanel(results);

        // Result is visible and uncolored
        assertEquals(0, results.getIndexOf(request1));
        assertEquals(null, request1.color());

        // Mark the store as forbidden
        MallPurchaseRequest.addForbiddenStore(request1.getShopId());

        // It is not currently visible
        // *** How to test this?
        // assertEquals(-1, results.getIndexOf(request1));
        assertEquals("red", request1.color());
      }
    }

    @Test
    public void forbiddenStoresCanBeVisible() {
      var cleanups =
          new Cleanups(
              withInteractivity(true),
              withMeat(1000),
              withProperty("forbiddenStores"),
              withProperty("showForbiddenStores", true));
      try (cleanups) {
        LockableListModel<PurchaseRequest> results = new LockableListModel<>();
        var request1 = makeMallPurchaseRequest(ItemPool.get(ItemPool.SEAL_TOOTH, 5), 123, 100);
        results.add(request1);

        var panel = new MallSearchResultsPanel(results);

        // Result is visible and uncolored
        assertEquals(0, results.getIndexOf(request1));
        assertEquals(null, request1.color());

        // Mark the store as forbidden
        MallPurchaseRequest.addForbiddenStore(request1.getShopId());

        // It is still visible but is now gray
        assertEquals(0, results.getIndexOf(request1));
        assertEquals("red", request1.color());
      }
    }
  }
}
