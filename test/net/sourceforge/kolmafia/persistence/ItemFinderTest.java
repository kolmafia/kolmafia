package net.sourceforge.kolmafia.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
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

  // getMatchingItemList uses getFirstMatchingItem
  // getFirstMatchingItem uses getFirstMatchingItemName
  // getFirstMatchingItemName uses getMatchingNames
  // getMatchingNames simply calls ItemDatabase.getMatchingNames

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
  // ("buy", "closet", etc.) with no ambiguity.
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

  // *********************** getFirstMatchingItem ***********************
  //
  // That method assumes its parameter is the name of a single item,
  // possible with a count. Assuming it unambiguously names a single
  // item, it returns an AdventureResult with itemId and count.

  @Test
  public void itShouldParseSingleItem() {

    // This set of tests will use null as the source list. That means
    // that it will not restrict its matches to what is available in a
    // specific list. The only exception is if you use wildcard counts -
    // * or -N - in which case, it assumes inventory. We'll test wildcard
    // counts separately.

    AdventureResult item;
    String message;
    String parameter;

    // Test successful exact match for entire string.
    item = ItemFinder.getFirstMatchingItem("toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(item.getCount() == 1);

    // Test successful exact match where item name starts with a number
    item = ItemFinder.getFirstMatchingItem("1337 7r0uZ0RZ", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.ELITE_TROUSERS);
    assertTrue(item.getCount() == 1);

    // Test successful exact match where item name contains commas
    item = ItemFinder.getFirstMatchingItem("Tea, Earl Grey, Hot", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.EARL_GREY);
    assertTrue(item.getCount() == 1);

    // Test successful exact match for item specified by pilcrow
    item = ItemFinder.getFirstMatchingItem("¶7961", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == 7961);
    assertTrue(item.getCount() == 1);

    // Test for the item whose name contains a pilcrow - &para;
    parameter = "Knob G&Atilde;&para;blin l&Atilde;&sup2;ve potion";
    item = ItemFinder.getFirstMatchingItem(parameter, false, null, Match.ANY);
    assertTrue(item != null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item.getItemId() == ItemPool.BUGGED_POTION);
    assertTrue(item.getCount() == 1);

    // Test successful exact match for item specified by itemId
    item = ItemFinder.getFirstMatchingItem("[7961]", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == 7961);
    assertTrue(item.getCount() == 1);

    // Test successful exact match for item specified by itemId and name
    item = ItemFinder.getFirstMatchingItem("[7961]Staff of Ed", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == 7961);
    assertTrue(item.getCount() == 1);

    // Test successful exact match for item specified by itemId and bogus name
    // *** One could argue this should check that the name agrees with item
    item = ItemFinder.getFirstMatchingItem("[7961]Staff of Edward", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == 7961);
    assertTrue(item.getCount() == 1);

    // Test unambiguous fuzzy matching. Fuzzy matching deserves its own test
    // suite. Elsewhere.
    item = ItemFinder.getFirstMatchingItem("mmj", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.MMJ);
    assertTrue(item.getCount() == 1);
  }

  @Test
  public void itShouldParseSingleItemWithCount() {

    AdventureResult item;
    String message;
    String parameter;

    // count + item name
    item = ItemFinder.getFirstMatchingItem("2 toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(item.getCount() == 2);

    // count + item name starts with a number
    item = ItemFinder.getFirstMatchingItem("2 1337 7r0uZ0RZ", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.ELITE_TROUSERS);
    assertTrue(item.getCount() == 2);

    // count + item name contains commas
    item = ItemFinder.getFirstMatchingItem("2 Tea, Earl Grey, Hot", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.EARL_GREY);
    assertTrue(item.getCount() == 2);

    // count + item specified by pilcrow
    item = ItemFinder.getFirstMatchingItem("2 ¶7961", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == 7961);
    assertTrue(item.getCount() == 2);

    // count + item specified by itemId
    item = ItemFinder.getFirstMatchingItem("2 [7961]", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == 7961);
    assertTrue(item.getCount() == 2);

    // count + item specified by itemId and name
    item = ItemFinder.getFirstMatchingItem("2 [7961]Staff of Ed", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == 7961);
    assertTrue(item.getCount() == 2);

    // Single test for fuzzy matching. As I said, that deserves its own test
    // suite. Elsewhere.
    item = ItemFinder.getFirstMatchingItem("10 mmj", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.MMJ);
    assertTrue(item.getCount() == 10);
  }

  @Test
  public void itShouldParseWildcardItem() {

    AdventureResult item;
    String message;
    String parameter;

    // Add stuff to inventory
    addToInventory(ItemPool.SEAL_TOOTH, 1);
    addToInventory(ItemPool.TOILET_PAPER, 3);
    addToInventory(ItemPool.MILK_OF_MAGNESIUM, 1);

    // All available (using null list == inventory)
    item = ItemFinder.getFirstMatchingItem("* toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(item.getCount() == 3);

    // All but 2
    item = ItemFinder.getFirstMatchingItem("-2 toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(item.getCount() == 1);

    // All available (using inventory)
    item =
        ItemFinder.getFirstMatchingItem("* toilet paper", false, KoLConstants.inventory, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(item.getCount() == 3);

    // All but 2
    item =
        ItemFinder.getFirstMatchingItem(
            "-2 toilet paper", false, KoLConstants.inventory, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(item.getCount() == 1);

    // *** Closet
    // *** Storage
    // *** FreePulls
  }

  @Test
  public void itShouldDetectParameterErrors() {

    AdventureResult item;
    String message;
    String parameter;

    // Test empty string, no error on failure
    item = ItemFinder.getFirstMatchingItem("", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item == null);

    // Test for an empty string, error on failure
    item = ItemFinder.getFirstMatchingItem("", true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "Need to provide an item to match.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test unsuccessful match for bogus string, no error on failure

    item = ItemFinder.getFirstMatchingItem("Bogus", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item == null);

    // Test unsuccessful exact match for bogus string, error on failure

    item = ItemFinder.getFirstMatchingItem("Bogus", true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    assertTrue("[Bogus] has no matches.".equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for bogus item whose name contains a pilcrow - &para;
    parameter = "bogus ¶ item";
    item = ItemFinder.getFirstMatchingItem(parameter, true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "Unknown item " + parameter;
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for item with too many matches
    item = ItemFinder.getFirstMatchingItem("tea", true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[tea] has too many matches.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  public void itShouldDetectItemFilteringFailures() {

    AdventureResult item;
    String message;

    // *** Test items that match each Match type

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.SEAL_TOOTH);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("Hell ramen", true, null, Match.FOOD);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.HELL_RAMEN);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("vesper", true, null, Match.BOOZE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.VESPER);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("moxie weed", true, null, Match.SPLEEN);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.MOXIE_WEED);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("cottage", true, null, Match.USE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.COTTAGE);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("cottage", true, null, Match.CREATE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.COTTAGE);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("cottage", true, null, Match.UNTINKER);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.COTTAGE);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("helmet turtle", true, null, Match.EQUIP);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.HELMET_TURTLE);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("fancy chocolate", true, null, Match.CANDY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.FANCY_CHOCOLATE);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.ABSORB);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.SEAL_TOOTH);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("literal grasshopper", true, null, Match.ROBO);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.LITERAL_GRASSHOPPER);
    assertTrue(item.getCount() == 1);

    item = ItemFinder.getFirstMatchingItem("Hell ramen", true, null, Match.ASDON);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(item != null);
    assertTrue(item.getItemId() == ItemPool.HELL_RAMEN);
    assertTrue(item.getCount() == 1);

    // *** Test items that don't match each Match type

    // Skip Match.ANY

    item = ItemFinder.getFirstMatchingItem("vesper", true, null, Match.FOOD);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[vesper] cannot be eaten.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("Hell ramen", true, null, Match.BOOZE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[Hell ramen] cannot be drunk.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.SPLEEN);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[seal tooth] cannot be chewed.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("stolen accordion", true, null, Match.USE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[stolen accordion] cannot be used.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.ERROR);

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.CREATE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[seal tooth] cannot be created.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.UNTINKER);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[seal tooth] cannot be untinkered.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("Hell ramen", true, null, Match.EQUIP);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[Hell ramen] cannot be equipped.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("alien drugs", true, null, Match.CANDY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[alien drugs] is not candy.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("spooky sapling", true, null, Match.ABSORB);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[spooky sapling] cannot be absorbed.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("pink pony", true, null, Match.ROBO);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[pink pony] cannot be fed.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("lemon", true, null, Match.ASDON);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[lemon] cannot be used as fuel.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  public void itShouldDetectSourceListFailures() {

    // Add stuff to inventory
    addToInventory(ItemPool.SEAL_TOOTH, 1);

    // Add stuff to storage
    addToStorage(ItemPool.MILK_OF_MAGNESIUM, 6);

    // Add stuff to freepulls
    addToFreePulls(ItemPool.TOILET_PAPER, 14);

    AdventureResult item;
    String message;

    // Test for requesting non-free-pull from freepulls
    item =
        ItemFinder.getFirstMatchingItem(
            "milk of magnesium", true, KoLConstants.freepulls, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[milk of magnesium] requested, but it's not a Free Pull";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for requesting too many of an item which is not on a list
    item =
        ItemFinder.getFirstMatchingItem(
            "milk of magnesium", true, KoLConstants.inventory, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[milk of magnesium] requested, but none available.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for requesting too many of an item which is on a list
    item = ItemFinder.getFirstMatchingItem("2 seal tooth", true, KoLConstants.inventory, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertTrue(item == null);
    message = "[2 seal tooth] requested, but only 1 available.";
    assertTrue(message.equals(KoLmafia.lastMessage));
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  // *********************** getMatchingItemList ***********************
  //
  // That method splits its "parameters" argument into multiple potential item
  // names and returns an array of AdventureResult objects.
  //
  // It depends on getFirstMatchingItem to parse individual item names, filter
  // them by match type, require items to be on specific item lists, and so
  // on. Tests for those things are above.

  @Test
  public void itShouldParseSingleItems() {

    // This set of tests will use null as the source list. That means
    // that it will not restrict its matches to what is available in a
    // specific list. The only exception is if you use wildcard counts -
    // * or -N - in which case, it assumes inventory. We'll test wildcard
    // counts separately.

    // Since getFirstMatchingItem will successfully parse a single item, the only thing to test here
    // is that the return result is a list with a single item.

    AdventureResult[] results = null;

    // Test successful exact match for entire string.

    results = ItemFinder.getMatchingItemList("toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 1);
  }

  // Next, test items preceded by a count - a simple integer

  @Test
  public void itShouldParseSingleItemsWithCount() {

    // Now test single items with a count + item

    // Since getFirstMatchingItem will successfully parse a single item, the only thing to test here
    // is that the return result is a list with a single item and the correct count

    AdventureResult[] results = null;

    // Since getFirstMatchingItem will successfully parse a single item, the only thing to test here
    // is that the return result is a list with a single item.

    // count + item name
    results = ItemFinder.getMatchingItemList("2 toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 2);
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

    results = ItemFinder.getMatchingItemList("1000 Meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].isMeat());
    assertTrue(results[0].getCount() == 1000);
  }

  @Test
  public void itShouldParseWildcardItems() {

    // Since getFirstMatchingItem will successfully parse a single item, the only thing to test here
    // is that the return result is a list with a single item.

    // Add stuff to inventory
    addToInventory(ItemPool.TOILET_PAPER, 3);

    AdventureResult[] results = null;

    // All available (using null list == inventory)
    results = ItemFinder.getMatchingItemList("* toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertTrue(results != null);
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.TOILET_PAPER);
    assertTrue(results[0].getCount() == 3);
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
    assertTrue("[bogus items] has no matches.".equals(KoLmafia.lastMessage));
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
    assertTrue("[Ch.1] has too many matches.".equals(KoLmafia.lastMessage));
    assertTrue(results.length == 1);
    assertTrue(results[0].getItemId() == ItemPool.SEAL_TOOTH);
    assertTrue(results[0].getCount() == 1);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }
}
