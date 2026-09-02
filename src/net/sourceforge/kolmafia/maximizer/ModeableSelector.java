package net.sourceforge.kolmafia.maximizer;

import java.util.Map;
import net.sourceforge.kolmafia.Modeable;

final class ModeableSelector {
  private ModeableSelector() {}

  static Map<Modeable, String> select(
      Map<Modeable, Boolean> needed,
      Map<Modeable, String> forced,
      EquipScope equipScope,
      long maxPrice,
      PriceLevel priceLevel) {
    Map<Modeable, String> selected = Modeable.getStringMap(modeable -> "");
    for (Modeable modeable : Modeable.values()) {
      if (!needed.get(modeable)) {
        continue;
      }

      String forcedMode = forced.get(modeable);
      if (!forcedMode.isEmpty()) {
        selected.put(modeable, forcedMode);
        continue;
      }

      CheckedItem item = new CheckedItem(modeable.getItemId(), equipScope, maxPrice, priceLevel);
      MaximizerSpeculation baseline = new MaximizerSpeculation();
      baseline.attachment = item;
      baseline.equipment.put(modeable.getSlot(), item);
      baseline.setModeable(modeable, modeable.getState());

      MaximizerSpeculation best =
          MaximizerSpeculation.bestOf(
              baseline,
              modeable.getModes(),
              (spec, mode) -> {
                spec.attachment = item;
                spec.equipment.put(modeable.getSlot(), item);
                spec.setModeable(modeable, mode);
              });
      selected.put(modeable, best.getModeables().get(modeable));
    }
    return selected;
  }
}
