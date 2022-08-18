package net.sourceforge.kolmafia.swingui;

import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GearChangeFrameTest {
  @BeforeEach
  void beforeEach() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("GearChangeFrameTest");
  }

  @Nested
  class ShowModifiers {
    private static String modifierText(String text) {
      return text.length() > 54 ? text.substring(29, text.length() - 25) : "";
    }

    @Test
    void canShowBasicItemModifiers() {
      var mods =
          GearChangeFrame.getModifiers(
              ItemPool.get(ItemPool.RAVIOLI_HAT), EquipmentManager.HAT, false, 1);
      assertThat(
          modifierText(mods.toString()),
          equalTo(
              "Dmg Absorption:<div align=right>+10.00</div>Spell Dmg:<div align=right>+1.00</div>"));
    }

    @Test
    void doesntShowItemModifiersForHatOnHatrack() {
      var cleanups = new Cleanups(withFamiliar(FamiliarPool.HATRACK));
      try (cleanups) {
        var mods =
            GearChangeFrame.getModifiers(
                ItemPool.get(ItemPool.RAVIOLI_HAT), EquipmentManager.FAMILIAR, false, 1);
        assertThat(modifierText(mods.toString()), equalTo(""));
      }
    }

    @Test
    void weaponsInOffhandSlotGetPowerDamage() {
      var mods =
          GearChangeFrame.getModifiers(
              ItemPool.get(ItemPool.SEAL_CLUB), EquipmentManager.WEAPON, false, 1);
      assertThat(modifierText(mods.toString()), equalTo("Weapon Dmg:<div align=right>+1.50</div>"));
    }

    @Test
    void offhandsInOffhandSlotDoNotGetPowerDamage() {
      var mods =
          GearChangeFrame.getModifiers(
              ItemPool.get(ItemPool.BONERDAGON_SKULL), EquipmentManager.OFFHAND, false, 1);
      assertThat(modifierText(mods.toString()), equalTo("Spooky Dmg:<div align=right>+5.00</div>"));
    }

    @ParameterizedTest
    @CsvSource({
      ",, Maximum HP:<div align=right>+25.00</div>Maximum MP:<div align=right>+25.00</div>",
      "diamondback skin, nicksilver spurs, Monster Level:<div align=right>+20.00</div>Item Drop:<div align=right>+20.00</div>Maximum HP:<div align=right>+25.00</div>Maximum MP:<div align=right>+25.00</div>",
    })
    void canShowCowboyBootsModifiers(String skin, String spurs, String expectedMods) {
      var cleanups = new Cleanups();

      if (skin != null) cleanups.add(withEquipped(EquipmentManager.BOOTSKIN, skin));
      if (spurs != null) cleanups.add(withEquipped(EquipmentManager.BOOTSPUR, spurs));

      try (cleanups) {
        var mods =
            GearChangeFrame.getModifiers(
                ItemPool.get(ItemPool.COWBOY_BOOTS), EquipmentManager.ACCESSORY1, false, 1);
        assertThat(modifierText(mods.toString()), equalTo(expectedMods));
      }
    }

    @ParameterizedTest
    @CsvSource({
      ", Maximum HP:<div align=right>+20.00</div>Maximum MP:<div align=right>+20.00</div>Single Equip",
      "meat, Meat Drop:<div align=right>+50.00</div>Maximum HP:<div align=right>+20.00</div>Maximum MP:<div align=right>+20.00</div>Single Equip"
    })
    void canShowBackupCameraModifiers(String setting, String expectedMods) {
      var cleanups = new Cleanups(withProperty("backupCameraMode", setting == null ? "" : setting));

      try (cleanups) {
        var mods =
            GearChangeFrame.getModifiers(
                ItemPool.get(ItemPool.BACKUP_CAMERA), EquipmentManager.ACCESSORY1, false, 1);
        assertThat(modifierText(mods.toString()), equalTo(expectedMods));
      }
    }
  }
}
