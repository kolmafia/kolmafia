package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.List;

record ItemAvailability(
    int inventory,
    int initial,
    int creatable,
    int npcBuyable,
    int mallBuyable,
    int foldable,
    int pullable,
    int pullFoldable,
    int storageBuyable) {

  int total() {
    return initial
        + creatable
        + npcBuyable
        + mallBuyable
        + foldable
        + pullable
        + pullFoldable
        + storageBuyable;
  }

  AcquisitionMethod acquisitionMethod(int used) {
    if (used < 0) throw new IllegalArgumentException("Used quantity cannot be negative");
    for (var option : this.options()) {
      if (used < option.quantity()) return option.method();
      used -= option.quantity();
    }
    throw new IllegalArgumentException("No acquisition option for requested copy");
  }

  List<AcquisitionOption> options() {
    var options = new ArrayList<AcquisitionOption>(8);
    addOption(options, AcquisitionMethod.ACCESSIBLE, initial);
    addOption(options, AcquisitionMethod.CREATE, creatable);
    addOption(options, AcquisitionMethod.NPC_BUY, npcBuyable);
    addOption(options, AcquisitionMethod.FOLD, foldable);
    addOption(options, AcquisitionMethod.PULL, pullable);
    addOption(options, AcquisitionMethod.PULL_FOLD, pullFoldable);
    addOption(options, AcquisitionMethod.STORAGE_BUY, storageBuyable);
    addOption(options, AcquisitionMethod.MALL_BUY, mallBuyable);
    return List.copyOf(options);
  }

  private static void addOption(
      List<AcquisitionOption> options, AcquisitionMethod method, int quantity) {
    if (quantity > 0) options.add(new AcquisitionOption(method, quantity));
  }
}
