package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class ExpressionTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("Expression");
    Preferences.reset("Expression");
  }

  @ParameterizedTest
  @CsvSource({"1+1, 2", "70-1, 69", "4*2, 8", "30/5, 6", "41%10, 1", "3^3, 27"})
  public void canDoBasicArithmetic(String input, String expected) {
    var exp = new Expression(input, input);
    assertEquals(Double.parseDouble(expected), exp.eval());
  }

  @ParameterizedTest
  @CsvSource({
    "abs(-11), 11",
    "ceil(9.1), 10",
    "floor(5.6), 5",
    "'min(1,2)', 1",
    "'max(1,2)', 2",
    "sqrt(25), 5",
  })
  public void canDoSupportedMathFunctions(String input, String expected) {
    var exp = new Expression(input, input);
    assertEquals(Double.parseDouble(expected), exp.eval());
  }

  @ParameterizedTest
  @CsvSource({
    "true, 1",
    "false, 0",
    "45, 45",
  })
  public void canReadPrefs(String prefValue, String expected) {
    Preferences.setString("test", prefValue);
    var exp = new Expression("pref(test)", "pref(test) where pref = " + prefValue);
    assertEquals(Double.parseDouble(expected), exp.eval());
  }

  @ParameterizedTest
  @CsvSource({
    "abc, 1", "xyz, 0",
  })
  public void canComparePrefs(String prefValue, String expected) {
    Preferences.setString("test", prefValue);
    var exp = new Expression("pref(test,abc)", "pref(test,abc) where pref = " + prefValue);
    assertEquals(Double.parseDouble(expected), exp.eval());
  }

  @Test
  public void canTrackBiggerInts() {
    var exp = new Expression("32769", "integer bigger than 32768");
    assertEquals(32769.0, exp.eval());
  }
}
