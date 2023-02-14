package internal.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class Equipment {
  public static void assertItem(Slot slot, String itemName) {
    assertEquals(
        AdventureResult.tallyItem(StringUtilities.getEntityEncode(itemName)),
        EquipmentManager.getEquipment(slot));
  }

  public static void assertItemUnequip(Slot slot) {
    assertEquals(EquipmentRequest.UNEQUIP, EquipmentManager.getEquipment(slot));
  }
}
