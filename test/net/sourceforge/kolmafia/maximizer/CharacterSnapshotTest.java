package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withEquipped;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.util.EnumSet;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.Test;

class CharacterSnapshotTest {
  @Test
  void equipmentDoesNotChangeAfterCapture() {
    try (var cleanups = new Cleanups(withEquipped(Slot.HAT, "helmet turtle"))) {
      var snapshot = CharacterSnapshot.capture();

      EquipmentManager.setEquipment(Slot.HAT, EquipmentRequest.UNEQUIP);

      assertThat(snapshot.currentEquipment().get(Slot.HAT).getName(), is("helmet turtle"));
    }
  }

  @Test
  void completedRunDoesNotLeakItsSnapshot() {
    try (var cleanups = new Cleanups(withEquipped(Slot.HAT, "helmet turtle"))) {
      Maximizer.maximize(
          "item drop",
          EquipScope.SPECULATE_INVENTORY,
          0,
          PriceLevel.DONT_CHECK,
          false,
          EnumSet.noneOf(KoLConstants.filterType.class));

      EquipmentManager.setEquipment(Slot.HAT, EquipmentRequest.UNEQUIP);

      assertThat(
          Maximizer.character().currentEquipment().get(Slot.HAT), is(EquipmentRequest.UNEQUIP));
    }
  }
}
