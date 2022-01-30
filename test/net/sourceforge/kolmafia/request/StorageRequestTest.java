package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Test;

public class StorageRequestTest extends RequestTestBase {

  private Set<Integer> pulledItemSet = new HashSet<>();
  private String pulledItemProperty = "";

  // *** Here are tests for the primitives that handle ronin item pulls.

  // We don't use @BeforeEach here because it's specific to ronin storage primitives related tests.
  private void roninStoragePrimitivesSetup() {
    // Simulate logging out and back in again.
    pulledItemSet.clear();
    pulledItemProperty = "";
  }

  @Test
  void itShouldParsePropertyCorrectly() {
    roninStoragePrimitivesSetup();

    // Normal case
    pulledItemProperty = "57,60";
    StorageRequest.pullsStringToSet(pulledItemProperty, pulledItemSet);
    assertTrue(pulledItemSet.size() == 2);
    assertTrue(pulledItemSet.contains(57));
    assertTrue(pulledItemSet.contains(60));

    // Duplicate elements with padding
    pulledItemProperty = "57 , 60 , 57, 60";
    StorageRequest.pullsStringToSet(pulledItemProperty, pulledItemSet);
    assertTrue(pulledItemSet.size() == 2);
    assertTrue(pulledItemSet.contains(57));
    assertTrue(pulledItemSet.contains(60));

    // Bogus elements
    pulledItemProperty = "57 , bogus 1 , bogus 2";
    StorageRequest.pullsStringToSet(pulledItemProperty, pulledItemSet);
    assertTrue(pulledItemSet.size() == 1);
    assertTrue(pulledItemSet.contains(57));
  }

  @Test
  void itShouldGeneratePropertyCorrectly() {
    roninStoragePrimitivesSetup();

    // Normal case
    pulledItemSet.add(57);
    pulledItemSet.add(60);
    String result = StorageRequest.pullsSetToString(pulledItemSet);
    // Sets are unordered
    List<String> list = Arrays.asList(result.split(","));
    assertTrue(list.size() == 2);
    assertTrue(list.contains("57"));
    assertTrue(list.contains("60"));
  }

  @Test
  void itShouldAddItemsCorrectly() {
    roninStoragePrimitivesSetup();

    // Add an item. It must be present;
    StorageRequest.addPulledItem(pulledItemSet, 57);
    assertTrue(pulledItemSet.size() == 1);
    assertTrue(pulledItemSet.contains(57));

    // Add another item. It also must be present;
    StorageRequest.addPulledItem(pulledItemSet, 60);
    assertTrue(pulledItemSet.size() == 2);
    assertTrue(pulledItemSet.contains(57));
    assertTrue(pulledItemSet.contains(60));

    // Add duplicate item. It also must be present
    StorageRequest.addPulledItem(pulledItemSet, 57);
    assertTrue(pulledItemSet.size() == 2);
    assertTrue(pulledItemSet.contains(57));
    assertTrue(pulledItemSet.contains(60));
  }

  @Test
  void itShouldCheckItemsCorrectly() {
    roninStoragePrimitivesSetup();

    // Add several items
    StorageRequest.addPulledItem(pulledItemSet, 57);
    StorageRequest.addPulledItem(pulledItemSet, 60);

    // Verify that they both are present and that another item is not present
    assertTrue(StorageRequest.itemPulledInRonin(pulledItemSet, 57));
    assertTrue(StorageRequest.itemPulledInRonin(pulledItemSet, 60));
    assertFalse(StorageRequest.itemPulledInRonin(pulledItemSet, 100));
  }

  // *** Here are tests for the actual methods that deal with the ronin item pulls property

  // We don't use @BeforeEach here because it's specific to ronin storage property related tests.
  private void roninStoragePropertySetup() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("ronin user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);
    // This shouldn't be necessary if reset does what is expected but....
    Preferences.setString("_roninStoragePulls", "");
    // Say that the character is in Ronin.
    KoLCharacter.setRonin(true);
  }

