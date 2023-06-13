package net.sourceforge.kolmafia.maximizer;

public enum PriceLevel {
  DONT_CHECK,
  BUYABLE_ONLY,
  ALL;

  public static PriceLevel byIndex(int index) {
    return switch (index) {
      case 2 -> ALL;
      case 1 -> BUYABLE_ONLY;
        // backwards compat
      default -> DONT_CHECK;
    };
  }
}
