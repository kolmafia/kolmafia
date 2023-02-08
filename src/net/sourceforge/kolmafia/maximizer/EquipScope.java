package net.sourceforge.kolmafia.maximizer;

public enum EquipScope {
  /** Equip items straight away. Used when running from commandline. */
  IMMEDIATE,
  /** Consider items from inventory, but speculate only. */
  INVENTORY,
  /** Consider accessible and creatable items, but speculate only. */
  CREATABLE,
  /** Consider any items, but speculate only. */
  ANY;

  public boolean checkInventoryOnly() {
    return this == IMMEDIATE || this == INVENTORY;
  }

  public static EquipScope byIndex(int index) {
    return switch (index) {
      case 0 -> INVENTORY;
      case 1 -> CREATABLE;
        // backwards compat
      case -1 -> IMMEDIATE;
        // ideally should just be 2, but allow backwards compat greater values
      default -> ANY;
    };
  }
}
