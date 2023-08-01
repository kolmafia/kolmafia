package net.sourceforge.kolmafia.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemFinder.Match;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ItemFinderTest {

  @BeforeAll
  public static void itemFinderClassSetup() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("item finder user");
  }

  @BeforeEach
  public void itemFinderSetup() {
    // Reset exactly the preferences used by this package to defaults
    Preferences.setString("_roboDrinks", "");
    Preferences.setBoolean("autoSatisfyWithNPCs", false);

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
    String parameter;

    // Test successful exact match for entire string.
    item = ItemFinder.getFirstMatchingItem("roll of toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 1);

    // Test successful exact match where item name starts with a number
    item = ItemFinder.getFirstMatchingItem("1337 7r0uZ0RZ", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.ELITE_TROUSERS);
    assertEquals(item.getCount(), 1);

    // Test successful exact match where item name contains commas
    item = ItemFinder.getFirstMatchingItem("Tea, Earl Grey, Hot", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.EARL_GREY);
    assertEquals(item.getCount(), 1);

    // Test successful exact match for item specified by pilcrow
    item = ItemFinder.getFirstMatchingItem("¶7961", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), 7961);
    assertEquals(item.getCount(), 1);

    // Test for the item whose name contains a pilcrow - &para;
    parameter =
        StringUtilities.getEntityDecode("Knob G&Atilde;&para;blin l&Atilde;&sup2;ve potion");
    item = ItemFinder.getFirstMatchingItem(parameter, false, null, Match.ANY);
    assertNotNull(item);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertEquals(item.getItemId(), ItemPool.BUGGED_POTION);
    assertEquals(item.getCount(), 1);

    // Test successful exact match for item specified by itemId
    item = ItemFinder.getFirstMatchingItem("[7961]", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), 7961);
    assertEquals(item.getCount(), 1);

    // Test successful exact match for item specified by itemId and name
    item = ItemFinder.getFirstMatchingItem("[7961]Staff of Ed", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), 7961);
    assertEquals(item.getCount(), 1);

    // Test successful exact match for item specified by itemId and bogus name
    // *** One could argue this should check that the name agrees with item
    item = ItemFinder.getFirstMatchingItem("[7961]Staff of Edward", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), 7961);
    assertEquals(item.getCount(), 1);

    // Test unambiguous fuzzy matching. Fuzzy matching deserves its own test
    // suite. Elsewhere.
    item = ItemFinder.getFirstMatchingItem("mmj", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.MMJ);
    assertEquals(item.getCount(), 1);
  }

  @Test
  public void itShouldParseSingleItemWithCount() {

    AdventureResult item;

    // count + item name
    item = ItemFinder.getFirstMatchingItem("2 roll of toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 2);

    // count + item name starts with a number
    item = ItemFinder.getFirstMatchingItem("2 1337 7r0uZ0RZ", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.ELITE_TROUSERS);
    assertEquals(item.getCount(), 2);

    // count + item name contains commas
    item = ItemFinder.getFirstMatchingItem("2 Tea, Earl Grey, Hot", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.EARL_GREY);
    assertEquals(item.getCount(), 2);

    // count + item specified by pilcrow
    item = ItemFinder.getFirstMatchingItem("2 ¶7961", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), 7961);
    assertEquals(item.getCount(), 2);

    // count + item specified by itemId
    item = ItemFinder.getFirstMatchingItem("2 [7961]", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), 7961);
    assertEquals(item.getCount(), 2);

    // count + item specified by itemId and name
    item = ItemFinder.getFirstMatchingItem("2 [7961]Staff of Ed", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), 7961);
    assertEquals(item.getCount(), 2);

    // Single test for fuzzy matching. As I said, that deserves its own test
    // suite. Elsewhere.
    item = ItemFinder.getFirstMatchingItem("10 mmj", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.MMJ);
    assertEquals(item.getCount(), 10);
  }

  @Test
  public void itShouldParseWildcardItem() {

    AdventureResult item;

    // Add stuff to inventory
    addToInventory(ItemPool.SEAL_TOOTH, 1);
    addToInventory(ItemPool.TOILET_PAPER, 3);
    addToInventory(ItemPool.MILK_OF_MAGNESIUM, 1);

    // All available (using null list == inventory)
    item = ItemFinder.getFirstMatchingItem("* roll of toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 3);

    // All but 2
    item = ItemFinder.getFirstMatchingItem("-2 roll of toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 1);

    // Zero
    item = ItemFinder.getFirstMatchingItem("0 roll of toilet paper", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 0);

    // All available (using inventory)
    item =
        ItemFinder.getFirstMatchingItem(
            "* roll of toilet paper", false, KoLConstants.inventory, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 3);

    // All but 2
    item =
        ItemFinder.getFirstMatchingItem(
            "-2 roll of toilet paper", false, KoLConstants.inventory, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 1);
  }

  @Test
  public void itShouldDetectParameterErrors() {

    AdventureResult item;
    String message;
    String parameter;

    // Test empty string, no error on failure
    item = ItemFinder.getFirstMatchingItem("", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNull(item);

    // Test for an empty string, error on failure
    item = ItemFinder.getFirstMatchingItem("", true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "Need to provide an item to match.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test unsuccessful match for bogus string, no error on failure

    item = ItemFinder.getFirstMatchingItem("Bogus", false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNull(item);

    // Test unsuccessful exact match for bogus string, error on failure

    item = ItemFinder.getFirstMatchingItem("Bogus", true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    assertEquals(KoLmafia.lastMessage, "[Bogus] has no matches.");
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for bogus item whose name contains a pilcrow - &para;
    parameter = "bogus ¶ item";
    item = ItemFinder.getFirstMatchingItem(parameter, true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "Unknown item " + parameter;
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for multiple items whose name contain a pilcrow
    parameter = "4 ¶8207, 28 ¶8206, 5 ¶8209";
    item = ItemFinder.getFirstMatchingItem(parameter, true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "More than one item specified by item ID.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for multiple items specified by item ID
    parameter = "4 [8207], 28 [8206], 5 [8209]";
    item = ItemFinder.getFirstMatchingItem(parameter, true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "More than one item specified by item ID.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for item with too many matches
    item = ItemFinder.getFirstMatchingItem("tea", true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[tea] has too many matches.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @Test
  public void itShouldDetectItemFilteringFailures() {

    AdventureResult item;
    String message;

    // *** Test items that match each Match type

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.SEAL_TOOTH);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("Hell ramen", true, null, Match.FOOD);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.HELL_RAMEN);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("vesper", true, null, Match.BOOZE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.VESPER);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("moxie weed", true, null, Match.SPLEEN);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.MOXIE_WEED);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("cottage", true, null, Match.USE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.COTTAGE);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("cottage", true, null, Match.CREATE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.COTTAGE);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("cottage", true, null, Match.UNTINKER);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.COTTAGE);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("helmet turtle", true, null, Match.EQUIP);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.HELMET_TURTLE);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("fancy chocolate", true, null, Match.CANDY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.FANCY_CHOCOLATE);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.ABSORB);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.SEAL_TOOTH);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("literal grasshopper", true, null, Match.ROBO);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.LITERAL_GRASSHOPPER);
    assertEquals(item.getCount(), 1);

    item = ItemFinder.getFirstMatchingItem("Hell ramen", true, null, Match.ASDON);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.HELL_RAMEN);
    assertEquals(item.getCount(), 1);

    // *** Test items that don't match each Match type

    // Skip Match.ANY

    item = ItemFinder.getFirstMatchingItem("vesper", true, null, Match.FOOD);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[vesper] cannot be eaten.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("Hell ramen", true, null, Match.BOOZE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[Hell ramen] cannot be drunk.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.SPLEEN);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[seal tooth] cannot be chewed.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("stolen accordion", true, null, Match.USE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[stolen accordion] cannot be used.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.ERROR);

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.CREATE);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[seal tooth] cannot be created.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("seal tooth", true, null, Match.UNTINKER);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[seal tooth] cannot be untinkered.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("Hell ramen", true, null, Match.EQUIP);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[Hell ramen] cannot be equipped.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("alien drugs", true, null, Match.CANDY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[alien drugs] is not candy.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("spooky sapling", true, null, Match.ABSORB);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[spooky sapling] cannot be absorbed.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("pink pony", true, null, Match.ROBO);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[pink pony] cannot be fed.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    item = ItemFinder.getFirstMatchingItem("lemon", true, null, Match.ASDON);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[lemon] cannot be used as fuel.";
    assertEquals(KoLmafia.lastMessage, message);
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
    assertNull(item);
    message = "[milk of magnesium] requested, but it's not a Free Pull";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for requesting a free pull from freepulLs
    item =
        ItemFinder.getFirstMatchingItem(
            "roll of toilet paper", true, KoLConstants.freepulls, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 1);

    // Test for requesting a free pull from storage
    item =
        ItemFinder.getFirstMatchingItem(
            "roll of toilet paper", true, KoLConstants.storage, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(item.getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(item.getCount(), 1);

    // Test for requesting too many of an item which is not on a list
    item =
        ItemFinder.getFirstMatchingItem(
            "milk of magnesium", true, KoLConstants.inventory, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[milk of magnesium] requested, but none available.";
    assertEquals(KoLmafia.lastMessage, message);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Test for requesting too many of an item which is on a list
    item = ItemFinder.getFirstMatchingItem("2 seal tooth", true, KoLConstants.inventory, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertNull(item);
    message = "[2 seal tooth] requested, but only 1 available.";
    assertEquals(KoLmafia.lastMessage, message);
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

    AdventureResult[] results;

    // Test successful exact match for entire string.

    results = ItemFinder.getMatchingItemList("roll of toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertEquals(results[0].getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(results[0].getCount(), 1);

    // Test failed exact match for entire string.
    results = ItemFinder.getMatchingItemList("bogus items", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertEquals(KoLmafia.lastMessage, "[bogus items] has no matches.");
    assertNotNull(results);
    assertEquals(results.length, 0);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  // Next, test items preceded by a count - a simple integer

  @Test
  public void itShouldParseSingleItemsWithCount() {

    // Now test single items with a count + item

    // Since getFirstMatchingItem will successfully parse a single item, the only thing to test here
    // is that the return result is a list with a single item and the correct count

    AdventureResult[] results;

    // Since getFirstMatchingItem will successfully parse a single item, the only thing to test here
    // is that the return result is a list with a single item.

    // count + item name
    results = ItemFinder.getMatchingItemList("2 roll of toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertEquals(results[0].getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(results[0].getCount(), 2);
  }

  // You can specify meat via "COUNT meat".

  @Test
  public void itShouldParseMeat() {

    AdventureResult[] results;

    // Test that you can get a Meat result
    results = ItemFinder.getMatchingItemList("1000 meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 1000);

    // And the funny special case of the actual item named "1 Meat"
    results = ItemFinder.getMatchingItemList("1 meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertEquals(results[0].getItemId(), ItemPool.ONE_MEAT);
    assertEquals(results[0].getCount(), 1);

    // Meat should be case insensitive

    results = ItemFinder.getMatchingItemList("1000 Meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 1000);
  }

  @Test
  public void itShouldParseWildcardItems() {

    // Since getFirstMatchingItem will successfully parse a single item, the only thing to test here
    // is that the return result is a list with a single item.

    // Add stuff to inventory
    addToInventory(ItemPool.TOILET_PAPER, 3);

    AdventureResult[] results;

    // All available (using null list == inventory)
    results = ItemFinder.getMatchingItemList("* roll of toilet paper", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertEquals(results[0].getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(results[0].getCount(), 3);
  }

  @Test
  public void itShouldParseWildcardMeat() {

    AdventureResult[] results;

    // Add meat to inventory
    KoLCharacter.setAvailableMeat(12_000);

    // All available Meat from null list (default inventory)
    results = ItemFinder.getMatchingItemList("* meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 12000);

    // All but 10000 Meat
    results = ItemFinder.getMatchingItemList("-10000 meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 2000);

    // 0 Meat
    results = ItemFinder.getMatchingItemList("0 meat", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 0);

    // All available Meat from inventory
    results = ItemFinder.getMatchingItemList("* meat", KoLConstants.inventory);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 12000);

    // All but 10000 Meat from inventory
    results = ItemFinder.getMatchingItemList("-10000 meat", KoLConstants.inventory);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 2000);

    // 0 Meat from inventory
    results = ItemFinder.getMatchingItemList("0 meat", KoLConstants.inventory);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 0);

    // Add meat to closet
    KoLCharacter.setClosetMeat(20_000);

    // All available Meat from closet
    results = ItemFinder.getMatchingItemList("* meat", KoLConstants.closet);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 20000);

    // All but 10000 Meat from closet
    results = ItemFinder.getMatchingItemList("-10000 meat", KoLConstants.closet);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 10000);

    // 0 Meat from closet
    results = ItemFinder.getMatchingItemList("0 meat", KoLConstants.closet);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 0);

    // Add meat to storage
    KoLCharacter.setStorageMeat(1_000_000);

    // All available Meat from storage
    results = ItemFinder.getMatchingItemList("* meat", KoLConstants.storage);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 1000000);

    // All but 10000 Meat from storage
    results = ItemFinder.getMatchingItemList("-10000 meat", KoLConstants.storage);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 990000);

    // 0 Meat from storage
    results = ItemFinder.getMatchingItemList("0 meat", KoLConstants.storage);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 1);
    assertTrue(results[0].isMeat());
    assertEquals(results[0].getCount(), 0);

    // There is no Meat in the freepulls list

    // All available Meat from freepulls
    results = ItemFinder.getMatchingItemList("* meat", KoLConstants.freepulls);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 0);
  }

  @Test
  public void itShouldParseMultipleItems() {

    AdventureResult[] results;

    // Multiple items, all valid
    results =
        ItemFinder.getMatchingItemList(
            "1 seal tooth, 2 roll of toilet paper, 3 milk of magnesium", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 3);
    assertEquals(results[0].getItemId(), ItemPool.SEAL_TOOTH);
    assertEquals(results[0].getCount(), 1);
    assertEquals(results[1].getItemId(), ItemPool.TOILET_PAPER);
    assertEquals(results[1].getCount(), 2);
    assertEquals(results[2].getItemId(), ItemPool.MILK_OF_MAGNESIUM);
    assertEquals(results[2].getCount(), 3);

    // Multiple items, not all valid
    results =
        ItemFinder.getMatchingItemList("1 seal tooth, 2 bogus items, 3 milk of magnesium", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertEquals(KoLmafia.lastMessage, "[bogus items] has no matches.");
    assertNotNull(results);
    assertEquals(results.length, 2);
    assertEquals(results[0].getItemId(), ItemPool.SEAL_TOOTH);
    assertEquals(results[0].getCount(), 1);
    assertEquals(results[1].getItemId(), ItemPool.MILK_OF_MAGNESIUM);
    assertEquals(results[1].getCount(), 3);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);

    // Multiple items specified by itemId
    results = ItemFinder.getMatchingItemList("1 ¶1, 2 [2], 3 ¶3, 4 [4]", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(results);
    assertEquals(results.length, 4);
    assertEquals(results[0].getItemId(), 1);
    assertEquals(results[0].getCount(), 1);
    assertEquals(results[1].getItemId(), 2);
    assertEquals(results[1].getCount(), 2);
    assertEquals(results[2].getItemId(), 3);
    assertEquals(results[2].getCount(), 3);
    assertEquals(results[3].getItemId(), 4);
    assertEquals(results[3].getCount(), 4);

    // Multiple by name, but one has commas. This should fail.
    results = ItemFinder.getMatchingItemList("seal tooth, A Crimbo Carol, Ch.1", null);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.ERROR);
    assertEquals(KoLmafia.lastMessage, "[Ch.1] has too many matches.");
    assertEquals(results.length, 1);
    assertEquals(results[0].getItemId(), ItemPool.SEAL_TOOTH);
    assertEquals(results[0].getCount(), 1);
    StaticEntity.setContinuationState(MafiaState.CONTINUE);
  }

  @ParameterizedTest
  @CsvSource({
    "'1 1337 7r0uZ0RZ'," + ItemPool.ELITE_TROUSERS + ",1",
    // space in name that begins with digit, 1 assumed for quantity
    "'1337 7r0uZ0RZ'," + ItemPool.ELITE_TROUSERS + ",1",
    // See https://wiki.kolmafia.us/index.php/CLI_Reference 1 WA fuzzy matched to 100-watt light
    // bulb
    "'1 WA'," + ItemPool.WA + ",1",
    "'1 Meat'," + ItemPool.ONE_MEAT + ",1",
    "'1 1 Meat'," + ItemPool.ONE_MEAT + ",1",
    "'123 1 Meat'," + ItemPool.ONE_MEAT + ",123",
    // Initial bug was 1 7-ball being returned as 17-ball
    "'1 7-ball'," + ItemPool.SEVEN_BALL + ",1"
  })
  public void parseStringAndFindItemAndQuantity(
      String toBeParsed, int expectedItemId, int expectedQuantity) {
    AdventureResult item;
    item = ItemFinder.getFirstMatchingItem(toBeParsed, false, null, Match.ANY);
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNotNull(item);
    assertEquals(expectedItemId, item.getItemId());
    assertEquals(expectedQuantity, item.getCount());
  }

  // Tests written for the sole purpose of 100% coverage
  @Test
  public void itShouldReturnNullWhenPassedNull() {
    String item;
    item = ItemFinder.getFirstMatchingItemName(null, "7-ball");
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNull(item);
  }

  @Test
  public void itShouldReturnNullWhenPassedEmptyList() {
    List<String> nameList = new ArrayList<>();
    String item;
    item = ItemFinder.getFirstMatchingItemName(nameList, "7-ball");
    assertEquals(StaticEntity.getContinuationState(), MafiaState.CONTINUE);
    assertNull(item);
  }
}
