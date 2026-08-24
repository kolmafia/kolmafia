package net.sourceforge.kolmafia.utilities;

/**
 * KoL's seeded selection of count distinct indices from a list of size entries. A single pick is an
 * MT roll that rerolls on the overflow value; multiple picks use a glibc selection. Used for TCRS
 * equipment enchantments and the voting booth's daily modifiers.
 */
public class PHPRandomSelection {
  private PHPRandomSelection() {}

  public static int[] pick(final int seed, final int size, final int count) {
    return pick(new PHPRandom(seed), new PHPMTRandom(seed), size, count);
  }

  // Draws from the provided rollers rather than creating them, so the caller can keep using them
  // afterwards. A single pick uses mtRng and leaves rng untouched; multiple picks advance rng.
  public static int[] pick(
      final PHPRandom rng, final PHPMTRandom mtRng, final int size, final int count) {
    if (count <= 0) {
      return new int[0];
    }
    if (count == 1) {
      int v = size;
      while (v == size) {
        v = mtRng.nextInt(0, size);
      }
      return new int[] {v};
    }
    return rng.array(size, count);
  }
}
