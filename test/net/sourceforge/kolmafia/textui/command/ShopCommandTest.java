package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Player;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
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
            + "Skipping '', no price provided"
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
    // Better way to convert item id or name to AdventureResult?
    KoLConstants.storage.add(ItemFinder.getFirstMatchingItem(itemName));
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
}
