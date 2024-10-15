package net.sourceforge.kolmafia.session;

import static internal.helpers.Equipment.assertItem;
import static internal.helpers.Equipment.assertItemUnequip;
import static internal.helpers.Networking.html;
import static internal.helpers.Networking.json;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withIntrinsicEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.fastjson2.JSONObject;
import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
            withEquipped(Slot.OFFHAND, "unbreakable umbrella"),
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
        new Cleanups(withEquipped(Slot.SHIRT, "Jurassic Parka"), withProperty("parkaMode", mode));

    try (cleanups) {
      if (mode.equals("")) {
        assertEquals("Jurassic Parka", JURASSIC_PARKA.getName());
      } else {
        assertEquals("Jurassic Parka (" + mode + " mode)", JURASSIC_PARKA.getName());
      }
      assertEquals(JURASSIC_PARKA.getItemId(), ItemDatabase.getItemId(JURASSIC_PARKA.getName()));
    }
  }

  private static final AdventureResult REPLICA_JURASSIC_PARKA =
      ItemPool.get(ItemPool.REPLICA_JURASSIC_PARKA);

  @ParameterizedTest
  @ValueSource(
      strings = {"", "kachungasaur", "dilophosaur", "spikolodon", "ghostasaurus", "pterodactyl"})
  public void thatReplicaJurassicParkaIsRecognized(String mode) {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.SHIRT, "replica Jurassic Parka"), withProperty("parkaMode", mode));

    try (cleanups) {
      if (mode.equals("")) {
        assertEquals("replica Jurassic Parka", REPLICA_JURASSIC_PARKA.getName());
      } else {
        assertEquals(
            "replica Jurassic Parka (" + mode + " mode)", REPLICA_JURASSIC_PARKA.getName());
      }
      assertEquals(
          REPLICA_JURASSIC_PARKA.getItemId(),
          ItemDatabase.getItemId(REPLICA_JURASSIC_PARKA.getName()));
    }
  }

  private static final AdventureResult BACKUP_CAMERA = ItemPool.get(ItemPool.BACKUP_CAMERA);

  @ParameterizedTest
  @ValueSource(strings = {"meat", "init", "ml"})
  public void thatBackupCameraIsRecognized(String mode) {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, "backup camera"), withProperty("backupCameraMode", mode));

    try (cleanups) {
      assertEquals("backup camera (" + mode + ")", BACKUP_CAMERA.getName());
      assertEquals(BACKUP_CAMERA.getItemId(), ItemDatabase.getItemId(BACKUP_CAMERA.getName()));
    }
  }

  @Test
  public void equippingDesignerSweatpantsGivesCombatSkills() {
    assertThat(KoLCharacter.hasSkill(SkillPool.SWEAT_FLICK), equalTo(false));

    var cleanup =
        new Cleanups(withEquipped(Slot.PANTS, "designer sweatpants"), withProperty("sweat", 100));

    try (cleanup) {
      assertThat(KoLCharacter.hasSkill(SkillPool.SWEAT_FLICK), equalTo(true));
    }
  }

  @Test
  public void unequippingDesignerSweatpantsRemovesCombatSkills() {
    var cleanup =
        new Cleanups(withEquipped(Slot.PANTS, "designer sweatpants"), withProperty("sweat", 100));

    try (cleanup) {
      assertThat(KoLCharacter.hasSkill(SkillPool.SWEAT_FLICK), equalTo(true));
      EquipmentManager.setEquipment(Slot.PANTS, EquipmentRequest.UNEQUIP);
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

      assertItem(Slot.HAT, "Daylight Shavings Helmet");
      assertItem(Slot.CONTAINER, "vampyric cloake");
      assertItem(Slot.SHIRT, "Sneaky Pete's leather jacket");
      assertItem(Slot.WEAPON, "cursed pirate cutlass");
      assertItem(Slot.OFFHAND, "June cleaver");
      assertItem(Slot.PANTS, "purpleheart \"pants\"");
      assertItem(Slot.ACCESSORY1, "fudgecycle");
      assertItem(Slot.ACCESSORY2, "Treads of Loathing");
      assertItem(Slot.ACCESSORY3, "Counterclockwise Watch");
      assertItem(Slot.FAMILIAR, "li'l unicorn costume");

      assertItem(Slot.CARDSLEEVE, "Alice's Army Coward");

      assertItem(Slot.STICKER1, "scratch 'n' sniff UPC sticker");
      assertItemUnequip(Slot.STICKER2);
      assertItemUnequip(Slot.STICKER3);

      assertItem(Slot.FOLDER1, "folder (heavy metal)");
      assertItem(Slot.FOLDER2, "folder (tranquil landscape)");
      assertItem(Slot.FOLDER3, "folder (owl)");
      assertItemUnequip(Slot.FOLDER4);
      assertItemUnequip(Slot.FOLDER5);
    }
  }

  @Nested
  class professor {
    @ParameterizedTest
    @CsvSource({
      "mafia thumb ring, false",
      "Treads of Loathing, false",
      "panhandle panhandling hat, false",
      "batskin belt, true",
      "mafia wedding ring, true"
    })
    public void itShouldEquipWhatWasRequestedForProf(String item, boolean canBeEquipped) {
      AdventureResult itemAR = ItemPool.get(item);
      var cleanups =
          new Cleanups(
              withPath(AscensionPath.Path.WEREPROFESSOR),
              withIntrinsicEffect(EffectPool.MILD_MANNERED_PROFESSOR),
              withStats(1, 5, 1),
              withItem(itemAR));
      try (cleanups) {
        assertEquals(canBeEquipped, EquipmentManager.canEquip(itemAR));
      }
    }
  }
}
