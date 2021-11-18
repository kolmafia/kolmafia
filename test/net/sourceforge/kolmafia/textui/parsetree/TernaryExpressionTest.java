package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TernaryExpressionTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "ternary expression",
            "(true ? false ? 1 : 2 : 3);",
            Arrays.asList("(", "true", "?", "false", "?", "1", ":", "2", ":", "3", ")", ";"),
            Arrays.asList(
                "1-1", "1-2", "1-7", "1-9", "1-15", "1-17", "1-19", "1-21", "1-23", "1-25", "1-26",
                "1-27"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              TernaryExpression ternary =
                  assertInstanceOf(TernaryExpression.class, commands.get(0));
              // The parenthesis changes the location, so we can't use that one for the test
              TernaryExpression ternary2 =
                  assertInstanceOf(TernaryExpression.class, ternary.getLeftHandSide());
              ParserTest.assertLocationEquals(1, 9, 1, 22, ternary2.getLocation());
            }),
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
