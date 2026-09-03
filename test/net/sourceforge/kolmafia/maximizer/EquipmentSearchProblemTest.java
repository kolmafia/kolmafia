package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withOverrideModifiers;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.List;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EquipmentSearchProblemTest {
  private static final int TURTLE = ItemPool.HELMET_TURTLE;
  private static final int SLIME_HAT = ItemPool.get("hardened slime hat", 1).getItemId();
  private static final int DESIGNER_PANTS = ItemPool.DESIGNER_SWEATPANTS;
  private static final int OLD_PANTS = ItemPool.get("old sweatpants", 1).getItemId();

  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("EquipmentSearchProblemTest");
  }

  @Test
  void matchesBruteForceOnReducedRealEquipment() {
    try (var cleanups =
        new Cleanups(
            withEquippableItem(TURTLE),
            withEquippableItem(SLIME_HAT),
            withEquippableItem(DESIGNER_PANTS),
            withEquippableItem(OLD_PANTS),
            withOverrideModifiers(ModifierType.ITEM, TURTLE, "Item Drop: +5"),
            withOverrideModifiers(ModifierType.ITEM, SLIME_HAT, "Item Drop: +2"),
            withOverrideModifiers(ModifierType.ITEM, DESIGNER_PANTS, "Item Drop: +1"),
            withOverrideModifiers(ModifierType.ITEM, OLD_PANTS, "Item Drop: +4"))) {
      assertTrue(maximize("item, 1 hat, 1 pants, -tie"));
      SolutionQuality actual = Maximizer.best().quality();

      var baseline = new MaximizerSpeculation();
      var choices = List.of(items(TURTLE, SLIME_HAT), items(DESIGNER_PANTS, OLD_PANTS));
      var oracle =
          BruteForceMaximizer.maximize(
              choices,
              ignored -> true,
              equipment -> {
                var candidate = baseline.clone();
                candidate.equipment.put(Slot.HAT, equipment.get(0));
                candidate.equipment.put(Slot.PANTS, equipment.get(1));
                candidate.setUnscored();
                return candidate.quality();
              });

      assertThat(actual, is(oracle.quality()));
    }
  }

  private static List<AdventureResult> items(int first, int second) {
    return List.of(EquipmentRequest.UNEQUIP, ItemPool.get(first, 1), ItemPool.get(second, 1));
  }
}
