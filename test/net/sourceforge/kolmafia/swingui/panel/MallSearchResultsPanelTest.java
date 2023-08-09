package net.sourceforge.kolmafia.swingui.panel;

import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MallSearchResultsPanelTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("mallSearchResultsPanel");
    MallPurchaseRequest.reset();
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("mallSearchResultsPanel");
  }

  @AfterEach
  public void AfterEach() {
    MallPurchaseRequest.reset();
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
        var filter = panel.getResultsList().filter;
        assertNotNull(filter);

        // Both results are visible and uncolored
        assertTrue(filter.isVisible(request1));
        assertEquals(null, request1.color());
        assertTrue(request1.canPurchase());

        assertTrue(filter.isVisible(request2));
        assertEquals(null, request2.color());
        assertTrue(request2.canPurchase());

        // Disable the first store
        MallPurchaseRequest.addDisabledStore(request1.getShopId());

        // It is still visible but is now gray
        assertTrue(filter.isVisible(request1));
        assertEquals("gray", request1.color());
        assertFalse(request1.canPurchase());
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
        var filter = panel.getResultsList().filter;
        assertNotNull(filter);

        // Result is visible and uncolored
        assertTrue(filter.isVisible(request1));
        assertEquals(null, request1.color());
        assertTrue(request1.canPurchase());

        // Mark the store as ignoring
        MallPurchaseRequest.addIgnoringStore(request1.getShopId());

        // It is not currently visible
        assertFalse(filter.isVisible(request1));
        assertFalse(request1.canPurchase());
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
        var filter = panel.getResultsList().filter;
        assertNotNull(filter);

        // Result is visible and uncolored
        assertTrue(filter.isVisible(request1));
        assertEquals(null, request1.color());
        assertTrue(request1.canPurchase());

        // Mark the store as ignoring
        MallPurchaseRequest.addIgnoringStore(request1.getShopId());

        // It is still visible but is now gray
        assertTrue(filter.isVisible(request1));
        assertEquals("gray", request1.color());
        assertFalse(request1.canPurchase());
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
        var filter = panel.getResultsList().filter;
        assertNotNull(filter);

        // Result is visible and uncolored
        assertTrue(filter.isVisible(request1));
        assertEquals(null, request1.color());
        assertTrue(request1.canPurchase());

        // Mark the store as forbidden
        MallPurchaseRequest.addForbiddenStore(request1.getShopId());

        // It is not currently visible
        assertFalse(filter.isVisible(request1));
        assertFalse(request1.canPurchase());
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
        var filter = panel.getResultsList().filter;
        assertNotNull(filter);

        // Result is visible and uncolored
        assertTrue(filter.isVisible(request1));
        assertEquals(null, request1.color());
        assertTrue(request1.canPurchase());

        // Mark the store as forbidden
        MallPurchaseRequest.addForbiddenStore(request1.getShopId());

        // It is still visible but is now red
        assertTrue(filter.isVisible(request1));
        assertEquals("red", request1.color());
        assertFalse(request1.canPurchase());
      }
    }
  }
}
