package net.sourceforge.kolmafia.textui;

import net.sourceforge.kolmafia.utilities.PHPMTRandom;

public class Rng {
  private final PHPMTRandom mtRand;

  public Rng() {
    this(0);
  }

  public Rng(long seed) {
    mtRand = new PHPMTRandom(seed);
  }

  public int nextInt() {
    return mtRand.nextInt();
  }
}
