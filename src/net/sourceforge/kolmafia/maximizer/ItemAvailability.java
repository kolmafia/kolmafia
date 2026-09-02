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
    if (initial > used) return AcquisitionMethod.ACCESSIBLE;
    if (creatable + initial > used) return AcquisitionMethod.CREATE;
    if (npcBuyable + initial > used) return AcquisitionMethod.NPC_BUY;
    if (foldable + initial > used) return AcquisitionMethod.FOLD;
    if (pullable + initial > used) return AcquisitionMethod.PULL;
    if (pullFoldable + initial > used) return AcquisitionMethod.PULL_FOLD;
    if (storageBuyable + initial > used) return AcquisitionMethod.STORAGE_BUY;
    return AcquisitionMethod.MALL_BUY;
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
