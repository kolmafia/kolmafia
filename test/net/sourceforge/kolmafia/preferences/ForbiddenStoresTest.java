package net.sourceforge.kolmafia.preferences;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.request.MallPurchaseRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForbiddenStoresTest {

  // These need to be before and after each because leakage has been observed between tests
  // in this class.
  @BeforeEach
  public void initializeCharPrefs() {
    KoLCharacter.reset("fakePrefUser");
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
  public void addAndRemoveForbiddenStore() {
    MallPurchaseRequest.addForbiddenStore(1);

    // 1
    assertEquals("1", Preferences.getString("forbiddenStores"));

    // 1,2
    MallPurchaseRequest.addForbiddenStore(2);
    // 1,2,3
    MallPurchaseRequest.addForbiddenStore(3);
    // 1,2,3
    MallPurchaseRequest.addForbiddenStore(3);

    // A store that doesn't exist
    // 1,2,3
    MallPurchaseRequest.removeForbiddenStore(4);
    // 2,3
    MallPurchaseRequest.removeForbiddenStore(1);

    assertEquals("2,3", Preferences.getString("forbiddenStores"));

    // 2,3
    MallPurchaseRequest.removeForbiddenStore(1);

    assertEquals("2,3", Preferences.getString("forbiddenStores"));
  }

  @Test
  public void addGetForbiddenStoreIds() {
    Integer[] storeIds = {1, 2, 3, 4, 5, 6};

    for (int id : storeIds) {
      MallPurchaseRequest.addForbiddenStore(id);
    }

    assertEquals("1,2,3,4,5,6", Preferences.getString("forbiddenStores"));

    Set<Integer> list = MallPurchaseRequest.getForbiddenStores();

    assertArrayEquals(storeIds, list.toArray(Integer[]::new));
  }

  @Test
  public void illegalForbiddenStoreIds() {
    Preferences.setString("forbiddenStores", "1,2 ,a,,-43,33H, 3, '4',");

    Set<Integer> list = MallPurchaseRequest.getForbiddenStores();

    assertArrayEquals(new Integer[] {1, 2, 3}, list.toArray(Integer[]::new));

    MallPurchaseRequest.addForbiddenStore(4);

    list = MallPurchaseRequest.getForbiddenStores();

    assertArrayEquals(new Integer[] {1, 2, 3, 4}, list.toArray(Integer[]::new));

    MallPurchaseRequest.removeForbiddenStore(2);

    list = MallPurchaseRequest.getForbiddenStores();

    assertArrayEquals(new Integer[] {1, 3, 4}, list.toArray(Integer[]::new));
  }

  @Test
  public void forbiddenMalformedPreferences() {
    Preferences.setString("forbiddenStores", "");

    assertTrue(MallPurchaseRequest.getForbiddenStores().isEmpty());

    Preferences.setString("forbiddenStores", "abc");

    assertTrue(MallPurchaseRequest.getForbiddenStores().isEmpty());

    Preferences.setString("forbiddenStores", ",");

    assertTrue(MallPurchaseRequest.getForbiddenStores().isEmpty());

    Preferences.setString("forbiddenStores", " , ");

    assertTrue(MallPurchaseRequest.getForbiddenStores().isEmpty());

    Preferences.setString("forbiddenStores", " ,,, Player,40+, ");

    assertTrue(MallPurchaseRequest.getForbiddenStores().isEmpty());
  }

  @Test
  public void forbiddenToggle() {
    assertTrue(MallPurchaseRequest.getForbiddenStores().isEmpty());

    // 3
    MallPurchaseRequest.toggleForbiddenStore(3);

    Set<Integer> list = MallPurchaseRequest.getForbiddenStores();

    assertArrayEquals(new Integer[] {3}, list.toArray(Integer[]::new));

    // 3,5
    MallPurchaseRequest.toggleForbiddenStore(5);

    list = MallPurchaseRequest.getForbiddenStores();

    assertArrayEquals(new Integer[] {3, 5}, list.toArray(Integer[]::new));

    // 3
    MallPurchaseRequest.toggleForbiddenStore(5);

    // Compare using string just to prove it didn't save malformed that parses properly
    assertEquals("3", Preferences.getString("forbiddenStores"));

    // 3, 5
    MallPurchaseRequest.toggleForbiddenStore(5);
    // 3, 5
    MallPurchaseRequest.addForbiddenStore(5);

    list = MallPurchaseRequest.getForbiddenStores();

    assertArrayEquals(new Integer[] {3, 5}, list.toArray(Integer[]::new));

    // 3
    MallPurchaseRequest.toggleForbiddenStore(5);

    // Empty String
    MallPurchaseRequest.toggleForbiddenStore(3);

    // Compare using string just to prove it didn't save malformed that parses properly
    assertEquals("", Preferences.getString("forbiddenStores"));
  }

  @Test
  public void forbiddenSwitchUsers() {
    assertEquals("", Preferences.getString("forbiddenStores"));

    MallPurchaseRequest.toggleForbiddenStore(5);

    assertEquals("5", Preferences.getString("forbiddenStores"));

    KoLCharacter.reset("fakePrefUserToo");

    assertEquals("", Preferences.getString("forbiddenStores"));

    // We don't test if exists when we switch back, because we don't save preferences in our tests
  }
}
