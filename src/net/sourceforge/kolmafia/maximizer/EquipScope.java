package net.sourceforge.kolmafia.maximizer;

public enum EquipScope {
  /** Equip items straight away. Used when running from commandline. */
  EQUIP_NOW,
  /** Consider items from inventory, but speculate only. */
  SPECULATE_INVENTORY,
  /** Consider accessible and creatable items, but speculate only. */
  SPECULATE_CREATABLE,
  /** Consider any items, but speculate only. */
  SPECULATE_ANY;

  public boolean checkInventoryOnly() {
    return this == EQUIP_NOW || this == SPECULATE_INVENTORY;
  }

  public static EquipScope byIndex(int index) {
    return switch (index) {
      case 0 -> SPECULATE_INVENTORY;
      case 1 -> SPECULATE_CREATABLE;
        // backwards compat
      case -1 -> EQUIP_NOW;
        // ideally should just be 2, but allow backwards compat greater values
      default -> SPECULATE_ANY;
    };
  }
}
