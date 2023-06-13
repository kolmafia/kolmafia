package net.sourceforge.kolmafia.request;

import static internal.helpers.Equipment.assertItem;
import static internal.helpers.Equipment.assertItemUnequip;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class EquipmentRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("EquipmentRequestTest");
  }

  private AdventureResult makeItem(String name) {
    return new AdventureResult(name, 1, false);
  }

  @Test
  public void canParseEquipmentPage() {
    String location = "inventory.php?which=2";
    String responseText = html("request/test_parse_equipment.html");

    var cleanups = new Cleanups(withFamiliar(FamiliarPool.TRICK_TOT));

    try (cleanups) {
      EquipmentRequest.parseEquipment(location, responseText);
      Map<Slot, AdventureResult> equipment = EquipmentManager.currentEquipment();

      assertEquals(equipment.get(Slot.HAT), makeItem("Daylight Shavings Helmet"));
      assertEquals(equipment.get(Slot.WEAPON), makeItem("June cleaver"));
      assertEquals(equipment.get(Slot.OFFHAND), makeItem("Drunkula's wineglass"));
      assertEquals(equipment.get(Slot.CONTAINER), makeItem("vampyric cloake"));
      assertEquals(equipment.get(Slot.SHIRT), makeItem("poncho de azucar"));
      assertEquals(equipment.get(Slot.PANTS), makeItem("purpleheart &quot;pants&quot;"));
      assertEquals(equipment.get(Slot.ACCESSORY1), makeItem("Draftsman's driving gloves"));
      assertEquals(equipment.get(Slot.ACCESSORY2), makeItem("fudgecycle"));
      assertEquals(equipment.get(Slot.ACCESSORY3), makeItem("Counterclockwise Watch"));
      assertEquals(equipment.get(Slot.FAMILIAR), makeItem("li'l unicorn costume"));
    }
  }

  @Nested
  class FolderHolder {
    @Test
    public void canParseFolderHolderPage() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.FOLDER1, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER2, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER3, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER4, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER5, ItemPool.FOLDER_01));

      try (cleanups) {
        String text = html("request/test_folder_holder.html");

        EquipmentRequest.parseFolders(text);

        assertItem(Slot.FOLDER1, "folder (heavy metal)");
        assertItem(Slot.FOLDER2, "folder (tranquil landscape)");
        assertItem(Slot.FOLDER3, "folder (owl)");
        assertItemUnequip(Slot.FOLDER4);
        assertItemUnequip(Slot.FOLDER5);
      }
    }

    @Test
    public void canParseFolderHolderPageWithNoMoreFolders() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.FOLDER1, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER2, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER3, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER4, ItemPool.FOLDER_01),
              withEquipped(Slot.FOLDER5, ItemPool.FOLDER_01));

      try (cleanups) {
        String text = html("request/test_folder_holder_no_more_folders.html");

        EquipmentRequest.parseFolders(text);

        assertItem(Slot.FOLDER1, "folder (heavy metal)");
        assertItem(Slot.FOLDER2, "folder (tranquil landscape)");
        assertItem(Slot.FOLDER3, "folder (owl)");
        assertItemUnequip(Slot.FOLDER4);
        assertItemUnequip(Slot.FOLDER5);
      }
    }
  }
}
