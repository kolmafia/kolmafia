package net.sourceforge.kolmafia.objectpool;

public class IntegerPool {
  private static final int MIN_VALUE = -2;
  private static final int MAX_VALUE = 13000;

  private static final int RANGE = (IntegerPool.MAX_VALUE - IntegerPool.MIN_VALUE) + 1;

  private static final Integer[] CACHE = new Integer[IntegerPool.RANGE];

  private static int cacheHits = 0;
  private static int cacheMissHighs = 0;
  private static int cacheMissLows = 0;

  static {
    for (int i = 0; i < IntegerPool.RANGE; ++i) {
      IntegerPool.CACHE[i] = Integer.valueOf(IntegerPool.MIN_VALUE + i);
    }
  }

  public static final int getCacheHits() {
    return IntegerPool.cacheHits;
  }

  public static final int getCacheMissLows() {
    return IntegerPool.cacheMissLows;
  }

  public static final int getCacheMissHighs() {
    return IntegerPool.cacheMissHighs;
  }

  public static final Integer get(int i) {
    if (i < IntegerPool.MIN_VALUE) {
      ++cacheMissLows;
      return i;
    }

    if (i > IntegerPool.MAX_VALUE) {
      ++cacheMissHighs;
      return i;
    }

    ++cacheHits;
    return IntegerPool.CACHE[i - IntegerPool.MIN_VALUE];
  }
}
