package net.sourceforge.kolmafia.utilities;

/**
 * KoL's seeded selection of count distinct indices from a list of size entries. A single pick is an
 * mtrand roll that rerolls on the overflow value. Multiple picks use a rand selection. Used for
 * TCRS equipment enchantments and the voting booth's daily modifiers.
 */
public class PHPRandomSelection {
  private final PHPRandom rng;
  private final PHPMTRandom mtRng;

  public PHPRandomSelection(final int seed) {
    this(new PHPRandom(seed), new PHPMTRandom(seed));
  }

  public PHPRandomSelection(final PHPRandom rng, final PHPMTRandom mtRng) {
    this.rng = rng;
    this.mtRng = mtRng;
  }

  public int[] pick(final int size, final int count) {
    if (count <= 0 || size <= 0) {
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
