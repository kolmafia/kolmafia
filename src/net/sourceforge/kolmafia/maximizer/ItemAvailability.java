package net.sourceforge.kolmafia.maximizer;

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
}
