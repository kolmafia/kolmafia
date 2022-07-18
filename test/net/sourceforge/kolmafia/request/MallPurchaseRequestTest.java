package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

public class MallPurchaseRequestTest {
  private MallPurchaseRequest mallPurchaseRequestSetup(AdventureResult item) {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("transfer items user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);
    // Not in error state
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Make a MallPurchaseRequest with specific URLstring and responseText
    MallPurchaseRequest request = new MallPurchaseRequest(item, 1, 1234, "shop", 100, 1, true);

    // The items are not in inventory, storage, or freepulls
    KoLConstants.inventory.clear();
    KoLConstants.storage.clear();
    KoLConstants.freepulls.clear();

    return request;
  }

  @Test
  public void itCountsItemsInInventory() {
    // Testing a non-static method, so make a request.
    AdventureResult item = ItemPool.get(ItemPool.SEAL_TOOTH, 1);
    MallPurchaseRequest request = mallPurchaseRequestSetup(item);

    // Test allowed to interact
    CharPaneRequest.setCanInteract(true);

    // Add an item to inventory
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.SEAL_TOOTH, 1));

    // Verify we find it in inventory
    assertTrue(request.getCurrentCount() == 1);
  }

  @Test
  public void itCountsItemsInStorage() {
    // Testing a non-static method, so make a request.
    AdventureResult item = ItemPool.get(ItemPool.SEAL_TOOTH, 1);
    MallPurchaseRequest request = mallPurchaseRequestSetup(item);

    // Test NOT allowed to interact
    CharPaneRequest.setCanInteract(false);

    // Add an item to storage
    AdventureResult.addResultToList(KoLConstants.storage, item);

    // Verify we find it in inventory
    assertTrue(request.getCurrentCount() == 1);
  }

  @Test
  public void itCountsItemsInFreepulls() {
    // Testing a non-static method, so make a request.
    AdventureResult item = ItemPool.get(ItemPool.TOILET_PAPER, 1);
    MallPurchaseRequest request = mallPurchaseRequestSetup(item);

    // Test NOT allowed to interact
    CharPaneRequest.setCanInteract(false);

    // Add an item to freepulls
    AdventureResult.addResultToList(KoLConstants.freepulls, item);

    // Verify we find it in freepulls
    assertTrue(request.getCurrentCount() == 1);
  }
}
