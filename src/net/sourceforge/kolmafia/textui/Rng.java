package net.sourceforge.kolmafia.textui;

import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;

public class Rng {
  private final PHPRandom rand;
  private final PHPMTRandom mtRand;

  public Rng() {
    this(0);
  }

  public Rng(long seed) {
    rand = new PHPRandom(seed);
    mtRand = new PHPMTRandom(seed);
  }

  public int nextRandInt() {
    return rand.nextInt();
  }

  public int nextMtRandInt() {
    return mtRand.nextInt();
  }
}
