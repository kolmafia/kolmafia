package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withNoItems;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRonin;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
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
import net.sourceforge.kolmafia.request.StorageRequest.StorageRequestType;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class StorageRequestTest {

  private final Set<Integer> pulledItemSet = new HashSet<>();
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
    assertEquals(2, pulledItemSet.size());
    assertTrue(pulledItemSet.contains(57));
    assertTrue(pulledItemSet.contains(60));

    // Duplicate elements with padding
    pulledItemProperty = "57 , 60 , 57, 60";
    StorageRequest.pullsStringToSet(pulledItemProperty, pulledItemSet);
    assertEquals(2, pulledItemSet.size());
    assertTrue(pulledItemSet.contains(57));
    assertTrue(pulledItemSet.contains(60));

    // Bogus elements
    pulledItemProperty = "57 , bogus 1 , bogus 2";
    StorageRequest.pullsStringToSet(pulledItemProperty, pulledItemSet);
    assertEquals(1, pulledItemSet.size());
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
    assertEquals(2, list.size());
    assertTrue(list.contains("57"));
    assertTrue(list.contains("60"));
  }

  @Test
  void itShouldAddItemsCorrectly() {
    roninStoragePrimitivesSetup();

    // Add an item. It must be present;
    StorageRequest.addPulledItem(pulledItemSet, 57);
    assertEquals(1, pulledItemSet.size());
    assertTrue(pulledItemSet.contains(57));

    // Add another item. It also must be present;
    StorageRequest.addPulledItem(pulledItemSet, 60);
    assertEquals(2, pulledItemSet.size());
    assertTrue(pulledItemSet.contains(57));
    assertTrue(pulledItemSet.contains(60));

    // Add duplicate item. It also must be present
    StorageRequest.addPulledItem(pulledItemSet, 57);
    assertEquals(2, pulledItemSet.size());
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
    assertTrue(StorageRequest.roninStoragePulls.isEmpty());
    Preferences.setString("_roninStoragePulls", "57,60");
    StorageRequest.loadRoninStoragePulls();
    assertEquals(2, StorageRequest.roninStoragePulls.size());
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldAddPulledItemInRonin() {
    roninStoragePropertySetup();
    String property = Preferences.getString("_roninStoragePulls");
    assertTrue(property.isEmpty());
    StorageRequest.addPulledItem(ItemPool.get(ItemPool.FIVE_ALARM_SAUCEPAN));
    StorageRequest.addPulledItem(ItemPool.MACE_OF_THE_TORTOISE);
    property = Preferences.getString("_roninStoragePulls");
    assertFalse(property.isEmpty());
    assertTrue(property.contains(Integer.toString(ItemPool.FIVE_ALARM_SAUCEPAN)));
    assertTrue(property.contains(Integer.toString(ItemPool.MACE_OF_THE_TORTOISE)));
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldNotAddPulledItemOutOfRonin() {
    roninStoragePropertySetup();
    KoLCharacter.setRonin(false);
    String property = Preferences.getString("_roninStoragePulls");
    assertTrue(property.isEmpty());
    StorageRequest.addPulledItem(ItemPool.get(ItemPool.FIVE_ALARM_SAUCEPAN));
    StorageRequest.addPulledItem(ItemPool.MACE_OF_THE_TORTOISE);
    property = Preferences.getString("_roninStoragePulls");
    assertTrue(property.isEmpty());
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldFindPulledItemsInRonin() {
    roninStoragePropertySetup();
    StorageRequest.addPulledItem(ItemPool.get(ItemPool.FIVE_ALARM_SAUCEPAN));
    StorageRequest.addPulledItem(ItemPool.MACE_OF_THE_TORTOISE);
    assertTrue(StorageRequest.itemPulledInRonin(ItemPool.get(ItemPool.FIVE_ALARM_SAUCEPAN)));
    assertTrue(StorageRequest.itemPulledInRonin(ItemPool.MACE_OF_THE_TORTOISE));
    assertFalse(StorageRequest.itemPulledInRonin(100));
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldNotFindPulledItemsOutOfRonin() {
    roninStoragePropertySetup();
    KoLCharacter.setRonin(false);
    StorageRequest.addPulledItem(ItemPool.get(ItemPool.FIVE_ALARM_SAUCEPAN));
    StorageRequest.addPulledItem(ItemPool.MACE_OF_THE_TORTOISE);
    assertFalse(StorageRequest.itemPulledInRonin(ItemPool.get(ItemPool.FIVE_ALARM_SAUCEPAN)));
    assertFalse(StorageRequest.itemPulledInRonin(ItemPool.MACE_OF_THE_TORTOISE));
    assertFalse(StorageRequest.itemPulledInRonin(100));
    Preferences.setString("_roninStoragePulls", "");
  }

  @Test
  public void itShouldSaveRoninStoragePulls() {
    roninStoragePropertySetup();
    String property = Preferences.getString("_roninStoragePulls");
    assertTrue(property.isEmpty());
    assertTrue(StorageRequest.roninStoragePulls.isEmpty());
    StorageRequest.roninStoragePulls.add(ItemPool.FIVE_ALARM_SAUCEPAN);
    StorageRequest.roninStoragePulls.add(ItemPool.MACE_OF_THE_TORTOISE);
    StorageRequest.saveRoninStoragePulls();
    property = Preferences.getString("_roninStoragePulls");
    assertFalse(property.isEmpty());
    assertTrue(property.contains(Integer.toString(ItemPool.FIVE_ALARM_SAUCEPAN)));
    assertTrue(property.contains(Integer.toString(ItemPool.MACE_OF_THE_TORTOISE)));
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
    AdventureResult[] attachments = items.toArray(new AdventureResult[0]);

    // Test NOT being in Ronin
    KoLCharacter.setRonin(false);

    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertEquals(1, subinstances.size());

    TransferItemRequest rq = subinstances.getFirst();

    // We expect there to be exactly 5 attachments
    assertNotNull(rq.attachments);
    assertEquals(5, rq.attachments.length);

    // We expect them to be exactly the items that are in storage
    // We expect them to be limited by min(requested, available)
    assertEquals(ItemPool.MILK_OF_MAGNESIUM, rq.attachments[0].getItemId());
    assertEquals(4, rq.attachments[0].getCount());
    assertEquals(ItemPool.FLAMING_MUSHROOM, rq.attachments[1].getItemId());
    assertEquals(2, rq.attachments[1].getCount());
    assertEquals(ItemPool.CLOCKWORK_CHEF, rq.attachments[2].getItemId());
    assertEquals(1, rq.attachments[2].getCount());
    assertEquals(ItemPool.HOT_WAD, rq.attachments[3].getItemId());
    assertEquals(5, rq.attachments[3].getCount());
    assertEquals(ItemPool.COLD_WAD, rq.attachments[4].getItemId());
    assertEquals(5, rq.attachments[4].getCount());
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
    AdventureResult[] attachments = items.toArray(new AdventureResult[0]);

    // Test NOT being in Ronin
    KoLCharacter.setRonin(false);

    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be two subinstances
    assertEquals(2, subinstances.size());

    TransferItemRequest rq1 = subinstances.getFirst();

    // We expect there to be exactly 11 attachments in the first request
    assertNotNull(rq1.attachments);
    assertEquals(11, rq1.attachments.length);

    // We expect them to be exactly the items that are in storage
    assertEquals(ItemPool.MILK_OF_MAGNESIUM, rq1.attachments[0].getItemId());
    assertEquals(1, rq1.attachments[0].getCount());
    assertEquals(ItemPool.FLAMING_MUSHROOM, rq1.attachments[1].getItemId());
    assertEquals(1, rq1.attachments[1].getCount());
    assertEquals(ItemPool.FROZEN_MUSHROOM, rq1.attachments[2].getItemId());
    assertEquals(1, rq1.attachments[2].getCount());
    assertEquals(ItemPool.STINKY_MUSHROOM, rq1.attachments[3].getItemId());
    assertEquals(1, rq1.attachments[3].getCount());
    assertEquals(ItemPool.CLOCKWORK_BARTENDER, rq1.attachments[4].getItemId());
    assertEquals(1, rq1.attachments[4].getCount());
    assertEquals(ItemPool.CLOCKWORK_CHEF, rq1.attachments[5].getItemId());
    assertEquals(1, rq1.attachments[5].getCount());
    assertEquals(ItemPool.CLOCKWORK_MAID, rq1.attachments[6].getItemId());
    assertEquals(1, rq1.attachments[6].getCount());
    assertEquals(ItemPool.HOT_WAD, rq1.attachments[7].getItemId());
    assertEquals(1, rq1.attachments[7].getCount());
    assertEquals(ItemPool.COLD_WAD, rq1.attachments[8].getItemId());
    assertEquals(1, rq1.attachments[8].getCount());
    assertEquals(ItemPool.SPOOKY_WAD, rq1.attachments[9].getItemId());
    assertEquals(1, rq1.attachments[9].getCount());
    assertEquals(ItemPool.STENCH_WAD, rq1.attachments[10].getItemId());
    assertEquals(1, rq1.attachments[10].getCount());

    TransferItemRequest rq2 = subinstances.get(1);

    // We expect there to be exactly 6 attachments in the second request
    assertNotNull(rq2.attachments);
    assertEquals(6, rq2.attachments.length);

    // We expect them to be exactly the items that are in storage
    assertEquals(ItemPool.SLEAZE_WAD, rq2.attachments[0].getItemId());
    assertEquals(1, rq2.attachments[0].getCount());
    assertEquals(ItemPool.HOT_HI_MEIN, rq2.attachments[1].getItemId());
    assertEquals(1, rq2.attachments[1].getCount());
    assertEquals(ItemPool.COLD_HI_MEIN, rq2.attachments[2].getItemId());
    assertEquals(1, rq2.attachments[2].getCount());
    assertEquals(ItemPool.SPOOKY_HI_MEIN, rq2.attachments[3].getItemId());
    assertEquals(1, rq2.attachments[3].getCount());
    assertEquals(ItemPool.STINKY_HI_MEIN, rq2.attachments[4].getItemId());
    assertEquals(1, rq2.attachments[4].getCount());
    assertEquals(ItemPool.SLEAZY_HI_MEIN, rq2.attachments[5].getItemId());
    assertEquals(1, rq2.attachments[5].getCount());
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
    AdventureResult[] attachments = items.toArray(new AdventureResult[0]);

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertEquals(1, subinstances.size());

    TransferItemRequest rq = subinstances.getFirst();

    // We expect there to be exactly 5 attachments
    assertNotNull(rq.attachments);
    assertEquals(5, rq.attachments.length);

    // We expect them to be exactly the items that are in storage
    // We expect them to be limited to 1
    assertEquals(ItemPool.MILK_OF_MAGNESIUM, rq.attachments[0].getItemId());
    assertEquals(1, rq.attachments[0].getCount());
    assertEquals(ItemPool.FLAMING_MUSHROOM, rq.attachments[1].getItemId());
    assertEquals(1, rq.attachments[1].getCount());
    assertEquals(ItemPool.CLOCKWORK_CHEF, rq.attachments[2].getItemId());
    assertEquals(1, rq.attachments[2].getCount());
    assertEquals(ItemPool.HOT_WAD, rq.attachments[3].getItemId());
    assertEquals(1, rq.attachments[3].getCount());
    assertEquals(ItemPool.COLD_WAD, rq.attachments[4].getItemId());
    assertEquals(1, rq.attachments[4].getCount());
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
    AdventureResult[] attachments = items.toArray(new AdventureResult[0]);

    // Test NOT being in Ronin
    KoLCharacter.setRonin(false);

    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertEquals(1, subinstances.size());

    TransferItemRequest rq = subinstances.getFirst();

    // We expect there to be exactly 5 attachments
    assertNotNull(rq.attachments);
    assertEquals(5, rq.attachments.length);

    // We expect them to be exactly the items that are in storage
    // We expect them to be limited by min(requested, available)
    assertEquals(ItemPool.MILK_OF_MAGNESIUM, rq.attachments[0].getItemId());
    assertEquals(4, rq.attachments[0].getCount());
    assertEquals(ItemPool.BRICK, rq.attachments[1].getItemId());
    assertEquals(3, rq.attachments[1].getCount());
    assertEquals(ItemPool.FLAMING_MUSHROOM, rq.attachments[2].getItemId());
    assertEquals(2, rq.attachments[2].getCount());
    assertEquals(ItemPool.TOILET_PAPER, rq.attachments[3].getItemId());
    assertEquals(3, rq.attachments[3].getCount());
    assertEquals(ItemPool.CLOCKWORK_CHEF, rq.attachments[4].getItemId());
    assertEquals(1, rq.attachments[4].getCount());
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
    AdventureResult[] attachments = items.toArray(new AdventureResult[0]);

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be 9 subinstances
    assertEquals(9, subinstances.size());

    // SubInstance #1
    TransferItemRequest rq1 = subinstances.getFirst();

    // We expect there to be exactly 1 attachment
    assertNotNull(rq1.attachments);
    assertEquals(1, rq1.attachments.length);
    assertEquals(ItemPool.MILK_OF_MAGNESIUM, rq1.attachments[0].getItemId());
    assertEquals(1, rq1.attachments[0].getCount());

    // SubInstance #2
    TransferItemRequest rq2 = subinstances.get(1);

    // We expect there to be exactly 1 attachment
    assertNotNull(rq2.attachments);
    assertEquals(1, rq2.attachments.length);
    assertEquals(ItemPool.BRICK, rq2.attachments[0].getItemId());
    assertEquals(1, rq2.attachments[0].getCount());

    // SubInstance #3
    TransferItemRequest rq3 = subinstances.get(2);

    // We expect there to be exactly 1 attachment
    assertNotNull(rq3.attachments);
    assertEquals(1, rq3.attachments.length);
    assertEquals(ItemPool.BRICK, rq3.attachments[0].getItemId());
    assertEquals(1, rq3.attachments[0].getCount());

    // SubInstance #4
    TransferItemRequest rq4 = subinstances.get(3);

    // We expect there to be exactly 1 attachment
    assertNotNull(rq4.attachments);
    assertEquals(1, rq4.attachments.length);
    assertEquals(ItemPool.BRICK, rq4.attachments[0].getItemId());
    assertEquals(1, rq4.attachments[0].getCount());

    // SubInstance #5
    TransferItemRequest rq5 = subinstances.get(4);

    // We expect there to be exactly 1 attachment
    assertNotNull(rq5.attachments);
    assertEquals(1, rq5.attachments.length);
    assertEquals(ItemPool.FLAMING_MUSHROOM, rq5.attachments[0].getItemId());
    assertEquals(1, rq5.attachments[0].getCount());

    // SubInstance #6
    TransferItemRequest rq6 = subinstances.get(5);

    // We expect there to be exactly 1 attachment
    assertNotNull(rq6.attachments);
    assertEquals(1, rq6.attachments.length);
    assertEquals(ItemPool.TOILET_PAPER, rq6.attachments[0].getItemId());
    assertEquals(1, rq6.attachments[0].getCount());

    // SubInstance #7
    TransferItemRequest rq7 = subinstances.get(6);

    // We expect there to be exactly 1 attachment
    assertNotNull(rq7.attachments);
    assertEquals(1, rq7.attachments.length);
    assertEquals(ItemPool.TOILET_PAPER, rq7.attachments[0].getItemId());
    assertEquals(1, rq7.attachments[0].getCount());

    // SubInstance #8
    TransferItemRequest rq8 = subinstances.get(7);

    // We expect there to be exactly 1 attachment
    assertNotNull(rq8.attachments);
    assertEquals(1, rq8.attachments.length);
    assertEquals(ItemPool.TOILET_PAPER, rq8.attachments[0].getItemId());
    assertEquals(1, rq8.attachments[0].getCount());

    // SubInstance #9
    TransferItemRequest rq9 = subinstances.get(8);

    // We expect there to be exactly 1 attachment
    assertNotNull(rq9.attachments);
    assertEquals(1, rq9.attachments.length);
    assertEquals(ItemPool.CLOCKWORK_CHEF, rq9.attachments[0].getItemId());
    assertEquals(1, rq9.attachments[0].getCount());
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
    AdventureResult[] attachments = items.toArray(new AdventureResult[0]);

    // Test being in Ronin
    KoLCharacter.setRonin(true);

    // Tell KoLmafia that 2 of the items have been pulled today
    StorageRequest.addPulledItem(ItemPool.FLAMING_MUSHROOM);
    StorageRequest.addPulledItem(ItemPool.HOT_WAD);

    // Tested individually above, but why not?
    assertTrue(StorageRequest.itemPulledInRonin(ItemPool.FLAMING_MUSHROOM));
    assertTrue(StorageRequest.itemPulledInRonin(ItemPool.HOT_WAD));

    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // Reset property
    Preferences.setString("_roninStoragePulls", "");

    // We expect there to be a single subinstance
    assertEquals(1, subinstances.size());

    TransferItemRequest rq = subinstances.getFirst();

    // We expect there to be exactly 3 attachments
    assertNotNull(rq.attachments);
    assertEquals(3, rq.attachments.length);

    // We expect them to be exactly the not-yet-pulled items
    // We expect them to be limited to 1
    assertEquals(ItemPool.MILK_OF_MAGNESIUM, rq.attachments[0].getItemId());
    assertEquals(1, rq.attachments[0].getCount());
    assertEquals(ItemPool.CLOCKWORK_CHEF, rq.attachments[1].getItemId());
    assertEquals(1, rq.attachments[1].getCount());
    assertEquals(ItemPool.COLD_WAD, rq.attachments[2].getItemId());
    assertEquals(1, rq.attachments[2].getCount());
  }

  @Test
  public void itShouldPullMeat() {
    storageSubInstanceSetup();

    List<AdventureResult> items = new ArrayList<>();

    // Make a list of 2 items to pull.

    items.add(new AdventureResult(AdventureLongCountResult.MEAT, 1000));
    items.add(new AdventureResult(AdventureLongCountResult.MEAT, 234));

    // StorageRequest wants an actual Java array
    AdventureResult[] attachments = items.toArray(new AdventureResult[0]);

    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequestType.PULL_MEAT_FROM_STORAGE, attachments);

    // Generate subinstances.
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertEquals(1, subinstances.size());

    TransferItemRequest rq = subinstances.getFirst();

    // We expect to reuse the original request as the subinstance
    assertEquals(request, rq);

    // We expect there to be 2 attachments - the originals
    assertNotNull(rq.attachments);
    assertEquals(2, rq.attachments.length);

    // We expect the total of the Meat values to be saved
    assertTrue(rq.getURLString().contains("amt=1234"));
  }

  // *** Here are tests for all the conditions that StorageRequest should check
  // *** before submitting a request to KoL. Since these short circuit the
  // *** run() method, we don't need to mock.

  private StorageRequest makeSubinstance(StorageRequest request) {
    // Make subinstance, just as TransferItemRequest does before calling run()
    ArrayList<TransferItemRequest> subinstances = request.generateSubInstances();

    // We expect there to be a single subinstance
    assertThat(subinstances, hasSize(1));

    TransferItemRequest rq = subinstances.getFirst();

    // We expect it to be a StorageRequest
    assertThat(rq, instanceOf(StorageRequest.class));

    return (StorageRequest) rq;
  }

  @Nested
  class Run {
    @BeforeEach
    public void storageRunMethodSetup() {
      // Simulate logging out and back in again.
      KoLCharacter.reset("");
      KoLCharacter.reset("run method user");
      // Reset preferences to defaults.
      KoLCharacter.reset(true);
    }

    private StorageRequest makeZeroItemRequest() {
      // Make a StorageRequest
      StorageRequest request =
          new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, new AdventureResult[0]);

      // Return a subinstance
      return makeSubinstance(request);
    }

    private StorageRequest makeSingleItemRequest(int itemId) {
      // Make a list of 1 item to pull.
      var attachments = List.of(ItemPool.get(itemId, 1)).toArray(new AdventureResult[0]);

      // Make a StorageRequest
      StorageRequest request =
          new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, attachments);

      // Return a subinstance
      return makeSubinstance(request);
    }

    private StorageRequest makeMeatRequest() {
      // Make a list of 1 item to pull.
      var attachments =
          List.of(new AdventureResult(AdventureLongCountResult.MEAT, 1000))
              .toArray(new AdventureResult[0]);

      // Make a StorageRequest
      StorageRequest request =
          new StorageRequest(StorageRequestType.PULL_MEAT_FROM_STORAGE, attachments);

      // Return a subinstance
      return makeSubinstance(request);
    }

    @Test
    public void itShouldNotEmptyStorageInHardcore() {
      var cleanups = new Cleanups(withHardcore(true), withContinuationState());

      try (cleanups) {
        // Make an request to empty storage
        StorageRequest request = new StorageRequest(StorageRequestType.EMPTY_STORAGE);

        // Run it and verify failure
        request.run();
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
      }
    }

    @Test
    public void itShouldNotPullMeatInHardcore() {
      var cleanups = new Cleanups(withHardcore(true), withContinuationState());

      try (cleanups) {
        // Make a request with Meat
        StorageRequest request = makeMeatRequest();

        // Run it and verify failure
        request.run();
        assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
      }
    }

    @Test
    public void itShouldNotPullNonFreePullsInHardcore() {
      var cleanups = new Cleanups(withHardcore(true), withContinuationState());

      try (cleanups) {
        // Make a request with an Item
        StorageRequest request = makeSingleItemRequest(ItemPool.HOT_WAD);

        // Run it and verify failure
        request.run();
        assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
      }
    }

    @Test
    public void itShouldNotEmptyStorageInRonin() {
      var cleanups = new Cleanups(withRonin(true), withContinuationState());

      try (cleanups) {
        // Make an request to empty storage
        StorageRequest request = new StorageRequest(StorageRequestType.EMPTY_STORAGE);

        // Run it and verify failure
        request.run();
        assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
      }
    }

    @Test
    public void itShouldNotPullTwiceInRonin() {
      var cleanups = new Cleanups(withRonin(true), withContinuationState());

      try (cleanups) {
        // Make a request with an Item
        StorageRequest request = makeSingleItemRequest(ItemPool.HOT_WAD);

        // Say that we've already pulled one today
        StorageRequest.addPulledItem(ItemPool.HOT_WAD);

        // Tested individually above, but why not?
        assertTrue(StorageRequest.itemPulledInRonin(ItemPool.HOT_WAD));

        // Run it and verify failure
        request.run();
        assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
      }
    }

    @Test
    public void itShouldNotPullMeatInFistcore() {
      var cleanups =
          new Cleanups(
              withProperty("kingLiberated", false),
              withPath(Path.SURPRISING_FIST),
              withContinuationState());

      try (cleanups) {
        // Make a request with Meat
        StorageRequest request = makeMeatRequest();

        // Run it and verify failure
        request.run();
        assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
      }
    }

    @Test
    public void itShouldRequestZeroItems() {
      var cleanups = new Cleanups(withContinuationState());

      try (cleanups) {
        // Make a request with no items
        StorageRequest request = makeZeroItemRequest();

        // Run it and verify failure
        request.run();
        assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
      }
    }
  }

  // *** Here are tests for StorageRequest.transferItems()

  private StorageRequest storageTransferItemsSetup() {
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
    AdventureResult[] attachments = items.toArray(new AdventureResult[0]);

    // Make a StorageRequest
    StorageRequest request =
        new StorageRequest(StorageRequestType.STORAGE_TO_INVENTORY, attachments);

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

    // The other four are in storage.
    KoLConstants.storage.clear();
    AdventureResult.addResultToList(KoLConstants.storage, ItemPool.get(ItemPool.BEER_HELMET));
    AdventureResult.addResultToList(
        KoLConstants.storage, ItemPool.get(ItemPool.BEJEWELED_PLEDGE_PIN));
    AdventureResult.addResultToList(
        KoLConstants.storage, ItemPool.get(ItemPool.BLACKBERRY_GALOSHES));
    AdventureResult.addResultToList(
        KoLConstants.storage, ItemPool.get(ItemPool.DISTRESSED_DENIM_PANTS));

    // Load the responseText from saved HTML file
    String path = "request/test_request_storage_pulls.html";
    request.responseText = html(path);

    // Voila! we are ready to test
    return subinstance;
  }

  @Test
  public void itShouldNonBulkTransferItems() {

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

    // Test that each item is now in inventory and not in storage, and is
    // marked as pulled in ronin
    for (AdventureResult ar : request.attachments) {
      assertTrue(StorageRequest.itemPulledInRonin(ar));
      assertEquals(1, ar.getCount(KoLConstants.inventory));
      assertEquals(0, ar.getCount(KoLConstants.storage));
    }

    // Test that pulls remaining has been decremented
    assertEquals(13, ConcoctionDatabase.getPullsRemaining());
  }

  @Test
  public void itShouldBulkTransferItems() {

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

    // Test that each item is now in inventory and not in storage, and is
    // marked as pulled in ronin
    for (AdventureResult ar : request.attachments) {
      assertTrue(StorageRequest.itemPulledInRonin(ar));
      assertEquals(1, ar.getCount(KoLConstants.inventory));
      assertEquals(0, ar.getCount(KoLConstants.storage));
    }

    // Test that pulls remaining has been decremented
    assertEquals(13, ConcoctionDatabase.getPullsRemaining());
  }

  @Nested
  class TransferParsing {
    // A storage "pull" action can ask for up to 11 items at a time:
    //     whichitem<N>=<ITEMID>&howmany<N>=<COUNT>
    //
    // The responseText contains a message for each whichitem/howmany pair:
    //     <b>beer helmet (1)</b> moved from storage to inventory.<br />
    //     You already pulled one of those today.
    //     You haven't got any of that item in your storage.
    //
    // If the same itemId appears more than once, only the first is processed.
    // If you attempt to pull an item which is not in inventory, subsequent items are ignored.

    private void addItem(StringBuilder buf, int index, int itemId, int count) {
      buf.append("&whichitem");
      buf.append(index);
      buf.append("=");
      buf.append(itemId);
      buf.append("&howmany");
      buf.append(index);
      buf.append("=");
      buf.append(count);
    }

    @Test
    public void itShouldDetectAlreadyPulledItems() {
      // storage.php?action=pull&ajax=1&whichitem1=594&howmany1=1&whichitem2=2069&howmany2=1&whichitem3=2353&howmany3=1&whichitem4=4659&howmany4=1&whichitem5=9707&howmany5=1&whichitem6=2070&howmany6=1&whichitem7=3399&howmany7=1
      //     You already pulled one of those today.
      //     <b>beer helmet (1)</b> moved from storage to inventory.<br />
      //     <b>bejeweled pledge pin (1)</b> moved from storage to inventory.<br />
      //     <b>blackberry galoshes (1)</b> moved from storage to inventory.<br />
      //     You already pulled one of those today.
      //     <b>distressed denim pants (1)</b> moved from storage to inventory.<br />
      //     You already pulled one of those today.
      // test_pull_already_pulled_item.html

      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withNoItems(),
              withItemInStorage(ItemPool.BEER_HELMET, 1),
              withItemInStorage(ItemPool.BEJEWELED_PLEDGE_PIN, 1),
              withItemInStorage(ItemPool.BLACKBERRY_GALOSHES, 1),
              withItemInStorage(ItemPool.DISTRESSED_DENIM_PANTS, 1));
      try (cleanups) {
        client.addResponse(200, html("request/test_pull_already_pulled_items.html"));

        StringBuilder buf = new StringBuilder("storage.php?action=pull&ajax=1");
        addItem(buf, 1, ItemPool.EXTREME_AMULET, 1);
        addItem(buf, 2, ItemPool.BEER_HELMET, 1);
        addItem(buf, 3, ItemPool.BEJEWELED_PLEDGE_PIN, 1);
        addItem(buf, 4, ItemPool.BLACKBERRY_GALOSHES, 1);
        addItem(buf, 5, ItemPool.DIETING_PILL, 1);
        addItem(buf, 6, ItemPool.DISTRESSED_DENIM_PANTS, 1);
        addItem(buf, 7, ItemPool.SQUEEZE, 1);

        var url = buf.toString();
        var request = new GenericRequest(url);
        request.run();

        assertThat(InventoryManager.getCount(ItemPool.BEER_HELMET), is(1));
        assertThat(InventoryManager.getCount(ItemPool.BEJEWELED_PLEDGE_PIN), is(1));
        assertThat(InventoryManager.getCount(ItemPool.BLACKBERRY_GALOSHES), is(1));
        assertThat(InventoryManager.getCount(ItemPool.DISTRESSED_DENIM_PANTS), is(1));
      }
    }

    @Test
    public void itShouldSkipDuplicateItems() {
      // storage.php?action=pull&ajax=1&whichitem1=1903&howmany1=10&whichitem2=1903&howmany2=20&whichitem3=1904&howmany3=10&pwd
      //     <b>4-ball (10)</b> moved from storage to inventory.<br />
      //     <b>5-ball (10)</b> moved from storage to inventory.
      // test_pull_duplicate_item.html

      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withNoItems(),
              withItemInStorage(ItemPool.FOUR_BALL, 12),
              withItemInStorage(ItemPool.FIVE_BALL, 10));
      try (cleanups) {
        client.addResponse(200, html("request/test_pull_duplicate_item.html"));

        StringBuilder buf = new StringBuilder("storage.php?action=pull&ajax=1");
        addItem(buf, 1, ItemPool.FOUR_BALL, 10);
        addItem(buf, 2, ItemPool.FOUR_BALL, 2);
        addItem(buf, 3, ItemPool.FIVE_BALL, 10);

        var url = buf.toString();
        var request = new GenericRequest(url);
        request.run();

        assertThat(InventoryManager.getCount(ItemPool.FOUR_BALL), is(10));
        assertThat(InventoryManager.getCount(ItemPool.FIVE_BALL), is(10));
      }
    }

    @Test
    public void itShouldSkipMissingItems() {
      // storage.php?action=pull&ajax=1&whichitem1=1906&howmany1=10&whichitem2=1913&howmany2=1&whichitem3=1907&howmany3=10&pwd
      //    <b>7-ball (10)</b> moved from storage to inventory.<br />
      //    You haven't got any of that item in your storage.
      // test_pull_missing_item.html

      var builder = new FakeHttpClientBuilder();
      var client = builder.client;
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withNoItems(),
              withItemInStorage(ItemPool.SEVEN_BALL, 12),
              withItemInStorage(ItemPool.EIGHT_BALL, 10));
      try (cleanups) {
        client.addResponse(200, html("request/test_pull_missing_item.html"));

        StringBuilder buf = new StringBuilder("storage.php?action=pull&ajax=1");
        addItem(buf, 1, ItemPool.SEVEN_BALL, 10);
        addItem(buf, 2, ItemPool.CLOCKWORK_HANDLE, 1);
        addItem(buf, 3, ItemPool.EIGHT_BALL, 10);

        var url = buf.toString();
        var request = new GenericRequest(url);
        request.run();

        assertThat(InventoryManager.getCount(ItemPool.SEVEN_BALL), is(10));
        assertThat(InventoryManager.getCount(ItemPool.CLOCKWORK_HANDLE), is(0));
        assertThat(InventoryManager.getCount(ItemPool.EIGHT_BALL), is(0));
      }
    }
  }
}
