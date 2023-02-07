package net.sourceforge.kolmafia.session;

import static internal.helpers.Equipment.assertItem;
import static internal.helpers.Equipment.assertItemUnequip;
import static internal.helpers.Networking.html;
import static internal.helpers.Networking.json;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Coverage driven collection of tests for FightRequest. */
public class EquipmentManagerTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("equipment manager test");
  }

  private static final AdventureResult UNBREAKABLE_UMBRELLA =
      ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA);

  @ParameterizedTest
  @ValueSource(
      strings = {
        "broken",
        "forward-facing",
        "bucket style",
        "pitchfork style",
        "constantly twirling",
        "cocoon"
      })
  public void thatUnbreakableUmbrellaIsRecognized(String style) {
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.OFFHAND, "unbreakable umbrella"),
            withProperty("umbrellaState", style));

    try (cleanups) {
      assertEquals("unbreakable umbrella (" + style + ")", UNBREAKABLE_UMBRELLA.getName());
      assertEquals(
          UNBREAKABLE_UMBRELLA.getItemId(), ItemDatabase.getItemId(UNBREAKABLE_UMBRELLA.getName()));
    }
  }

  private static final AdventureResult JURASSIC_PARKA = ItemPool.get(ItemPool.JURASSIC_PARKA);

  @ParameterizedTest
  @ValueSource(
      strings = {"", "kachungasaur", "dilophosaur", "spikolodon", "ghostasaurus", "pterodactyl"})
  public void thatJurassicParkaIsRecognized(String mode) {
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.SHIRT, "Jurassic Parka"),
            withProperty("parkaMode", mode));

    try (cleanups) {
      if (mode.equals("")) {
        assertEquals("Jurassic Parka", JURASSIC_PARKA.getName());
      } else {
        assertEquals("Jurassic Parka (" + mode + " mode)", JURASSIC_PARKA.getName());
      }
      assertEquals(JURASSIC_PARKA.getItemId(), ItemDatabase.getItemId(JURASSIC_PARKA.getName()));
    }
  }

  private static final AdventureResult BACKUP_CAMERA = ItemPool.get(ItemPool.BACKUP_CAMERA);

  @ParameterizedTest
  @ValueSource(strings = {"meat", "init", "ml"})
  public void thatBackupCameraIsRecognized(String mode) {
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.ACCESSORY1, "backup camera"),
            withProperty("backupCameraMode", mode));

    try (cleanups) {
      assertEquals("backup camera (" + mode + ")", BACKUP_CAMERA.getName());
      assertEquals(BACKUP_CAMERA.getItemId(), ItemDatabase.getItemId(BACKUP_CAMERA.getName()));
    }
  }

  @Test
  public void equippingDesignerSweatpantsGivesCombatSkills() {
    assertThat(KoLCharacter.hasSkill(SkillPool.SWEAT_FLICK), equalTo(false));

    var cleanup =
        new Cleanups(
            withEquipped(EquipmentManager.PANTS, "designer sweatpants"),
            withProperty("sweat", 100));

    try (cleanup) {
      assertThat(KoLCharacter.hasSkill(SkillPool.SWEAT_FLICK), equalTo(true));
    }
  }

  @Test
  public void unequippingDesignerSweatpantsRemovesCombatSkills() {
    var cleanup =
        new Cleanups(
            withEquipped(EquipmentManager.PANTS, "designer sweatpants"),
            withProperty("sweat", 100));

    try (cleanup) {
      assertThat(KoLCharacter.hasSkill(SkillPool.SWEAT_FLICK), equalTo(true));
      EquipmentManager.setEquipment(EquipmentManager.PANTS, EquipmentRequest.UNEQUIP);
      assertThat(KoLCharacter.hasSkill(SkillPool.SWEAT_FLICK), equalTo(false));
    }
  }

  @Test
  public void canParseStatus() {
    var cleanups = withFamiliar(FamiliarPool.TRICK_TOT);

    try (cleanups) {
      String text = html("request/test_status.json");
      JSONObject JSON = json(text);

      EquipmentManager.parseStatus(JSON);

      assertItem(EquipmentManager.HAT, "Daylight Shavings Helmet");
      assertItem(EquipmentManager.CONTAINER, "vampyric cloake");
      assertItem(EquipmentManager.SHIRT, "Sneaky Pete's leather jacket");
      assertItem(EquipmentManager.WEAPON, "cursed pirate cutlass");
      assertItem(EquipmentManager.OFFHAND, "June cleaver");
      assertItem(EquipmentManager.PANTS, "purpleheart \"pants\"");
      assertItem(EquipmentManager.ACCESSORY1, "fudgecycle");
      assertItem(EquipmentManager.ACCESSORY2, "Treads of Loathing");
      assertItem(EquipmentManager.ACCESSORY3, "Counterclockwise Watch");
      assertItem(EquipmentManager.FAMILIAR, "li'l unicorn costume");

      assertItem(EquipmentManager.CARDSLEEVE, "Alice's Army Coward");

      assertItem(EquipmentManager.STICKER1, "scratch 'n' sniff UPC sticker");
      assertItemUnequip(EquipmentManager.STICKER2);
      assertItemUnequip(EquipmentManager.STICKER3);

      assertItem(EquipmentManager.FOLDER1, "folder (heavy metal)");
      assertItem(EquipmentManager.FOLDER2, "folder (tranquil landscape)");
      assertItem(EquipmentManager.FOLDER3, "folder (owl)");
      assertItemUnequip(EquipmentManager.FOLDER4);
      assertItemUnequip(EquipmentManager.FOLDER5);
    }
  }
}
