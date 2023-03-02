package net.sourceforge.kolmafia.swingui;

import static internal.helpers.Player.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GearChangePanelTest {
  @BeforeEach
  void beforeEach() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("GearChangeFrameTest");
  }

  @Nested
  class Update {
    @BeforeAll
    static void beforeAll() {
      new GearChangePanel();
    }

    @BeforeEach
    void beforeEach() {
      GearChangePanel.updateSlot(Slot.HAT);
    }

    @Test
    void doesUpdateList() {
      var cleanups = withItem(ItemPool.RAVIOLI_HAT);
      try (cleanups) {
        var ravioliHat =
            KoLConstants.inventory.get(
                KoLConstants.inventory.indexOf(ItemPool.get(ItemPool.RAVIOLI_HAT, 1)));
        var equipCleanups = withEquipped(Slot.HAT, ravioliHat);
        try (equipCleanups) {
          GearChangePanel.updateSlot(Slot.HAT);
          assertThat(
              ((AdventureResult) GearChangePanel.getModel(Slot.HAT).getSelectedItem()).getItemId(),
              equalTo(ItemPool.RAVIOLI_HAT));
        }
      }
    }

    @Test
    void updatesWaitForDeferralResolution() {
      var cleanups1 = withItem(ItemPool.RAVIOLI_HAT);
      try (cleanups1) {
        var ravioliHat =
            KoLConstants.inventory.get(
                KoLConstants.inventory.indexOf(ItemPool.get(ItemPool.RAVIOLI_HAT, 1)));
        GearChangePanel.updateSlot(Slot.HAT);
        GearChangePanel.deferUpdate();
        // withEquipped calls updateSlot again, which sets the equipped item but should defer
        // updating the whole list.
        var cleanups2 =
            new Cleanups(withEquipped(Slot.HAT, ravioliHat), withItem(ItemPool.HELMET_TURTLE));
        try (cleanups2) {
          var hatModel = GearChangePanel.getModel(Slot.HAT);
          // List should not be updated yet, so helmet turtle should not be in it.
          assertThat(hatModel, not(contains(ItemPool.get(ItemPool.HELMET_TURTLE, 1))));
          GearChangePanel.resolveDeferredUpdate();
          assertThat(
              ((AdventureResult) hatModel.getSelectedItem()).getItemId(),
              equalTo(ItemPool.RAVIOLI_HAT));
        }
      }
    }
  }

  @Nested
  class ShowModifiers {
    private static String modifierText(String text) {
      return text.length() > 54 ? text.substring(29, text.length() - 25) : "";
    }

    @Test
    void canShowBasicItemModifiers() {
      var mods =
          GearChangePanel.getModifiers(ItemPool.get(ItemPool.RAVIOLI_HAT), Slot.HAT, false, 1);
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
            GearChangePanel.getModifiers(
                ItemPool.get(ItemPool.RAVIOLI_HAT), Slot.FAMILIAR, false, 1);
        assertThat(modifierText(mods.toString()), equalTo(""));
      }
    }

    @Test
    void weaponsInOffhandSlotGetPowerDamage() {
      var mods =
          GearChangePanel.getModifiers(ItemPool.get(ItemPool.SEAL_CLUB), Slot.WEAPON, false, 1);
      assertThat(modifierText(mods.toString()), equalTo("Weapon Dmg:<div align=right>+1.50</div>"));
    }

    @Test
    void offhandsInOffhandSlotDoNotGetPowerDamage() {
      var mods =
          GearChangePanel.getModifiers(
              ItemPool.get(ItemPool.BONERDAGON_SKULL), Slot.OFFHAND, false, 1);
      assertThat(modifierText(mods.toString()), equalTo("Spooky Dmg:<div align=right>+5.00</div>"));
    }

    @ParameterizedTest
    @CsvSource({
      ",, Maximum HP:<div align=right>+25.00</div>Maximum MP:<div align=right>+25.00</div>",
      "diamondback skin, nicksilver spurs, Monster Level:<div align=right>+20.00</div>Item Drop:<div align=right>+20.00</div>Maximum HP:<div align=right>+25.00</div>Maximum MP:<div align=right>+25.00</div>",
    })
    void canShowCowboyBootsModifiers(String skin, String spurs, String expectedMods) {
      var cleanups = new Cleanups();

      if (skin != null) cleanups.add(withEquipped(Slot.BOOTSKIN, skin));
      if (spurs != null) cleanups.add(withEquipped(Slot.BOOTSPUR, spurs));

      try (cleanups) {
        var mods =
            GearChangePanel.getModifiers(
                ItemPool.get(ItemPool.COWBOY_BOOTS), Slot.ACCESSORY1, false, 1);
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
            GearChangePanel.getModifiers(
                ItemPool.get(ItemPool.BACKUP_CAMERA), Slot.ACCESSORY1, false, 1);
        assertThat(modifierText(mods.toString()), equalTo(expectedMods));
      }
    }
  }
}
