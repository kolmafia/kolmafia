package net.sourceforge.kolmafia.maximizer;

record AcquisitionOption(AcquisitionMethod method, int quantity) {}

enum AcquisitionMethod {
  ACCESSIBLE,
  CREATE,
  NPC_BUY,
  FOLD,
  PULL,
  PULL_FOLD,
  STORAGE_BUY,
  MALL_BUY
}
