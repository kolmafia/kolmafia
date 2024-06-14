package net.sourceforge.kolmafia;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
  @ValueSource(strings = {"1/0", "-4^0.5", "999^999", "sqrt(-1)"})
  public void invalidArithmeticReturnsZero(String invalidExpr) {
    var exp = new Expression(invalidExpr, invalidExpr);
    assertEquals(0.0, exp.eval());
  }

  @Test
  public void invalidExpressionReturnsZero() {
    var exp = new Expression("@", "Invalid bytecode");
    assertEquals(0.0, exp.eval());
  }

  @ParameterizedTest
  @CsvSource({
    "abs(-11), 11",
    "ceil(9.1), 10",
    "floor(5.6), 5",
    "'min(1,2)', 1",
    "'max(1,2)', 2",
    "'gt(4,5)', 0",
    "'gt(5,5)', 0",
    "'gt(5,4)', 1",
    "'gte(4,5)', 0",
    "'gte(5,5)', 1",
    "'gte(5,4)', 1",
    "'lt(4,5)', 1",
    "'lt(5,5)', 0",
    "'lt(5,4)', 0",
    "'lte(4,5)', 1",
    "'lte(5,5)', 1",
    "'lte(5,4)', 0",
    "sqrt(25), 5",
  })
  public void canDoSupportedMathFunctions(String input, String expected) {
    var exp = new Expression(input, input);
    assertEquals(Double.parseDouble(expected), exp.eval());
  }

  @Nested
  class Pref {
    @ParameterizedTest
    @CsvSource({
      "true, 1",
      "false, 0",
      "45, 45",
    })
    public void canReadPrefs(String prefValue, String expected) {
      var cleanups = withProperty("test", prefValue);

      try (cleanups) {
        var exp = new Expression("pref(test)", "pref(test) where pref = " + prefValue);
        assertEquals(Double.parseDouble(expected), exp.eval());
      }
    }

    @ParameterizedTest
    @CsvSource({
      "abc, 1", "xyz, 0",
    })
    public void canComparePrefs(String prefValue, String expected) {
      var cleanups = withProperty("test", prefValue);

      try (cleanups) {
        var exp = new Expression("pref(test,abc)", "pref(test,abc) where pref = " + prefValue);
        assertEquals(Double.parseDouble(expected), exp.eval());
      }
    }
  }

  @Test
  public void canTrackBiggerInts() {
    var exp = new Expression("32769", "integer bigger than 32768");
    assertEquals(32769.0, exp.eval());
  }

  @Test
  void canReportMultipleErrors() {
    var exp = new Expression("1+(4*path(The Source))", "nonexistent function");
    assertThat(exp.hasErrors(), equalTo(true));
  }

  @Test
  void canReportItemCountsById() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.UNIVERSAL_SEASONING, 4),
            withItemInCloset(ItemPool.UNIVERSAL_SEASONING, 2),
            withItemInStorage(ItemPool.UNIVERSAL_SEASONING, 1));

    try (cleanups) {
      var exp = new Expression("haveitem(Universal Seasoning)", "have universal seasoning");
      assertThat(exp.eval(), is(4.0));
    }
  }

  @Test
  void canReportItemCountsByName() {
    var cleanups = new Cleanups(withItem(ItemPool.FILET_OF_TANGY_GNAT, 2));

    try (cleanups) {
      var exp = new Expression("haveitem(2528)", "have filet of tangy gnat");
      assertThat(exp.eval(), is(2.0));
    }
  }
}
