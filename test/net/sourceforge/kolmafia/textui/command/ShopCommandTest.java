package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Player;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.StoreManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShopCommandTest extends AbstractCommandTestBase {

  private static final String LS = System.lineSeparator();

  public ShopCommandTest() {
    this.command = "shop";
  }

  @BeforeEach
  public void initializeState() {
    KoLCharacter.reset("shoppy");
    KoLCharacter.reset(true);
    Preferences.saveSettingsToFile = false;
    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  @AfterEach
  public void resetCharAndPrefs() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
    Preferences.saveSettingsToFile = false;
  }

  @Test
  public void itShouldIdentifyBadOrIncompleteCommands() {
    String output = execute("");
    String expected = "Invalid shop command." + LS;
    assertEquals(expected, output, "Unexpected results.");
    output = execute("xyzzy");
    assertEquals(expected, output, "Unexpected results.");
    output = execute("put ");
    expected = "Need to provide an item to match." + LS + "Skipping ''." + LS;
    assertEquals(expected, output, "Unexpected results.");
    output = execute("take ");
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "Need to provide an item to match."
            + LS
            + "Skipping ''."
            + LS;
    assertEquals(expected, output, "Unexpected results.");
    output = execute("reprice ");
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "'' is ambiguous."
            + LS
            + "Please check commas and/or resubmit with one item per command."
            + LS;

    assertEquals(expected, output, "Unexpected results.");
  }

  @Test
  public void itShouldPutAnItem() {
    int itemID = ItemPool.GUITAR_4D;
    String itemName = ItemDatabase.getItemDataName(itemID);
    String output = execute("put " + itemName);
    String expected = "Skipping '4-dimensional guitar', none found in inventory." + LS;
    assertEquals(expected, output, "Item not in inventory.");
    Player.addItem(itemID);
    output = execute("put " + itemName);
    expected = "Transferring items to store..." + LS + "Requests complete." + LS + LS;
    assertEquals(expected, output, "Unexpected results.");
  }

  @Test
  public void itShouldPutAnItemFromStorage() {
    int itemID = ItemPool.GUITAR_4D;
    String itemName = ItemDatabase.getItemDataName(itemID);
    String output = execute("put using storage " + itemName);
    String expected = "Skipping '4-dimensional guitar', none found in storage." + LS;
    assertEquals(expected, output, "Item not in storage.");
    KoLConstants.storage.add(ItemPool.get(itemID));
    output = execute("put using storage " + itemName);
    expected = "Adding 4-dimensional guitar to store..." + LS + "Requests complete." + LS + LS;
    assertEquals(expected, output, "Item not transferred.");
  }

  @Test
  public void itShouldPutItemWithPriceAndLimit() {
    int itemID = ItemPool.GUITAR_4D;
    String itemName = ItemDatabase.getItemDataName(itemID);
    Player.addItem(itemID, 3);
    String output = execute("put " + itemName + "@ 1337");
    String expected = "Transferring items to store..." + LS + "Requests complete." + LS + LS;
    assertEquals(expected, output, "Item not put.");
    output = execute("put " + itemName + "@ 1337 limit 2");
    expected = "Transferring items to store..." + LS + "Requests complete." + LS + LS;
    assertEquals(expected, output, "Item not put.");
  }

  @Test
  public void itShouldCatchACommonUserTypo() {
    int itemID = ItemPool.GUITAR_4D;
    String itemName = ItemDatabase.getItemDataName(itemID);
    Player.addItem(itemID, 3);
    String output = execute("put " + itemName + "@ 1,337");
    String expected =
        "'337' is not an item.  Did you use a comma in the middle of a number?  Quitting..." + LS;
    assertEquals(expected, output, "Item not put.");
  }

  @Test
  public void itShouldTakeItem() {
    int itemID = ItemPool.GUITAR_4D;
    String itemName = ItemDatabase.getItemDataName(itemID);
    String output = execute("take 2 " + itemName);
    String expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "4-dimensional guitar not found in shop."
            + LS;
    assertEquals(expected, output, "Items not taken.");
    StoreManager.addItem(itemID, 5, 1337, 1);
    output = execute("take 2 " + itemName);
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "Removing 4-dimensional guitar from store..."
            + LS
            + "2 4-dimensional guitar removed from your store."
            + LS;
    assertEquals(expected, output, "Items not taken.");
    StoreManager.clearCache();
    StoreManager.addItem(itemID, 5, 1337, 1);
    output = execute("take all " + itemName);
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "Removing 4-dimensional guitar from store..."
            + LS
            + "5 4-dimensional guitar removed from your store."
            + LS;
    assertEquals(expected, output, "Items not taken.");
  }

  @Test
  public void itShouldReprice() {
    int itemID = ItemPool.GUITAR_4D;
    String itemName = ItemDatabase.getItemDataName(itemID);
    Player.addItem(itemID, 1);
    StoreManager.addItem(itemID, 1, 999999999, 1);
    String output = execute("reprice " + itemName + " @ 1337 limit 2");
    String expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "Updating store prices..."
            + LS
            + "Store prices updated."
            + LS;
    assertEquals(expected, output, "Item not repriced.");
    output = execute("reprice " + "xyzzy" + " @ 1337");
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "[xyzzy] has no matches."
            + LS
            + "Skipping 'xyzzy '."
            + LS;
    assertEquals(expected, output, "Unreal item repriced.");
    StoreManager.clearCache();
    itemID = ItemPool.D20;
    itemName = ItemDatabase.getItemDataName(itemID);
    output = execute("reprice " + itemName + " @ 1337");
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "d20  not found in shop."
            + LS;
    assertEquals(expected, output, "Absent item repriced.");
  }

  @Test
  public void itShouldDetectErrorAndBail() {
    int itemID = ItemPool.GUITAR_4D;
    String itemName = ItemDatabase.getItemDataName(itemID);
    StoreManager.addItem(itemID, 1, 999999999, 1);
    String output = execute("reprice " + itemName + " @ 1,337 limit 2");
    String expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "'4-dimensional guitar @ 1,337 limit 2' is ambiguous."
            + LS
            + "Please check commas and/or resubmit with one item per command."
            + LS;
    assertEquals(expected, output, "Command not parsed.");
    int item2 = ItemPool.D20;
    String itemName2 = ItemDatabase.getItemDataName(item2);
    StoreManager.addItem(item2, 1, 999999999, 1);
    output = execute("reprice " + itemName + " @ 1,337 limit 2, " + itemName2 + " @ 7331");
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "'4-dimensional guitar @ 1,337 limit 2, d20 @ 7331' is ambiguous."
            + LS
            + "Please check commas and/or resubmit with one item per command."
            + LS;
    assertEquals(expected, output, "Command not parsed.");
    output = execute("reprice " + itemName2 + " @ 1337 limit 2, " + itemName + " @ 7,331");
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "'d20 @ 1337 limit 2, 4-dimensional guitar @ 7,331' is ambiguous."
            + LS
            + "Please check commas and/or resubmit with one item per command."
            + LS;
    assertEquals(expected, output, "Command not parsed.");
    output = execute("reprice " + itemName + " @ 1337 limit 2, " + itemName2 + " @ 7331");
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "Updating store prices..."
            + LS
            + "Store prices updated."
            + LS;
    assertEquals(expected, output, "Command not parsed.");
    output = execute("reprice " + itemName + " ");
    expected =
        "Requesting store inventory..."
            + LS
            + "Store inventory request complete."
            + LS
            + "'4-dimensional guitar' is ambiguous."
            + LS
            + "Please check commas and/or resubmit with one item per command."
            + LS;
    assertEquals(expected, output, "Command not parsed.");
  }
}
