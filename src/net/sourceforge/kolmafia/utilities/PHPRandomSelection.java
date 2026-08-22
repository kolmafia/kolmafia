package net.sourceforge.kolmafia.utilities;

/**
 * KoL's seeded selection of {@code count} distinct indices from a list of {@code size} entries. A
 * single pick is a {@link PHPMTRandom} roll over {@code [0, size]} that rerolls on the overflow
 * value {@code size}; multiple picks use {@link PHPRandom#array}. Used for TCRS equipment
 * enchantments and the voting booth's daily modifiers.
 */
public class PHPRandomSelection {
  private PHPRandomSelection() {}

  public static int[] pick(final int seed, final int size, final int count) {
    if (count <= 0) {
      return new int[0];
    }
    if (count == 1) {
      var rng = new PHPMTRandom(seed);
      int v = size;
      while (v == size) {
        v = rng.nextInt(0, size);
      }
      return new int[] {v};
    }
    return new PHPRandom(seed).array(size, count);
  }
}
