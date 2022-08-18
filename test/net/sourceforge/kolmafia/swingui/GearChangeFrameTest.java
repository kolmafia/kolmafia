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
import org.junit.jupiter.api.BeforeAll;
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
    static GearChangeFrame.EquipmentTabPanel PANE = null;

    @BeforeAll
    static void beforeAll() {
      var frame = new GearChangeFrame();
      frame.tabs.setSelectedIndex(0);
      PANE = (GearChangeFrame.EquipmentTabPanel) frame.tabs.getSelectedComponent();
    }

    private String modifierText() {
      var text = PANE.getModifiersLabel().getText();
      return text.length() > 56 ? text.substring(31, text.length() - 25) : "";
    }

    @Test
    void canShowBasicItemModifiers() {
      GearChangeFrame.showModifiers(ItemPool.get(ItemPool.RAVIOLI_HAT), false);
      assertThat(
          modifierText(),
          equalTo(
              "Dmg Absorption:<div align=right>+10.00</div>Spell Dmg:<div align=right>+1.00</div>"));
    }

    @Test
    void doesntShowItemModifiersForHatOnHatrack() {
      var cleanups = new Cleanups(withFamiliar(FamiliarPool.HATRACK));
      try (cleanups) {
        GearChangeFrame.showModifiers(ItemPool.get(ItemPool.RAVIOLI_HAT), true);
        assertThat(modifierText(), equalTo(""));
      }
    }

    @ParameterizedTest
    @CsvSource({
      ",, Maximum HP:<div align=right>+25.00</div>Maximum MP:<div align=right>+25.00</div>",
      "diamondback skin, nicksilver spurs, Monster Level:<div align=right>+20.00</div>Item Drop:<div align=right>+20.00</div>Maximum HP:<div align=right>+25.00</div>Maximum MP:<div align=right>+25.00</div>",
    })
    void canShowCowboyBootsModifiers(String skin, String spurs, String mods) {
      var cleanups = new Cleanups();

      if (skin != null) cleanups.add(withEquipped(EquipmentManager.BOOTSKIN, skin));
      if (spurs != null) cleanups.add(withEquipped(EquipmentManager.BOOTSPUR, spurs));

      try (cleanups) {
        GearChangeFrame.showModifiers(ItemPool.get(ItemPool.COWBOY_BOOTS), false);
        assertThat(modifierText(), equalTo(mods));
      }
    }

    @ParameterizedTest
    @CsvSource({
      ", Maximum HP:<div align=right>+20.00</div>Maximum MP:<div align=right>+20.00</div>Single Equip",
      "meat, Meat Drop:<div align=right>+50.00</div>Maximum HP:<div align=right>+20.00</div>Maximum MP:<div align=right>+20.00</div>Single Equip"
    })
    void canShowBackupCameraModifiers(String setting, String mods) {
      var cleanups = new Cleanups(withProperty("backupCameraMode", setting == null ? "" : setting));

      try (cleanups) {
        GearChangeFrame.showModifiers(ItemPool.get(ItemPool.BACKUP_CAMERA), false);
        assertThat(modifierText(), equalTo(mods));
      }
    }
  }
}
