package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

public class ProcessAcquiredItemsTest {

  // This package tests acquisition of items from either a Mall purchase or from pulling from your
  // store.
  //
  // In both cases, the items can end up in one of three places:
  //
  // inventory - if you are not in Hardcore and are out of Ronin
  // storage - if you are in Hardcore or Ronin
  // freepulls - if you are in Hardcore or Ronin
  //
  // As it happens, KoL formats such items identically, using
  // "relstrings" and KoLmafia uses the same primitive to parse them
  //
  // Since I have saved request URLs & response text for all 3
  // destinations for both use cases, we'll test them all.

  // *** Here are tests for pull items out of your store

  private ManageStoreRequest manageStoreRequestSetup(AdventureResult item, String path) {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("transfer items user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);
    // Not in error state
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Make a ManageStoreRequest with specific URLstring and responseText
    ManageStoreRequest request = new ManageStoreRequest(item.getItemId(), item.getCount());

    // The items are not in inventory, storage, or freepulls
    KoLConstants.inventory.clear();
    KoLConstants.storage.clear();
    KoLConstants.freepulls.clear();

    // Load the responseText from saved HTML file
    request.responseText = html("request/" + path);

    // Voila! we are ready to test
    return request;
  }

  @Test
  public void itMovesFromStoreToInventory() {

    // Load up our request/response
    AdventureResult item = ItemPool.get(ItemPool.REAGENT, 1);
    ManageStoreRequest request = manageStoreRequestSetup(item, "test_store_not_ronin_remove.html");
    String urlString = request.getURLString();
    String responseText = request.responseText;

    // Test NOT being in Ronin
    KoLCharacter.setRonin(false);

    // Parse response and move items into inventory
    ManageStoreRequest.parseResponse(urlString, responseText);

    // Test whether item is in inventory, storage, or freepulls
    int count = item.getCount();
    assertEquals(item.getCount(KoLConstants.inventory), count);
    assertEquals(0, item.getCount(KoLConstants.storage));
    assertEquals(0, item.getCount(KoLConstants.freepulls));
  }

  @Test
  public void itMovesFromStoreToStorage() {

    // Load up our request/response
    AdventureResult item = ItemPool.get(ItemPool.SUMP_M_SUMP_M, 2);
    ManageStoreRequest request =
        manageStoreRequestSetup(item, "test_store_ronin_remove_storage.html");
    String urlString = request.getURLString();
    String responseText = request.responseText;

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Parse response and move items into inventory
    ManageStoreRequest.parseResponse(urlString, responseText);

    // Test whether item is in inventory, storage, or freepulls
    int count = item.getCount();
    assertEquals(0, item.getCount(KoLConstants.inventory));
    assertEquals(item.getCount(KoLConstants.storage), count);
    assertEquals(0, item.getCount(KoLConstants.freepulls));
  }

  @Test
  public void itMovesFromStoreToFreepulls() {

    // Load up our request/response
    AdventureResult item = ItemPool.get(ItemPool.TOILET_PAPER, 2);
    ManageStoreRequest request =
        manageStoreRequestSetup(item, "test_store_ronin_remove_freepulls.html");
    String urlString = request.getURLString();
    String responseText = request.responseText;

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Parse response and move items into inventory
    ManageStoreRequest.parseResponse(urlString, responseText);

    // Test whether item is in inventory, storage, or freepulls
    int count = item.getCount();
    assertEquals(0, item.getCount(KoLConstants.inventory));
    assertEquals(0, item.getCount(KoLConstants.storage));
    assertEquals(item.getCount(KoLConstants.freepulls), count);
  }

  // *** Here are tests for buying items from the mall

  private MallPurchaseRequest mallPurchaseRequestSetup(
      AdventureResult item, int shopId, int price, String path) {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("transfer items user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);
    // Not in error state
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Make a MallPurchaseRequest with specific URLstring and responseText
    int quantity = item.getCount();
    MallPurchaseRequest request =
        new MallPurchaseRequest(item, quantity, shopId, "shop", price, quantity, true);

    // The items are not in inventory, storage, or freepulls
    KoLConstants.inventory.clear();
    KoLConstants.storage.clear();
    KoLConstants.freepulls.clear();

    // Load the responseText from saved HTML file
    request.responseText = html("request/" + path);

    // Voila! we are ready to test
    return request;
  }

  @Test
  public void itMovesFromMallToInventory() {

    // Load up our request/response
    AdventureResult item = ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 1);
    MallPurchaseRequest request =
        mallPurchaseRequestSetup(item, 79795, 107, "test_mall_buy_not_ronin.html");
    String urlString = request.getURLString();
    String responseText = request.responseText;

    // Test NOT being in Ronin
    KoLCharacter.setRonin(false);

    // Parse response and move items into inventory
    MallPurchaseRequest.parseResponse(urlString, responseText);

    // Test whether item is in inventory, storage, or freepulls
    int count = item.getCount();
    assertEquals(item.getCount(KoLConstants.inventory), count);
    assertEquals(0, item.getCount(KoLConstants.storage));
    assertEquals(0, item.getCount(KoLConstants.freepulls));
  }

  @Test
  public void itMovesFromMallToStorage() {

    // Load up our request/response
    AdventureResult item = ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 1);
    MallPurchaseRequest request =
        mallPurchaseRequestSetup(item, 845708, 101, "test_mall_buy_ronin_storage.html");
    String urlString = request.getURLString();
    String responseText = request.responseText;

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Parse response and move items into inventory
    MallPurchaseRequest.parseResponse(urlString, responseText);

    // Test whether item is in inventory, storage, or freepulls
    int count = item.getCount();
    assertEquals(0, item.getCount(KoLConstants.inventory));
    assertEquals(item.getCount(KoLConstants.storage), count);
    assertEquals(0, item.getCount(KoLConstants.freepulls));
  }

  @Test
  public void itMovesFromMallToFreepulls() {

    // Load up our request/response
    AdventureResult item = ItemPool.get(ItemPool.TOILET_PAPER, 1);
    MallPurchaseRequest request =
        mallPurchaseRequestSetup(item, 845708, 101, "test_mall_buy_ronin_freepulls.html");
    String urlString = request.getURLString();
    String responseText = request.responseText;

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Parse response and move items into inventory
    MallPurchaseRequest.parseResponse(urlString, responseText);

    // Test whether item is in inventory, storage, or freepulls
    int count = item.getCount();
    assertEquals(0, item.getCount(KoLConstants.inventory));
    assertEquals(0, item.getCount(KoLConstants.storage));
    assertEquals(item.getCount(KoLConstants.freepulls), count);
  }
}
