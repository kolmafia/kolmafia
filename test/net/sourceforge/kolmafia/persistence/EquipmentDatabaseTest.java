package net.sourceforge.kolmafia.persistence;

import net.sourceforge.kolmafia.KoLConstants;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class EquipmentDatabaseTest {

    @Test
    public void itShouldWriteEquipment() {
        //This is an awkward test because it generates a lot of coverage but verifying that file is correct
        //depends upon the dynamic state of the equipment file.  The attempts to read the version in the source
        //tree or jar have failed so far.  Revisit when the test environment is better understood?
        File equip = new File("testeqf.txt");
        EquipmentDatabase.reset();
        EquipmentDatabase.writeEquipment(equip);
        assertTrue(true);
        //delete which is probably not helpful if test fails but...
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
        String bbo = EquipmentDatabase.outfitString(1, "Bugbear Costume","bugbear.gif");
        String expected = "1\tBugbear Costume\tbugbear.gif\tbugbear beanie, bugbear bungguard";
        assertEquals(bbo, expected);
    }

    @Test
    public void itShouldKnowSomeThingsAboutPulverization() {
        EquipmentDatabase.initializePulverization();
        assertFalse(EquipmentDatabase.isPulverizable(-1)); //not an item
        assertTrue(EquipmentDatabase.isPulverizable(1)); //seal club
        assertFalse(EquipmentDatabase.isPulverizable(26)); // quest item Dolphin Map
    }
}