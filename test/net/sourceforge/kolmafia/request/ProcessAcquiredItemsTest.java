package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

  private ManageStoreRequest manageStoreRequestSetup(AdventureResult item, String path)
      throws IOException {
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
    String html = Files.readString(Paths.get("request/" + path)).trim();
    request.responseText = html;

    // Voila! we are ready to test
    return request;
  }

  @Test
  public void itMovesFromStoreToInventory() throws IOException {

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
    assertTrue(item.getCount(KoLConstants.inventory) == count);
    assertTrue(item.getCount(KoLConstants.storage) == 0);
    assertTrue(item.getCount(KoLConstants.freepulls) == 0);
  }

  @Test
  public void itMovesFromStoreToStorage() throws IOException {

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
    assertTrue(item.getCount(KoLConstants.inventory) == 0);
    assertTrue(item.getCount(KoLConstants.storage) == count);
    assertTrue(item.getCount(KoLConstants.freepulls) == 0);
  }

  @Test
  public void itMovesFromStoreToFreepulls() throws IOException {

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
    assertTrue(item.getCount(KoLConstants.inventory) == 0);
    assertTrue(item.getCount(KoLConstants.storage) == 0);
    assertTrue(item.getCount(KoLConstants.freepulls) == count);
  }

  // *** Here are tests for buying items from the mall

  private MallPurchaseRequest mallPurchaseRequestSetup(
      AdventureResult item, int shopId, int price, String path) throws IOException {
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
    String html = Files.readString(Paths.get("request/" + path)).trim();
    request.responseText = html;

    // Voila! we are ready to test
    return request;
  }

  @Test
  public void itMovesFromMallToInventory() throws IOException {

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
    assertTrue(item.getCount(KoLConstants.inventory) == count);
    assertTrue(item.getCount(KoLConstants.storage) == 0);
    assertTrue(item.getCount(KoLConstants.freepulls) == 0);
  }

  @Test
  public void itMovesFromMallToStorage() throws IOException {

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
    assertTrue(item.getCount(KoLConstants.inventory) == 0);
    assertTrue(item.getCount(KoLConstants.storage) == count);
    assertTrue(item.getCount(KoLConstants.freepulls) == 0);
  }

  @Test
  public void itMovesFromMallToFreepulls() throws IOException {

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
    assertTrue(item.getCount(KoLConstants.inventory) == 0);
    assertTrue(item.getCount(KoLConstants.storage) == 0);
    assertTrue(item.getCount(KoLConstants.freepulls) == count);
  }
}
