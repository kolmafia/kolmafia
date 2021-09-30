package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.Random;

/**
 * PHP < 7.1.0 uses glibc's rand function which is explained very clearly at
 * https://www.mscs.dal.ca/~selinger/random/. This is mostly derived directly from PHP's source code
 * by Gausie.
 */
public class PHPRandom extends Random {
  public static final long serialVersionUID = 0l;
  public ArrayList<Integer> state;

  public int next(int bits) {
    int i = state.size();
    int value = state.get(i - 31) + state.get(i - 3);
    state.add(value);
    return value >>> 1;
  }

  public double nextDouble() {
    return nextInt() / (Integer.MAX_VALUE + 1.0);
  }

  public int nextInt(final int max) {
    return nextInt(0, max);
  }

  public int nextInt(final int min, final int max) {
    double clamped = (max - min + 1.0) * nextDouble();
    int val = min + (int) clamped;
    return val;
  }

  public synchronized void setSeed(long seed) {
    if (state == null) {
      state = new ArrayList<Integer>();
    }

    state.clear();
    state.add((int) seed);

    for (int i = 1; i < 31; i++) {
      int value = (int) ((16_807l * state.get(i - 1)) % Integer.MAX_VALUE);
      if (value < 0) {
        value += Integer.MAX_VALUE;
      }
      state.add(value);
    }

    for (int i = 31; i < 34; i++) {
      state.add(state.get(i - 31));
    }

    for (int i = 34; i < 344; i++) {
      next(32);
    }
  }

  public int[] array(int count, int required) {
    required = Math.min(required, count);

    int[] result = new int[required];
    int j = 0;

    for (int i = 0; i < count; i++) {
      double chance = ((required - j) / (double) (count - i));
      if (nextDouble() < chance) {
        result[j++] = i;
      }
    }

    return result;
  }

  public PHPRandom(int s) {
    super(s);
  }
}
