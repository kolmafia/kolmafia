package net.sourceforge.kolmafia.request;

import static internal.helpers.Equipment.assertItem;
import static internal.helpers.Equipment.assertItemUnequip;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
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
      AdventureResult[] equipment = EquipmentManager.currentEquipment();

      assertEquals(equipment[EquipmentManager.HAT], makeItem("Daylight Shavings Helmet"));
      assertEquals(equipment[EquipmentManager.WEAPON], makeItem("June cleaver"));
      assertEquals(equipment[EquipmentManager.OFFHAND], makeItem("Drunkula's wineglass"));
      assertEquals(equipment[EquipmentManager.CONTAINER], makeItem("vampyric cloake"));
      assertEquals(equipment[EquipmentManager.SHIRT], makeItem("poncho de azucar"));
      assertEquals(equipment[EquipmentManager.PANTS], makeItem("purpleheart &quot;pants&quot;"));
      assertEquals(equipment[EquipmentManager.ACCESSORY1], makeItem("Draftsman's driving gloves"));
      assertEquals(equipment[EquipmentManager.ACCESSORY2], makeItem("fudgecycle"));
      assertEquals(equipment[EquipmentManager.ACCESSORY3], makeItem("Counterclockwise Watch"));
      assertEquals(equipment[EquipmentManager.FAMILIAR], makeItem("li'l unicorn costume"));
    }
  }

  @Nested
  class FolderHolder {
    @Test
    public void canParseFolderHolderPage() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.FOLDER1, ItemPool.FOLDER_01),
              withEquipped(EquipmentManager.FOLDER2, ItemPool.FOLDER_01),
              withEquipped(EquipmentManager.FOLDER3, ItemPool.FOLDER_01),
              withEquipped(EquipmentManager.FOLDER4, ItemPool.FOLDER_01),
              withEquipped(EquipmentManager.FOLDER5, ItemPool.FOLDER_01));

      try (cleanups) {
        String text = html("request/test_folder_holder.html");

        EquipmentRequest.parseFolders(text);

        assertItem(EquipmentManager.FOLDER1, "folder (heavy metal)");
        assertItem(EquipmentManager.FOLDER2, "folder (tranquil landscape)");
        assertItem(EquipmentManager.FOLDER3, "folder (owl)");
        assertItemUnequip(EquipmentManager.FOLDER4);
        assertItemUnequip(EquipmentManager.FOLDER5);
      }
    }

    @Test
    public void canParseFolderHolderPageWithNoMoreFolders() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.FOLDER1, ItemPool.FOLDER_01),
              withEquipped(EquipmentManager.FOLDER2, ItemPool.FOLDER_01),
              withEquipped(EquipmentManager.FOLDER3, ItemPool.FOLDER_01),
              withEquipped(EquipmentManager.FOLDER4, ItemPool.FOLDER_01),
              withEquipped(EquipmentManager.FOLDER5, ItemPool.FOLDER_01));

      try (cleanups) {
        String text = html("request/test_folder_holder_no_more_folders.html");

        EquipmentRequest.parseFolders(text);

        assertItem(EquipmentManager.FOLDER1, "folder (heavy metal)");
        assertItem(EquipmentManager.FOLDER2, "folder (tranquil landscape)");
        assertItem(EquipmentManager.FOLDER3, "folder (owl)");
        assertItemUnequip(EquipmentManager.FOLDER4);
        assertItemUnequip(EquipmentManager.FOLDER5);
      }
    }
  }
}
