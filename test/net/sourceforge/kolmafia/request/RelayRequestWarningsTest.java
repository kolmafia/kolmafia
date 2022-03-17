package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RelayRequestWarningsTest {

  // The Relay Browser will show "warnings" if you might be doing something unintentionally.
  //
  // There are two types:
  //
  // A "General" warning gives you no options; it tells you about something you are about to do and
  // you can either click the (single) icon to proceed, or you can go do someting (possibly in the
  // GUI) and THEN click the icon.
  //
  // An "Optional" warning gives you such an icon and also at one or more additional icons to go
  // somewhere else or perform some action in-line before continuing.
  //
  // The internal mechanism for the "go ahead anyway" icon is to add a "confirm" field to the
  // original URL. If that field is detected in the resubmitted URL, you have already confiremd.
  //
  // Additionally, some locations will be repeatedly adventured in and, once you have confirmed, you
  // don't want to be nagged again. There are global static booleans in RelayRequest for such
  // warnings. Once you have confirmed once, it holds for rest of the session.

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("relay request warnings user");
    Preferences.saveSettingsToFile = false;
  }

  @BeforeEach
  private void beforeEach() {
    RelayRequest.ignoreMacheteWarning = false;
    RelayRequest.ignoreMohawkWigWarning = false;
    KoLConstants.inventory.clear();
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
    RelayRequest.ignoreMacheteWarning = false;
    RelayRequest.ignoreMohawkWigWarning = false;
  }

  private String adventureURL(int adventureId, String confirmation) {
    StringBuilder buf = new StringBuilder();
    buf.append("adventure.php?snarfblat=");
    buf.append(adventureId);
    if (confirmation != null) {
      buf.append("&");
      buf.append(confirmation);
      buf.append("=on");
    }
    return buf.toString();
  }

  @Test
  public void thatMohawkWigWarningWorks() {
    RelayRequest request = new RelayRequest(false);

    // No warning needed you are not in the Castle Top Floor
    request.constructURLString(adventureURL(AdventurePool.ABOO_PEAK, null), true);
    assertFalse(request.sendMohawkWigWarning());

    // No warning needed if this a resubmission of the "go ahead" URL
    request.constructURLString(
        adventureURL(AdventurePool.CASTLE_TOP, RelayRequest.CONFIRM_MOHAWK_WIG), true);
    assertFalse(request.sendMohawkWigWarning());
    assertTrue(RelayRequest.ignoreMohawkWigWarning);

    // No warning needed if we already said to ignore warning
    request.constructURLString(adventureURL(AdventurePool.CASTLE_TOP, null), true);
    assertFalse(request.sendMohawkWigWarning());
    RelayRequest.ignoreMohawkWigWarning = false;

    // No warning if we don't have a Mohawk wig in inventory
    assertFalse(request.sendMohawkWigWarning());

    // No warning if we have a Mohawk wig equipped
    int slot = EquipmentManager.HAT;
    AdventureResult wig = ItemPool.get(ItemPool.MOHAWK_WIG);
    EquipmentManager.setEquipment(slot, wig);
    assertFalse(request.sendMohawkWigWarning());
    EquipmentManager.setEquipment(slot, EquipmentRequest.UNEQUIP);

    // Put a Mohawk wig into inventory
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.MOHAWK_WIG));

    // No warning if we have already completed the castle quest
    QuestDatabase.setQuestIfBetter(Quest.GARBAGE, "step10");
    assertFalse(request.sendMohawkWigWarning());
    QuestDatabase.setQuestProgress(Quest.GARBAGE, "step9");

    // At this point, we have the wig in inventory, have not completed the
    // castle quest, are adventuring on the top floor of the castle, and have
    // not previously told KoLmafia to stop nagging.

    // Set Base Moxie to 50 and Buffed Moxie to 60
    long base = KoLCharacter.calculatePointSubpoints(50);
    long buffed = KoLCharacter.calculatePointSubpoints(60);
    KoLCharacter.setStatPoints(0, 0, 0, 0, (int) buffed, (int) base);
    assertEquals(50, KoLCharacter.getBaseMoxie());

    // We expect a warning.
    KoLCharacter.setPath(Path.NONE);
    assertTrue(request.sendMohawkWigWarning());
    String expected =
        "You are about to adventure without your Mohawk Wig in the Castle."
            + " It requires base Moxie of 55, but yours is only 50."
            + " If you are sure you wish to adventure without it, click the icon to adventure.";
    assertEquals(expected, request.lastWarning);

    // Try it in You, Robot
    KoLCharacter.setPath(Path.YOU_ROBOT);
    assertTrue(request.sendMohawkWigWarning());
    expected =
        "You are about to adventure without your Mohawk Wig in the Castle."
            + " It requires base Moxie of 55, but yours is only 50."
            + " Perhaps it is time to visit Statbot 5000."
            + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
            + " If you want to visit the Scrapheap, click the icon on the right.";
    assertEquals(expected, request.lastWarning);

    // Set your stats to make the item equippable
    base = KoLCharacter.calculatePointSubpoints(55);
    KoLCharacter.setStatPoints(0, 0, 0, 0, (int) buffed, (int) base);
    assertEquals(55, KoLCharacter.getBaseMoxie());

    // Try again with no path
    KoLCharacter.setPath(Path.NONE);
    assertTrue(request.sendMohawkWigWarning());
    expected =
        "You are about to adventure without your Mohawk Wig in the Castle."
            + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
            + " If you want to put the hat on first, click the icon on the right.";
    assertEquals(expected, request.lastWarning);

    // One last time with You, Robot
    KoLCharacter.setPath(Path.YOU_ROBOT);
    assertTrue(request.sendMohawkWigWarning());
    expected =
        "You are about to adventure without your Mohawk Wig in the Castle."
            + " You need to attach a Mannequin Head in order to wear a hat."
            + " If you are sure you wish to adventure without it, click the icon on the left to adventure."
            + " If you want to visit the Scrapheap, click the icon on the right.";
    assertEquals(expected, request.lastWarning);
  }
}
