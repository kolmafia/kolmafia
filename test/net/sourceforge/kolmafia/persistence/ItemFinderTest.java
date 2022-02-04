package net.sourceforge.kolmafia.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ItemFinderTest {

  @BeforeEach
  private void itemFinderSetup() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("item finder user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);

    // Not in error state
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Default is not in Ronin or Hardcore
    KoLCharacter.setRonin(false);
    KoLCharacter.setHardcore(false);
    CharPaneRequest.setCanInteract(true);

    KoLConstants.inventory.clear();
    KoLConstants.closet.clear();
    KoLConstants.storage.clear();
    KoLConstants.freepulls.clear();

    KoLCharacter.setAvailableMeat(0);
    KoLCharacter.setClosetMeat(0);
    KoLCharacter.setStorageMeat(0);
  }

  private void addToInventory(int itemId, int count) {
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(itemId, count));
  }

  private void addToCloset(int itemId, int count) {
    AdventureResult.addResultToList(KoLConstants.closet, ItemPool.get(itemId, count));
  }

  private void addToStorage(int itemId, int count) {
    AdventureResult.addResultToList(KoLConstants.storage, ItemPool.get(itemId, count));
  }

  private void addToFreePulls(int itemId, int count) {
    AdventureResult.addResultToList(KoLConstants.freepulls, ItemPool.get(itemId, count));
  }

  // ItemFinder is the utility which accepts a parameter string (as typed by a
  // user in the gCLI) and parses it into an array of AdventureResults,
  //
  // The string can specify a single item or a comma seperated list of items.
  //
  // The user can specify a "count" for each item, including wildcards:
  // "*" means "all available and "-N" means "all except N"

  // Primitives:
  //
  // List<String> getMatchingNames(String)
  // String getFirstMatchingItemName(List, String, Match)
  // AdventureResult getFirstMatchingItem(String, boolean, List, Match)
  // AdventureResult[] getMatchingItemList(String, boolean, List, Match)

  // getMatchingItemList is implemented using getFirstMatchingItem
  // getFirstMatchingItem is implemented using getFirstMatchingItemName
  // getFirstMatchingItemName is implemented using getMatchingNames

  // Therefore, this test suite focuses on testing getMatchingItemList

  // The first thing to test is a string containing a single item
  //
  // "" -> ERROR
  // "bogus" -> ERROR
  // seal tooth
  // 1 Meat
  // mmj -> magical mystery juice
  // 1337 7r0uZ0RZ
  // Tea, Earl Grey, Hot
  // [7961]Staff of Ed
  // [7961]
  //  ¶7961
  //
  // The "pilcrow" character followed by itemID was invented by Jason
  // Harper to allow ASH RuntimeLibrary methods to call CLI commands
  // ("but", "closet", etc.) with no ambiguity.
  //
  // The [itemId] forms were added later as general disambiguation for
  // multiple object types in ASH
  //
  // Item names can contain a comma. Even though the parameter string can
  // contain multiple comma-separated items, we do not allow or expect you to
  // escape the comma. Instead, you can only specify such an item (by name) if
  // it is the entire parameter string.
  //
  // Although the user can specify N of an item, item names can start with a
  // number. Tricky!
  //
  // Lastly notice that we do "fuzzy matching" using ItemDatabase.getMatchingNames(),
  // which uses StringUtilities.getMatchingNames().
  // That package deserves its own test suite.
  //
  // also:
  //
  // 3215 Meat -> MEAT, not an item

  @Test
  public void itShouldParseSingleItems() {

    // This set of tests will use null as the source list. That means
    // that it will not restrict its matches to what is available in a
    // specific list. The only exception is if you use wildcard counts -
    // * or -N - in which case, it assumes inventory. We'll test wildcard
    // counts separately.

    AdventureResult[] results = null;

    // Test empty string, no error on failure

    results = ItemFinder.getMatchingItemList("", false, null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 0);

    // Test empty string, error on failure

    results = ItemFinder.getMatchingItemList("", true, null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(results != null);
    assertTrue(results.length == 0);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test unsuccessful exact match for bogus string, no error on failure

    results = ItemFinder.getMatchingItemList("Bogus", false, null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 0);

    // Test unsuccessful exact match for bogus string, error on failure

    results = ItemFinder.getMatchingItemList("Bigfoot", true, null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(results != null);
    assertTrue(results.length == 0);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test successful exact match for entire string.

    results = ItemFinder.getMatchingItemList("toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 1);

    // Test successful exact match for entire string where item name starts
    // with a number

    results = ItemFinder.getMatchingItemList("1337 7r0uZ0RZ", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.ELITE_TROUSERS);
    assertTrue(results[0].getCount() == 1);

    // Test successful exact match for entire string where item name contains
    // commas

    results = ItemFinder.getMatchingItemList("Tea, Earl Grey, Hot", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.EARL_GREY);
    assertTrue(results[0].getCount() == 1);

    // Test successful exact match for item specified by pilcrow
    results = ItemFinder.getMatchingItemList("¶7961", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == 7961);
    assertTrue(results[0].getCount() == 1);

    // Test for the item whose name contains a pilcrow - &para;
    results =
        ItemFinder.getMatchingItemList("Knob G&Atilde;&para;blin l&Atilde;&sup2;ve potion", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.BUGGED_POTION);
    assertTrue(results[0].getCount() == 1);

    // Test successful exact match for item specified by itemId
    results = ItemFinder.getMatchingItemList("[7961]", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == 7961);
    assertTrue(results[0].getCount() == 1);

    // Test successful exact match for item specified by itemId and name
    results = ItemFinder.getMatchingItemList("[7961]Staff of Ed", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == 7961);
    assertTrue(results[0].getCount() == 1);

    // Test successful exact match for item specified by itemId and bogus name
    // *** One could argue this should check that the name agrees with item
    results = ItemFinder.getMatchingItemList("[7961]Staff of Edward", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == 7961);
    assertTrue(results[0].getCount() == 1);

    // Single test for fuzzy matching. As I said, that deserves its own test
    // suite. elsewhere.
    results = ItemFinder.getMatchingItemList("mmj", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.MMJ);
    assertTrue(results[0].getCount() == 1);
  }

  // Next, test items preceded by a count - a simple integer

  @Test
  public void itShouldParseSingleItemsWithCount() {

    // Now test single items with a count + item

    AdventureResult[] results = null;

    // count + item name
    results = ItemFinder.getMatchingItemList("2 toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 2);

    // count + item name starts with a number

    results = ItemFinder.getMatchingItemList("2 1337 7r0uZ0RZ", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.ELITE_TROUSERS);
    assertTrue(results[0].getCount() == 2);

    // count + item name contains commas

    results = ItemFinder.getMatchingItemList("2 Tea, Earl Grey, Hot", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.EARL_GREY);
    assertTrue(results[0].getCount() == 2);

    // count + item specified by pilcrow
    results = ItemFinder.getMatchingItemList("2 ¶7961", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == 7961);
    assertTrue(results[0].getCount() == 2);

    // count + item specified by itemId
    results = ItemFinder.getMatchingItemList("2 [7961]", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == 7961);
    assertTrue(results[0].getCount() == 2);

    // count + item specified by itemId and name
    results = ItemFinder.getMatchingItemList("2 [7961]Staff of Ed", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == 7961);
    assertTrue(results[0].getCount() == 2);

    // Single test for fuzzy matching. As I said, that deserves its own test
    // suite. Elsewhere.
    results = ItemFinder.getMatchingItemList("10 mmj", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.MMJ);
    assertTrue(results[0].getCount() == 10);
  }

  // You can specify meat via "COUNT meat".

  @Test
  public void itShouldParseMeat() {

    AdventureResult[] results = null;

    // Test that you can get a Meat result
    results = ItemFinder.getMatchingItemList("1000 meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 1000);

    // And the funny special case of the actual item named "1 Meat"
    results = ItemFinder.getMatchingItemList("1 meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.ONE_MEAT);
    assertTrue(results[0].getCount() == 1);

    // Meat should be case insensitive

    // *** This currently fails
    // results = ItemFinder.getMatchingItemList("1000 Meat", null);
    // assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    // assertTrue(results != null);
    // assertTrue(results.length == 1);
    // assertTrue(results[0].isMeat());
    // assertTrue(results[0].getCount() == 1000);
  }

  @Test
  public void itShouldParseWildcardItems() {

    // Add stuff to inventory
    addToInventory(ItemPool.SEAL_TOOTH, 1);
    addToInventory(ItemPool.TOILET_PAPER, 3);
    addToInventory(ItemPool.MILK_OF_MAGNESIUM, 1);

    AdventureResult[] results = null;

    // All available (using null list == inventory)
    results = ItemFinder.getMatchingItemList("* toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 3);

    // All but 2
    results = ItemFinder.getMatchingItemList("-2 toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 1);

    // All available (using inventory)
    results = ItemFinder.getMatchingItemList("* toilet paper", KoLConstants.inventory);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 3);

    // All but 2
    results = ItemFinder.getMatchingItemList("-2 toilet paper", KoLConstants.inventory);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 1);

    // *** Closet
    // *** Storage
    // *** FreePulls
  }

  @Test
  public void itShouldParseWildcardMeat() {

    AdventureResult[] results = null;

    // Add meat to inventory
    KoLCharacter.setAvailableMeat(12_000);

    // All available Meat from null list (default inventory)
    results = ItemFinder.getMatchingItemList("* meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 12000);

    // All but 10000 Meat
    results = ItemFinder.getMatchingItemList("-10000 meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 2000);

    // All available Meat from inventory
    results = ItemFinder.getMatchingItemList("* meat", KoLConstants.inventory);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 12000);

    // All but 10000 Meat from inventory
    results = ItemFinder.getMatchingItemList("-10000 meat", KoLConstants.inventory);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 2000);

    // Add meat to closet
    KoLCharacter.setClosetMeat(20_000);

    // All available Meat from closet
    results = ItemFinder.getMatchingItemList("* meat", KoLConstants.closet);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 20000);

    // All but 10000 Meat from closet
    results = ItemFinder.getMatchingItemList("-10000 meat", KoLConstants.closet);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 10000);

    // Add meat to storage
    KoLCharacter.setStorageMeat(1_000_000);

    // All available Meat from storage
    results = ItemFinder.getMatchingItemList("* meat", KoLConstants.storage);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 1000000);

    // All but 10000 Meat from closet
    results = ItemFinder.getMatchingItemList("-10000 meat", KoLConstants.storage);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 990000);

    // There is no Meat in the freepulls list

    // All available Meat from freepulls
    results = ItemFinder.getMatchingItemList("* meat", KoLConstants.freepulls);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 0);
  }

  @Test
  public void itShouldParseMultipleItems() {

    AdventureResult[] results = null;

    // Multiple items, all valid
    results =
        ItemFinder.getMatchingItemList("1 seal tooth, 2 toilet paper, 3 milk of magnesium", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 3);
    assertTrue(results[0].getItemId() == ItemPool.SEAL_TOOTH);
    assertTrue(results[0].getCount() == 1);
    assertTrue(results[1].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[1].getCount() == 2);
    assertTrue(results[2].getItemId() == ItemPool.MILK_OF_MAGNESIUM);
    assertTrue(results[2].getCount() == 3);

    // Multiple items, not all valid
    results =
        ItemFinder.getMatchingItemList("1 seal tooth, 2 bogus items, 3 milk of magnesium", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(results != null);
    assertTrue(results.length == 2);
    assertTrue(results[0].getItemId() == ItemPool.SEAL_TOOTH);
    assertTrue(results[0].getCount() == 1);
    assertTrue(results[1].getItemId() == ItemPool.MILK_OF_MAGNESIUM);
    assertTrue(results[1].getCount() == 3);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Multiple items specified by itemId
    results = ItemFinder.getMatchingItemList("1 ¶1, 2 [2], 3 ¶3, 4 [4]", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 4);
    assertTrue(results[0].getItemId() == 1);
    assertTrue(results[0].getCount() == 1);
    assertTrue(results[1].getItemId() == 2);
    assertTrue(results[1].getCount() == 2);
    assertTrue(results[2].getItemId() == 3);
    assertTrue(results[2].getCount() == 3);
    assertTrue(results[3].getItemId() == 4);
    assertTrue(results[3].getCount() == 4);

    // Multiple by name, but one has commas. This should fail.
    results = ItemFinder.getMatchingItemList("seal tooth, A Crimbo Carol, Ch.1", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.SEAL_TOOTH);
    assertTrue(results[0].getCount() == 1);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  public void itShouldFindItemsInInventory() {
    // Doesn't matter if we are in Ronin or not

    // Add stuff to inventory
    addToInventory(ItemPool.SEAL_TOOTH, 1);
    addToInventory(ItemPool.TOILET_PAPER, 3);
    addToInventory(ItemPool.MILK_OF_MAGNESIUM, 1);

    // Test with list == KoLConstants.inventory. That affects how we treat item counts.

    AdventureResult[] results = null;
  }

  @Test
  public void itShouldFindItemsInCloset() {
    // Doesn't matter if we are in Ronin or not

    // Add stuff to closet
    addToCloset(ItemPool.TOILET_PAPER, 8);
    addToCloset(ItemPool.MILK_OF_MAGNESIUM, 2);

    AdventureResult[] results = null;
  }

  @Test
  public void itShouldFindItemsInStorageInteracting() {
    // Not in Ronin, Not in Hardcore. Pulls are unlimited.
    KoLCharacter.setRonin(false);
    KoLCharacter.setHardcore(false);
    CharPaneRequest.setCanInteract(true);

    // Add stuff to storage
    addToStorage(ItemPool.SEAL_TOOTH, 6);
    addToStorage(ItemPool.TOILET_PAPER, 14);
    addToStorage(ItemPool.MILK_OF_MAGNESIUM, 3);

    AdventureResult[] results = null;
  }

  @Test
  public void itShouldFindItemsInStorageInRonin() {
    // In Ronin, Not in Hardcore. Pulls are limited.
    KoLCharacter.setRonin(true);
    KoLCharacter.setHardcore(false);
    CharPaneRequest.setCanInteract(false);

    // Add stuff to storage
    KoLCharacter.setStorageMeat(1_000_000);
    addToStorage(ItemPool.SEAL_TOOTH, 6);
    addToStorage(ItemPool.MILK_OF_MAGNESIUM, 3);

    // Add stuff to freepulls
    addToFreePulls(ItemPool.TOILET_PAPER, 14);

    AdventureResult[] results = null;
  }

  @Test
  public void itShouldFindItemsInFreepullsInHardcore() {
    // Not in Ronin, In Hardcore. Only Free Pulls. No Meat.
    KoLCharacter.setRonin(false);
    KoLCharacter.setHardcore(true);
    CharPaneRequest.setCanInteract(false);

    // Add stuff to freepulls
    addToFreePulls(ItemPool.TOILET_PAPER, 14);

    AdventureResult[] results = null;
  }
}