  @Test
  void itShouldLoadPropertyInRonin() {
    roninStoragePropertySetup();
    assertTrue(StorageRequest.roninStoragePulls.size() == 0);
    Preferences.setString("_roninStoragePulls", "57,60");
    StorageRequest.loadRoninStoragePulls();
    assertTrue(StorageRequest.roninStoragePulls.size() == 2);
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldAddPulledItemInRonin() {
    roninStoragePropertySetup();
    String property = Preferences.getString("_roninStoragePulls");
    assertTrue(property.equals(""));
    StorageRequest.addPulledItem(ItemPool.get(57));
    StorageRequest.addPulledItem(60);
    property = Preferences.getString("_roninStoragePulls");
    assertFalse(property.equals(""));
    assertTrue(property.contains("57"));
    assertTrue(property.contains("60"));
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldNotAddPulledItemOutOfRonin() {
    roninStoragePropertySetup();
    KoLCharacter.setRonin(false);
    String property = Preferences.getString("_roninStoragePulls");
    assertTrue(property.equals(""));
    StorageRequest.addPulledItem(ItemPool.get(57));
    StorageRequest.addPulledItem(60);
    property = Preferences.getString("_roninStoragePulls");
    assertTrue(property.equals(""));
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldFindPulledItemsInRonin() {
    roninStoragePropertySetup();
    StorageRequest.addPulledItem(ItemPool.get(57));
    StorageRequest.addPulledItem(60);
    assertTrue(StorageRequest.itemPulledInRonin(ItemPool.get(57)));
    assertTrue(StorageRequest.itemPulledInRonin(60));
    assertFalse(StorageRequest.itemPulledInRonin(100));
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldNotFindPulledItemsOutOfRonin() {
    roninStoragePropertySetup();
    KoLCharacter.setRonin(false);
    StorageRequest.addPulledItem(ItemPool.get(57));
    StorageRequest.addPulledItem(60);
    assertFalse(StorageRequest.itemPulledInRonin(ItemPool.get(57)));
    assertFalse(StorageRequest.itemPulledInRonin(60));
    assertFalse(StorageRequest.itemPulledInRonin(100));
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldSaveRoninStoragePulls() {
    roninStoragePropertySetup();
    String property = Preferences.getString("_roninStoragePulls");
    assertTrue(property.equals(""));
    assertTrue(StorageRequest.roninStoragePulls.size() == 0);
    StorageRequest.roninStoragePulls.add(57);
    StorageRequest.roninStoragePulls.add(60);
    StorageRequest.saveRoninStoragePulls();
    property = Preferences.getString("_roninStoragePulls");
    assertFalse(property.equals(""));
    assertTrue(property.contains("57"));
    assertTrue(property.contains("60"));
    Preferences.setString("_roninStoragePulls", "");
  }

  // *** Here are tests for how StorageRequest generates subminstances.
  //
  // Which is to say, how it splits up a StorageRequest with potentially
  // many items into multiple StorageRequests with at most 11 items each.
  //
  // There is also special handling for pull items in Ronin:
  // - As many of a free-pull item as you want, but one per request
  // - Only one of non-free-pull items, and only once per day
  // We don't use @BeforeEach here because it's specific to ronin storage property related tests.

  private void addToStorage(int itemId, int count) {
    AdventureResult.addResultToList(KoLConstants.storage, ItemPool.get(itemId, count));
  }

  private void addToFreePulls(int itemId, int count) {
    AdventureResult.addResultToList(KoLConstants.freepulls, ItemPool.get(itemId, count));
  }

  private void storageSubInstanceSetup() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("subinstance user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);

    // Items which are not free pulls are in the storage list, whether or not
    // you are in Ronin

    addToStorage(ItemPool.MILK_OF_MAGNESIUM, 5);
    addToStorage(ItemPool.FLAMING_MUSHROOM, 5);
    addToStorage(ItemPool.FROZEN_MUSHROOM, 5);
    addToStorage(ItemPool.STINKY_MUSHROOM, 5);
    addToStorage(ItemPool.CLOCKWORK_BARTENDER, 5);
    addToStorage(ItemPool.CLOCKWORK_CHEF, 5);
    addToStorage(ItemPool.CLOCKWORK_MAID, 5);
    addToStorage(ItemPool.HOT_WAD, 5);
    addToStorage(ItemPool.COLD_WAD, 5);
    addToStorage(ItemPool.SPOOKY_WAD, 5);
    addToStorage(ItemPool.STENCH_WAD, 5);
    addToStorage(ItemPool.SLEAZE_WAD, 5);
    addToStorage(ItemPool.HOT_HI_MEIN, 5);
    addToStorage(ItemPool.COLD_HI_MEIN, 5);
    addToStorage(ItemPool.SPOOKY_HI_MEIN, 5);
    addToStorage(ItemPool.STINKY_HI_MEIN, 5);
    addToStorage(ItemPool.SLEAZY_HI_MEIN, 5);

    // Items which are free pulls are in the freepulls list during Ronin and
    // the storage list out of Ronin.

    addToFreePulls(ItemPool.BRICK, 100);
    addToFreePulls(ItemPool.TOILET_PAPER, 100);
  }

  @Test
  public void itShouldGenerateOneSubInstanceNotInRonin() {
    storageSubInstanceSetup();

    List<AdventureResult> items = new ArrayList<>();

    // Make a list of 8 items to pull.

    // 5 items are in storage.
    // 3 items are not in storage.
    // 3 in-storage items are fully available.
    items.add(ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 4));
    items.add(ItemPool.get(ItemPool.FLAMING_MUSHROOM, 2));
    items.add(ItemPool.get(ItemPool.SEAL_TOOTH, 1)); // Not in storage
    items.add(ItemPool.get(ItemPool.FRILLY_SKIRT, 1)); // Not in storage
    items.add(ItemPool.get(ItemPool.CLOCKWORK_CHEF, 1));
    // 2 in-storage items have less available than desired.
    items.add(ItemPool.get(ItemPool.HOT_WAD, 20));
    items.add(ItemPool.get(ItemPool.WINDCHIMES, 1)); // Not in storage
    items.add(ItemPool.get(ItemPool.COLD_WAD, 20));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Test NOT being in Ronin
    KoLCharacter.setRonin(false);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertTrue(subinstances.size() == 1);

    TransferItemRequest rq = subinstances.get(0);

    // We expect there to be exactly 5 attachments
    assertTrue(rq.attachments != null);
    assertTrue(rq.attachments.length == 5);

    // We expect them to be exactly the items that are in storage
    // We expect them to be limited by min(requested, available)
    assertTrue(rq.attachments[0].getItemId() == ItemPool.MILK_OF_MAGNESIUM);
    assertTrue(rq.attachments[0].getCount() == 4);
    assertTrue(rq.attachments[1].getItemId() == ItemPool.FLAMING_MUSHROOM);
    assertTrue(rq.attachments[1].getCount() == 2);
    assertTrue(rq.attachments[2].getItemId() == ItemPool.CLOCKWORK_CHEF);
    assertTrue(rq.attachments[2].getCount() == 1);
    assertTrue(rq.attachments[3].getItemId() == ItemPool.HOT_WAD);
    assertTrue(rq.attachments[3].getCount() == 5);
    assertTrue(rq.attachments[4].getItemId() == ItemPool.COLD_WAD);
    assertTrue(rq.attachments[4].getCount() == 5);
  }

  @Test
  public void itShouldGenerateTwoSubInstances() {
    storageSubInstanceSetup();

    List<AdventureResult> items = new ArrayList<>();

    // Make a list of 17 items to pull.

    items.add(ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 1));
    items.add(ItemPool.get(ItemPool.FLAMING_MUSHROOM, 1));
    items.add(ItemPool.get(ItemPool.FROZEN_MUSHROOM, 1));
    items.add(ItemPool.get(ItemPool.STINKY_MUSHROOM, 1));
    items.add(ItemPool.get(ItemPool.CLOCKWORK_BARTENDER, 1));
    items.add(ItemPool.get(ItemPool.CLOCKWORK_CHEF, 1));
    items.add(ItemPool.get(ItemPool.CLOCKWORK_MAID, 1));
    items.add(ItemPool.get(ItemPool.HOT_WAD, 1));
    items.add(ItemPool.get(ItemPool.COLD_WAD, 1));
    items.add(ItemPool.get(ItemPool.SPOOKY_WAD, 1));
    items.add(ItemPool.get(ItemPool.STENCH_WAD, 1));
    items.add(ItemPool.get(ItemPool.SLEAZE_WAD, 1));
    items.add(ItemPool.get(ItemPool.HOT_HI_MEIN, 1));
    items.add(ItemPool.get(ItemPool.COLD_HI_MEIN, 1));
    items.add(ItemPool.get(ItemPool.SPOOKY_HI_MEIN, 1));
    items.add(ItemPool.get(ItemPool.STINKY_HI_MEIN, 1));
    items.add(ItemPool.get(ItemPool.SLEAZY_HI_MEIN, 1));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Test NOT being in Ronin
    KoLCharacter.setRonin(false);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be two subinstances
    assertTrue(subinstances.size() == 2);

    TransferItemRequest rq1 = subinstances.get(0);

    // We expect there to be exactly 11 attachments in the first request
    assertTrue(rq1.attachments != null);
    assertTrue(rq1.attachments.length == 11);

    // We expect them to be exactly the items that are in storage
    assertTrue(rq1.attachments[0].getItemId() == ItemPool.MILK_OF_MAGNESIUM);
    assertTrue(rq1.attachments[0].getCount() == 1);
    assertTrue(rq1.attachments[1].getItemId() == ItemPool.FLAMING_MUSHROOM);
    assertTrue(rq1.attachments[1].getCount() == 1);
    assertTrue(rq1.attachments[2].getItemId() == ItemPool.FROZEN_MUSHROOM);
    assertTrue(rq1.attachments[2].getCount() == 1);
    assertTrue(rq1.attachments[3].getItemId() == ItemPool.STINKY_MUSHROOM);
    assertTrue(rq1.attachments[3].getCount() == 1);
    assertTrue(rq1.attachments[4].getItemId() == ItemPool.CLOCKWORK_BARTENDER);
    assertTrue(rq1.attachments[4].getCount() == 1);
    assertTrue(rq1.attachments[5].getItemId() == ItemPool.CLOCKWORK_CHEF);
    assertTrue(rq1.attachments[5].getCount() == 1);
    assertTrue(rq1.attachments[6].getItemId() == ItemPool.CLOCKWORK_MAID);
    assertTrue(rq1.attachments[6].getCount() == 1);
    assertTrue(rq1.attachments[7].getItemId() == ItemPool.HOT_WAD);
    assertTrue(rq1.attachments[7].getCount() == 1);
    assertTrue(rq1.attachments[8].getItemId() == ItemPool.COLD_WAD);
    assertTrue(rq1.attachments[8].getCount() == 1);
    assertTrue(rq1.attachments[9].getItemId() == ItemPool.SPOOKY_WAD);
    assertTrue(rq1.attachments[9].getCount() == 1);
    assertTrue(rq1.attachments[10].getItemId() == ItemPool.STENCH_WAD);
    assertTrue(rq1.attachments[10].getCount() == 1);

    TransferItemRequest rq2 = subinstances.get(1);

    // We expect there to be exactly 6 attachments in the second request
    assertTrue(rq2.attachments != null);
    assertTrue(rq2.attachments.length == 6);

    // We expect them to be exactly the items that are in storage
    assertTrue(rq2.attachments[0].getItemId() == ItemPool.SLEAZE_WAD);
    assertTrue(rq2.attachments[0].getCount() == 1);
    assertTrue(rq2.attachments[1].getItemId() == ItemPool.HOT_HI_MEIN);
    assertTrue(rq2.attachments[1].getCount() == 1);
    assertTrue(rq2.attachments[2].getItemId() == ItemPool.COLD_HI_MEIN);
    assertTrue(rq2.attachments[2].getCount() == 1);
    assertTrue(rq2.attachments[3].getItemId() == ItemPool.SPOOKY_HI_MEIN);
    assertTrue(rq2.attachments[3].getCount() == 1);
    assertTrue(rq2.attachments[4].getItemId() == ItemPool.STINKY_HI_MEIN);
    assertTrue(rq2.attachments[4].getCount() == 1);
    assertTrue(rq2.attachments[5].getItemId() == ItemPool.SLEAZY_HI_MEIN);
    assertTrue(rq2.attachments[5].getCount() == 1);
  }

  @Test
  public void itShouldGenerateOneSubInstanceInRonin() {
    storageSubInstanceSetup();

    List<AdventureResult> items = new ArrayList<>();

    // Make a list of 8 items to pull.

    // 5 items are in storage.
    // 3 items are not in storage.
    // 3 in-storage items are fully available.
    items.add(ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 4));
    items.add(ItemPool.get(ItemPool.FLAMING_MUSHROOM, 2));
    items.add(ItemPool.get(ItemPool.SEAL_TOOTH, 1)); // Not in storage
    items.add(ItemPool.get(ItemPool.FRILLY_SKIRT, 1)); // Not in storage
    items.add(ItemPool.get(ItemPool.CLOCKWORK_CHEF, 1));
    // 2 in-storage items have less available than desired.
    items.add(ItemPool.get(ItemPool.HOT_WAD, 20));
    items.add(ItemPool.get(ItemPool.WINDCHIMES, 1)); // Not in storage
    items.add(ItemPool.get(ItemPool.COLD_WAD, 20));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertTrue(subinstances.size() == 1);

    TransferItemRequest rq = subinstances.get(0);

    // We expect there to be exactly 5 attachments
    assertTrue(rq.attachments != null);
    assertTrue(rq.attachments.length == 5);

    // We expect them to be exactly the items that are in storage
    // We expect them to be limited to 1
    assertTrue(rq.attachments[0].getItemId() == ItemPool.MILK_OF_MAGNESIUM);
    assertTrue(rq.attachments[0].getCount() == 1);
    assertTrue(rq.attachments[1].getItemId() == ItemPool.FLAMING_MUSHROOM);
    assertTrue(rq.attachments[1].getCount() == 1);
    assertTrue(rq.attachments[2].getItemId() == ItemPool.CLOCKWORK_CHEF);
    assertTrue(rq.attachments[2].getCount() == 1);
    assertTrue(rq.attachments[3].getItemId() == ItemPool.HOT_WAD);
    assertTrue(rq.attachments[3].getCount() == 1);
    assertTrue(rq.attachments[4].getItemId() == ItemPool.COLD_WAD);
    assertTrue(rq.attachments[4].getCount() == 1);
  }

  @Test
  public void itShouldMixInFreepullsNotInRonin() {
    storageSubInstanceSetup();

    List<AdventureResult> items = new ArrayList<>();

    // Make a list of 5 items to pull.

    // 3 items are not free pulls
    // 2 items are free pulls
    items.add(ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 4));
    items.add(ItemPool.get(ItemPool.BRICK, 3));
    items.add(ItemPool.get(ItemPool.FLAMING_MUSHROOM, 2));
    items.add(ItemPool.get(ItemPool.TOILET_PAPER, 3));
    items.add(ItemPool.get(ItemPool.CLOCKWORK_CHEF, 1));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Test NOT being in Ronin
    KoLCharacter.setRonin(false);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertTrue(subinstances.size() == 1);

    TransferItemRequest rq = subinstances.get(0);

    // We expect there to be exactly 5 attachments
    assertTrue(rq.attachments != null);
    assertTrue(rq.attachments.length == 5);

    // We expect them to be exactly the items that are in storage
    // We expect them to be limited by min(requested, available)
    assertTrue(rq.attachments[0].getItemId() == ItemPool.MILK_OF_MAGNESIUM);
    assertTrue(rq.attachments[0].getCount() == 4);
    assertTrue(rq.attachments[1].getItemId() == ItemPool.BRICK);
    assertTrue(rq.attachments[1].getCount() == 3);
    assertTrue(rq.attachments[2].getItemId() == ItemPool.FLAMING_MUSHROOM);
    assertTrue(rq.attachments[2].getCount() == 2);
    assertTrue(rq.attachments[3].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(rq.attachments[3].getCount() == 3);
    assertTrue(rq.attachments[4].getItemId() == ItemPool.CLOCKWORK_CHEF);
    assertTrue(rq.attachments[4].getCount() == 1);
  }

  @Test
  public void itShouldSeparateFreepullsInRonin() {
    storageSubInstanceSetup();

    List<AdventureResult> items = new ArrayList<>();

    // Make a list of 5 items to pull.

    // 3 items are not free pulls
    // 2 items are free pulls
    items.add(ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 4));
    items.add(ItemPool.get(ItemPool.BRICK, 3));
    items.add(ItemPool.get(ItemPool.FLAMING_MUSHROOM, 2));
    items.add(ItemPool.get(ItemPool.TOILET_PAPER, 3));
    items.add(ItemPool.get(ItemPool.CLOCKWORK_CHEF, 1));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be 9 subinstances
    assertTrue(subinstances.size() == 9);

    // SubInstance #1
    TransferItemRequest rq1 = subinstances.get(0);

    // We expect there to be exactly 1 attachment
    assertTrue(rq1.attachments != null);
    assertTrue(rq1.attachments.length == 1);
    assertTrue(rq1.attachments[0].getItemId() == ItemPool.MILK_OF_MAGNESIUM);
    assertTrue(rq1.attachments[0].getCount() == 1);

    // SubInstance #2
    TransferItemRequest rq2 = subinstances.get(1);

    // We expect there to be exactly 1 attachment
    assertTrue(rq2.attachments != null);
    assertTrue(rq2.attachments.length == 1);
    assertTrue(rq2.attachments[0].getItemId() == ItemPool.BRICK);
    assertTrue(rq2.attachments[0].getCount() == 1);

    // SubInstance #3
    TransferItemRequest rq3 = subinstances.get(2);

    // We expect there to be exactly 1 attachment
    assertTrue(rq3.attachments != null);
    assertTrue(rq3.attachments.length == 1);
    assertTrue(rq3.attachments[0].getItemId() == ItemPool.BRICK);
    assertTrue(rq3.attachments[0].getCount() == 1);

    // SubInstance #4
    TransferItemRequest rq4 = subinstances.get(3);

    // We expect there to be exactly 1 attachment
    assertTrue(rq4.attachments != null);
    assertTrue(rq4.attachments.length == 1);
    assertTrue(rq4.attachments[0].getItemId() == ItemPool.BRICK);
    assertTrue(rq4.attachments[0].getCount() == 1);

    // SubInstance #5
    TransferItemRequest rq5 = subinstances.get(4);

    // We expect there to be exactly 1 attachment
    assertTrue(rq5.attachments != null);
    assertTrue(rq5.attachments.length == 1);
    assertTrue(rq5.attachments[0].getItemId() == ItemPool.FLAMING_MUSHROOM);
    assertTrue(rq5.attachments[0].getCount() == 1);

    // SubInstance #6
    TransferItemRequest rq6 = subinstances.get(5);

    // We expect there to be exactly 1 attachment
    assertTrue(rq6.attachments != null);
    assertTrue(rq6.attachments.length == 1);
    assertTrue(rq6.attachments[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(rq6.attachments[0].getCount() == 1);

    // SubInstance #7
    TransferItemRequest rq7 = subinstances.get(6);

    // We expect there to be exactly 1 attachment
    assertTrue(rq7.attachments != null);
    assertTrue(rq7.attachments.length == 1);
    assertTrue(rq7.attachments[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(rq7.attachments[0].getCount() == 1);

    // SubInstance #8
    TransferItemRequest rq8 = subinstances.get(7);

    // We expect there to be exactly 1 attachment
    assertTrue(rq8.attachments != null);
    assertTrue(rq8.attachments.length == 1);
    assertTrue(rq8.attachments[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(rq8.attachments[0].getCount() == 1);

    // SubInstance #9
    TransferItemRequest rq9 = subinstances.get(8);

    // We expect there to be exactly 1 attachment
    assertTrue(rq9.attachments != null);
    assertTrue(rq9.attachments.length == 1);
    assertTrue(rq9.attachments[0].getItemId() == ItemPool.CLOCKWORK_CHEF);
    assertTrue(rq9.attachments[0].getCount() == 1);
  }

  @Test
  public void itShouldSkipAlreadyPulledItemsInRonin() {
    storageSubInstanceSetup();

    List<AdventureResult> items = new ArrayList<>();

    // Make a list of 5 items to pull.

    // 3 items have not been pulled yet today
    // 2 items have been pulled today

    items.add(ItemPool.get(ItemPool.MILK_OF_MAGNESIUM, 2));
    items.add(ItemPool.get(ItemPool.FLAMING_MUSHROOM, 2));
    items.add(ItemPool.get(ItemPool.CLOCKWORK_CHEF, 1));
    items.add(ItemPool.get(ItemPool.HOT_WAD, 10));
    items.add(ItemPool.get(ItemPool.COLD_WAD, 10));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Tell KoLmafia that 2 of the items have been pulled today
    StorageRequest.addPulledItem(ItemPool.FLAMING_MUSHROOM);
    StorageRequest.addPulledItem(ItemPool.HOT_WAD);

    // Tested individually above, but why not?
    assertTrue(StorageRequest.itemPulledInRonin(ItemPool.FLAMING_MUSHROOM));
    assertTrue(StorageRequest.itemPulledInRonin(ItemPool.HOT_WAD));

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // Reset property
    Preferences.setString("_roninStoragePulls", "");

    // We expect there to be a single subinstance
    assertTrue(subinstances.size() == 1);

    TransferItemRequest rq = subinstances.get(0);

    // We expect there to be exactly 3 attachments
    assertTrue(rq.attachments != null);
    assertTrue(rq.attachments.length == 3);

    // We expect them to be exactly the not-yet-pulled items
    // We expect them to be limited to 1
    assertTrue(rq.attachments[0].getItemId() == ItemPool.MILK_OF_MAGNESIUM);
    assertTrue(rq.attachments[0].getCount() == 1);
    assertTrue(rq.attachments[1].getItemId() == ItemPool.CLOCKWORK_CHEF);
    assertTrue(rq.attachments[1].getCount() == 1);
    assertTrue(rq.attachments[2].getItemId() == ItemPool.COLD_WAD);
    assertTrue(rq.attachments[2].getCount() == 1);
  }

  @Test
  public void itShouldPullMeat() {
    storageSubInstanceSetup();

    List<AdventureResult> items = new ArrayList<>();

    // Make a list of 2 items to pull.

    items.add(new AdventureResult(AdventureLongCountResult.MEAT, 1000));
    items.add(new AdventureResult(AdventureLongCountResult.MEAT, 234));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.PULL_MEAT_FROM_STORAGE, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertTrue(subinstances.size() == 1);

    TransferItemRequest rq = subinstances.get(0);

    // We expect to reuse the original request as the subinstance
    assertEquals(request, rq);

    // We expect there to be 2 attachments - the originals
    assertTrue(rq.attachments != null);
    assertTrue(rq.attachments.length == 2);

    // We expect the total of the Meat values to be saved
    assertTrue(rq.getURLString().contains("amt=1234"));
  }

  // *** Here are tests for all the conditions that StorageRequest should check
  // *** before submitting a request to KoL. Since these short circuit the
  // *** run() method, we don't need to mock.

  private void storageRunMethodSetup() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("run method user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);
    // Not in error state
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  private StorageRequest makeZeroItemRequest() {
    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, new AdventureResult[0]);

    // Return a subinstance
    return makeSubinstance(request);
  }

  private StorageRequest makeSingleItemRequest(int itemId) {
    // Make a list of 1 item to pull.
    List<AdventureResult> items = new ArrayList<>();
    items.add(ItemPool.get(itemId, 1));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, attachments);

    // Return a subinstance
    return makeSubinstance(request);
  }

  private StorageRequest makeMeatRequest() {
    // Make a list of 1 item to pull.
    List<AdventureResult> items = new ArrayList<>();
    items.add(new AdventureResult(AdventureLongCountResult.MEAT, 1000));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.PULL_MEAT_FROM_STORAGE, attachments);

    // Return a subinstance
    return makeSubinstance(request);
  }

  private StorageRequest makeSubinstance(StorageRequest request) {
    // Make subinstance, just as TransferItemRequest does before calling run()
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertTrue(subinstances.size() == 1);

    TransferItemRequest rq = subinstances.get(0);

    // We expect it to be a StorageRequest
    assertTrue(rq instanceof StorageRequest);

    return (StorageRequest) rq;
  }

  @Test
  public void itShouldNotEmptyStorageInHardcore() {
    storageRunMethodSetup();

    // Test being in Hardcore
    KoLCharacter.setHardcore(true);

    // Make an request to empty storage
    StorageRequest request = new StorageRequest(StorageRequest.EMPTY_STORAGE);

    // Run it and verify failure
    request.run();
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
  }

  @Test
  public void itShouldNotPullMeatInHardcore() {
    storageRunMethodSetup();

    // Test being in Hardcore
    KoLCharacter.setHardcore(true);

    // Make a request with Meat
    StorageRequest request = makeMeatRequest();

    // Run it and verify failure
    request.run();
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
  }

  @Test
  public void itShouldNotPullNonFreePullsInHardcore() {
    storageRunMethodSetup();

    // Test being in Hardcore
    KoLCharacter.setHardcore(true);

    // Make a request with an Item
    StorageRequest request = makeSingleItemRequest(ItemPool.HOT_WAD);

    // Run it and verify failure
    request.run();
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
  }

  @Test
  public void itShouldNotEmptyStorageInRonin() {
    storageRunMethodSetup();

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Make an request to empty storage
    StorageRequest request = new StorageRequest(StorageRequest.EMPTY_STORAGE);

    // Run it and verify failure
    request.run();
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
  }

  @Test
  public void itShouldNotPullTwiceInRonin() {
    storageRunMethodSetup();

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Make a request with an Item
    StorageRequest request = makeSingleItemRequest(ItemPool.HOT_WAD);

    // Say that we've already pulled one today
    StorageRequest.addPulledItem(ItemPool.HOT_WAD);

    // Tested individually above, but why not?
    assertTrue(StorageRequest.itemPulledInRonin(ItemPool.HOT_WAD));

    // Run it and verify failure
    request.run();
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
  }

  @Test
  public void itShouldNotPullMeatInFistcore() {
    storageRunMethodSetup();

    // Test being in Fistcore
    KoLCharacter.setKingLiberated(false);
    KoLCharacter.setPath(Path.SURPRISING_FIST);

    // Make a request with Meat
    StorageRequest request = makeMeatRequest();

    // Run it and verify failure
    request.run();
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
  }

  @Test
  public void itShouldRequestZeroItems() {
    storageRunMethodSetup();

    // Make a request with no items
    StorageRequest request = makeZeroItemRequest();

    // Run it and verify failure
    request.run();
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
  }

  // *** Here are tests for StorageRequest.transferItems()

  private StorageRequest storageTransferItemsSetup() throws IOException {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("transfer items user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);
    // Not in error state
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Make a StorageRequest with specific URLstring and responseText

    // Make a list of 7 items to pull.
    List<AdventureResult> items = new ArrayList<>();
    items.add(ItemPool.get(ItemPool.EXTREME_AMULET, 1));
    items.add(ItemPool.get(ItemPool.BEER_HELMET, 1));
    items.add(ItemPool.get(ItemPool.BEJEWELED_PLEDGE_PIN, 1));
    items.add(ItemPool.get(ItemPool.BLACKBERRY_GALOSHES, 1));
    items.add(ItemPool.get(ItemPool.DIETING_PILL, 1));
    items.add(ItemPool.get(ItemPool.DISTRESSED_DENIM_PANTS, 1));
    items.add(ItemPool.get(ItemPool.SQUEEZE, 1));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[items.size()]);

    // Make a StorageRequest
    StorageRequest request = new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, attachments);

    // Return a subinstance
    StorageRequest subinstance = makeSubinstance(request);

    // Add the items to the URL
    for (int index = 0; index < attachments.length; index++) {
      subinstance.attachItem(attachments[index], index + 1);
    }

    // Three of the items are already in inventory.
    KoLConstants.inventory.clear();
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.EXTREME_AMULET));
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.DIETING_PILL));
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.SQUEEZE));

    // Load the responseText from saved HTML file
    String path = "request/test_request_storage_pulls.html";
    String html = Files.readString(Paths.get(path)).trim();
    request.responseText = html;

    // Voila! we are ready to test
    return subinstance;
  }

  @Test
  public void itShouldNonBulkTransferItems() throws IOException {

    // Load up our request/response
    StorageRequest request = storageTransferItemsSetup();
    String urlString = request.getURLString();
    String responseText = request.responseText;

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Clear out the set of Ronin Pulls
    Preferences.setString("_roninStoragePulls", "");
    StorageRequest.loadRoninStoragePulls();

    // Parse response and move items into inventory
    ConcoctionDatabase.setPullsRemaining(17);
    StorageRequest.transferItems(urlString, responseText, false);
    Preferences.setString("_roninStoragePulls", "");

    // Test that each item is now in inventory and is marked as pulled in ronin
    for (AdventureResult ar : request.attachments) {
      assertTrue(StorageRequest.itemPulledInRonin(ar));
      assertTrue(ar.getCount(KoLConstants.inventory) == 1);
    }

    // Test that pulls remaining has been decremented
    assertTrue(ConcoctionDatabase.getPullsRemaining() == 13);
  }

  @Test
  public void itShouldBulkTransferItems() throws IOException {

    // Load up our request/response
    StorageRequest request = storageTransferItemsSetup();
    String urlString = request.getURLString();
    String responseText = request.responseText;

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Clear out the set of Ronin Pulls
    Preferences.setString("_roninStoragePulls", "");
    StorageRequest.loadRoninStoragePulls();

    // Parse response and move items into inventory
    ConcoctionDatabase.setPullsRemaining(17);
    StorageRequest.transferItems(urlString, responseText, true);
    Preferences.setString("_roninStoragePulls", "");

    // Test that each item is now in inventory and is marked as pulled in ronin
    for (AdventureResult ar : request.attachments) {
      assertTrue(StorageRequest.itemPulledInRonin(ar));
      assertTrue(ar.getCount(KoLConstants.inventory) == 1);
    }

    // Test that pulls remaining has been decremented
    assertTrue(ConcoctionDatabase.getPullsRemaining() == 13);
  }
}
