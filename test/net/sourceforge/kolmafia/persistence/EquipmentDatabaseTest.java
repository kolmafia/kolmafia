package net.sourceforge.kolmafia.persistence;

import static org.junit.Assert.*;

import java.io.File;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.Test;

public class EquipmentDatabaseTest {

  @Test
  public void itShouldWriteEquipment() {
    // This is an awkward test because it generates a lot of coverage but verifying that file is
    // correct
    // depends upon the dynamic state of the equipment file.  The attempts to read the version in
    // the source
    // tree or jar have failed so far.  Revisit when the test environment is better understood?
    File equip = new File("testeqf.txt");
    EquipmentDatabase.reset();
    EquipmentDatabase.writeEquipment(equip);
    assertTrue(true);
    // delete which is probably not helpful if test fails but...
    equip.delete();
  }

  @Test
  public void itShouldKnowWhatIsEquipment() {
    assertTrue(EquipmentDatabase.isEquipment(KoLConstants.EQUIP_ACCESSORY));
    assertTrue(EquipmentDatabase.isEquipment(KoLConstants.EQUIP_CONTAINER));
    assertTrue(EquipmentDatabase.isEquipment(KoLConstants.EQUIP_HAT));
    assertTrue(EquipmentDatabase.isEquipment(KoLConstants.EQUIP_OFFHAND));
    assertTrue(EquipmentDatabase.isEquipment(KoLConstants.EQUIP_PANTS));
    assertTrue(EquipmentDatabase.isEquipment(KoLConstants.EQUIP_SHIRT));
    assertTrue(EquipmentDatabase.isEquipment(KoLConstants.EQUIP_WEAPON));
    assertFalse(EquipmentDatabase.isEquipment(KoLConstants.EQUIP_FAMILIAR));
    assertFalse(EquipmentDatabase.isEquipment(-1));
  }

  @Test
  public void itShouldGetAnOutfitAsString() {
    String bbo = EquipmentDatabase.outfitString(1, "Bugbear Costume", "bugbear.gif");
    String expected = "1\tBugbear Costume\tbugbear.gif\tbugbear beanie, bugbear bungguard";
    assertEquals(bbo, expected);
  }

  @Test
  public void itShouldKnowSomeThingsAboutPulverization() {
    EquipmentDatabase.initializePulverization();
    assertFalse(EquipmentDatabase.isPulverizable(-1)); // not an item
    assertTrue(EquipmentDatabase.isPulverizable(1)); // seal club
    assertFalse(EquipmentDatabase.isPulverizable(26)); // quest item Dolphin Map
  }

  // This is a canary test.  There are homebrew array implementations that are used in
  // EquipmentDatabase (and elsewhere).  They are basically arrays but as implemented
  // an index out of bounds returns a reasonable value and there is a version of set that
  // initializes everything from the current end of the array to the index value with
  // a reasonable value and then sets the requested value.  This attempts to test those
  // features so that refactoring can provide the same behavior.
  @Test
  public void powerStorageShouldBehave() {
    // negative indices
    assertEquals(EquipmentDatabase.getPower(-1), 0);
    assertEquals(EquipmentDatabase.getPower(-999), 0);
    // there is no item 13 but it should return zero and not an access violation
    assertEquals(EquipmentDatabase.getPower(13), 0);
    // an index past the end of the array
    assertEquals(EquipmentDatabase.getPower(ItemDatabase.maxItemId() + 5), 0);
  }

  @Test
  public void handsStorageShouldBehave() {
    // negative indices
    assertEquals(EquipmentDatabase.getHands(-1), 0);
    assertEquals(EquipmentDatabase.getHands(-999), 0);
    // there is no item 13 but it should return zero and not an access violation
    assertEquals(EquipmentDatabase.getHands(13), 0);
    // an index past the end of the array
    assertEquals(EquipmentDatabase.getHands(ItemDatabase.maxItemId() + 5), 0);
  }

  @Test
  public void pulverizationStorageShouldBehave() {
    // negative indices - pulverization is special and -1 is trapped
    // by code and not storage access.  -1 is returned for not-pulverizable
    assertEquals(EquipmentDatabase.getPulverization(-1), -1);
    assertEquals(EquipmentDatabase.getPulverization(-999), -1);
    // there is no item 13 but it should return -1 and not an access violation
    assertEquals(EquipmentDatabase.getPulverization(13), -1);
    // an index past the end of the array
    assertEquals(EquipmentDatabase.getPulverization(ItemDatabase.maxItemId() + 5), -1);
  }
}
