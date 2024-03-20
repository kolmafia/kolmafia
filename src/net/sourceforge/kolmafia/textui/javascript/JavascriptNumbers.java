package net.sourceforge.kolmafia.textui.javascript;

/** This mimics the behavior of the Rhino NativeNumber class */
@SuppressWarnings("UnnecessaryUnboxing")
public class JavascriptNumbers {

  /**
   * @see https://www.ecma-international.org/ecma-262/6.0/#sec-number.max_safe_integer
   */
  private static final double MAX_SAFE_INTEGER = 9007199254740991.0; // Math.pow(2, 53) - 1

  private static final double MIN_SAFE_INTEGER = -MAX_SAFE_INTEGER;

  private static boolean isDoubleInteger(Double d) {
    return !d.isInfinite() && !d.isNaN() && (Math.floor(d.doubleValue()) == d.doubleValue());
  }

  static boolean isDoubleSafeInteger(Double d) {
    return isDoubleInteger(d)
        && (d.doubleValue() <= MAX_SAFE_INTEGER)
        && (d.doubleValue() >= MIN_SAFE_INTEGER);
  }
}
