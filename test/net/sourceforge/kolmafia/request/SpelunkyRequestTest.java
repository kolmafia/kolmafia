package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpelunkyRequestTest {
  @BeforeEach
  public void initializeCharPrefs() {
    KoLCharacter.reset("lunkhead");
    KoLCharacter.reset(true);
    Preferences.saveSettingsToFile = false;
  }

  @AfterEach
  public void resetCharAndPrefs() {
    KoLCharacter.reset("");
    KoLCharacter.reset(true);
    KoLCharacter.setUserId(0);
    Preferences.saveSettingsToFile = false;
  }

  @Test
  public void itShouldGetImageNames() {
    // Not a spelunky adventure
    KoLAdventure adventure = AdventureDatabase.getAdventureByName("Summoning Chamber");
    assertNull(SpelunkyRequest.adventureImage(adventure), "Not a spelunky location");
    // Spelunky adventure
    adventure = AdventureDatabase.getAdventureByName("Hell");
    assertEquals(
        "otherimages/spelunky/heckofirezzz.gif",
        SpelunkyRequest.adventureImage(adventure),
        "Not the expected image.");
  }

  @Test
  public void itShouldResetSomeItemsAndPreferences() {
    // Set some preferences
    Preferences.setInteger("spelunkyNextNoncombat", 12345);
    Preferences.setInteger("spelunkySacrifices", 12345);
    Preferences.setString("spelunkyStatus", "STATUS");
    Preferences.setInteger("spelunkyWinCount", -1);
    // Add some items
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.SPELUNKY_BOOMERANG));
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.SPELUNKY_COFFEE_CUP));
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.SPELUNKY_FEDORA));
    AdventureResult item = ItemPool.get(ItemPool.SPELUNKY_BOOMERANG);
    assertEquals(1, item.getCount(KoLConstants.inventory), "Unexpected item count");
    item = ItemPool.get(ItemPool.SPELUNKY_COFFEE_CUP);
    assertEquals(1, item.getCount(KoLConstants.inventory), "Unexpected item count");
    item = ItemPool.get(ItemPool.SPELUNKY_FEDORA);
    assertEquals(1, item.getCount(KoLConstants.inventory), "Unexpected item count");
    SpelunkyRequest.reset();
    assertEquals(
        Preferences.getDefault("spelunkyNextNoncombat"),
        Preferences.getString("spelunkyNextNoncombat"));
    assertEquals(
        Preferences.getDefault("spelunkySacrifices"), Preferences.getString("spelunkySacrifices"));
    assertEquals(Preferences.getDefault("spelunkyStatus"), Preferences.getString("spelunkyStatus"));
    assertEquals(
        Preferences.getDefault("spelunkyWinCount"), Preferences.getString("spelunkyWinCount"));
    item = ItemPool.get(ItemPool.SPELUNKY_BOOMERANG);
    assertEquals(0, item.getCount(KoLConstants.inventory), "Unexpected item count after reset");
    item = ItemPool.get(ItemPool.SPELUNKY_COFFEE_CUP);
    assertEquals(0, item.getCount(KoLConstants.inventory), "Unexpected item count after reset");
    item = ItemPool.get(ItemPool.SPELUNKY_FEDORA);
    assertEquals(0, item.getCount(KoLConstants.inventory), "Unexpected item count after reset");
  }

  @Test
  public void itShouldExerciseSomeRoutinesWithNoData() {
    // Just make an object for coverage
    SpelunkyRequest req = new SpelunkyRequest();
    // For coverage
    SpelunkyRequest.parseCharpane("Not a response");
    // There is no buddy or image
    assertNull(SpelunkyRequest.getBuddyName(), "Should not be a buddy");
    assertNull(SpelunkyRequest.getBuddyImageName(), "Should not be a buddy image");
    assertNull(SpelunkyRequest.getBuddyRole(), "Should not be a buddy role");
    // No gold or other things
    assertEquals(0, SpelunkyRequest.getGold(), "Should not have gold");
    assertEquals(0, SpelunkyRequest.getBomb(), "Should not have bomb");
    assertEquals(0, SpelunkyRequest.getKey(), "Should not have keys");
    assertEquals(0, SpelunkyRequest.getTurnsLeft(), "Should not have turns left");
    assertEquals(0, SpelunkyRequest.getRope(), "Should not have rope");
  }
}
