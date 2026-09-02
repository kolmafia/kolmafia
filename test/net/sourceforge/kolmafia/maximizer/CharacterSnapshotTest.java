package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.util.EnumSet;
import net.sourceforge.kolmafia.AscensionPath.Path;
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

  @Test
  void activatesOnlyResourcesForTheCurrentPath() {
    assertThat(CharacterSnapshot.capture().resourceUsage("Bobby"), is(ResourceUsage.EMPTY));

    try (var cleanups = new Cleanups(withPath(Path.BEES_HATE_YOU))) {
      var defaultSnapshot = CharacterSnapshot.capture(new Evaluator("item drop"));
      var defaultUsage = defaultSnapshot.resourceUsage("Bobby");
      assertThat(defaultUsage.cost(0), is(3));
      assertThat(defaultSnapshot.resourcesExceeded(defaultUsage), is(true));

      var relaxedSnapshot = CharacterSnapshot.capture(new Evaluator("item drop, 5 beeosity"));
      assertThat(
          relaxedSnapshot.resourcesExceeded(relaxedSnapshot.resourceUsage("Bobby")), is(false));
    }
  }
}
