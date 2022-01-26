package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
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
  public void itShouldNotindPulledItemsOutOfRonin() {
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
}
