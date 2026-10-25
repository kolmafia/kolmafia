package net.sourceforge.kolmafia.utilities;

import java.util.List;
import java.util.Random;

/**
 * Constructed referencing the pseudocode available at <a
 * href="https://en.wikipedia.org/wiki/Mersenne_Twister#Pseudocode">...</a> This is mostly derived
 * from PHP's source code by Gausie.
 */
public class PHPMTRandom extends Random {
  public static final long serialVersionUID = 0L;

  static int STATE_LENGTH = 624;
  static int PERIOD = 397;
  static long MAGIC_CONSTANT = 0x9908b0dfL;
  static long MAX_UNSIGNED = 0xFFFFFFFFL;

  // Mask all but highest bit of u
  static long hiBit(long u) {
    return u & 0x80000000L;
  }

  // Mask all bit lowest bit of u
  static long loBit(long u) {
    return u & 0x00000001L;
  }

  // Mask the highest bit of u
  static long loBits(long u) {
    return u & 0x7FFFFFFFL;
  }

  // Move hi bit of u to hi bit of v
  static long mixBits(long u, long v) {
    return hiBit(u) | loBits(v);
  }

  static long twist(long m, long u, long v) {
    return (m ^ (mixBits(u, v) >> 1) ^ ((MAX_UNSIGNED * (loBit(u))) & MAGIC_CONSTANT));
  }

  static long temper(long value) {
    value ^= (value >> 11);
    value ^= (value << 7) & 2_636_928_640L;
    value ^= (value << 15) & 4_022_730_752L;
    return (value ^ (value >> 18));
  }

  long[] state;
  int index;

  @Override
  public int next(int bits) {
    if (index >= STATE_LENGTH) {
      reload();
    }

    long value = state[index++];
    return (int) (temper(value) >> 1);
  }

  @Override
  public double nextDouble() {
    return nextInt() / (Integer.MAX_VALUE + 1.0);
  }

  @Override
  public int nextInt(final int max) {
    return nextInt(0, max);
  }

  @SuppressWarnings("PMD.MissingOverride")
  public int nextInt(final int min, final int max) {
    double clamped = (max - min + 1.0) * nextDouble();
    return min + (int) clamped;
  }

  public <T> T pickOne(List<T> list) {
    if (list.size() == 0) {
      return null;
    }

    return list.get(nextInt(0, list.size() - 1));
  }

  void initialize(final long seed) {
    state[0] = seed & Long.MAX_VALUE;

    for (int i = 1; i < STATE_LENGTH; i++) {
      long prev = state[i - 1];
      state[i] = ((1_812_433_253L * (prev ^ (prev >> 30))) + i) & 4_294_967_295L;
    }
  }

  void reload() {
    for (int i = 0; i < STATE_LENGTH; i++) {
      state[i] = twist(state[(i + PERIOD) % STATE_LENGTH], state[i], state[(i + 1) % STATE_LENGTH]);
    }
    index = 0;
  }

  @Override
  public synchronized void setSeed(long seed) {
    if (state == null) {
      state = new long[STATE_LENGTH];
    }

    // In PHP source code the signed int is converted to an unsigned int
    if (seed < 0) {
      seed += 0x100000000L;
    }

    initialize(seed);
    reload();
  }

  public PHPMTRandom(long s) {
    super(s);
  }
}
