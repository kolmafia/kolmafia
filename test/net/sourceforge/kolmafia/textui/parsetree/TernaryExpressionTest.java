package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TernaryExpressionTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "ternary with non-boolean condition",
            "int x = 1 ? 1 : 2;",
            "Non-boolean expression 1 (int)"),
        invalid("ternary without lhs", "int x = true ? : 2;", "Value expected in left hand side"),
        invalid("ternary without colon", "int x = true ? 1;", "Expected :, found ;"),
        invalid(
            "ternary without rhs",
            "int x = true ? 1:;",
            // Another asymmetry: not "Value expected in right hand side"
            "Value expected"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
