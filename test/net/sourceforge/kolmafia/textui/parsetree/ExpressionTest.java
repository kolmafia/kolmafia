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

public class ExpressionTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Complex expression parsing",
            // Among other things, this tests that true / false are case insensitive, in case
            // you want to pretend you're working in Python, C, or both.
            "(!(~-5 == 10) && True || FALSE);",
            Arrays.asList(
                "(", "!", "(", "~", "-", "5", "==", "10", ")", "&&", "True", "||", "FALSE", ")",
                ";"),
            Arrays.asList(
                "1-1", "1-2", "1-3", "1-4", "1-5", "1-6", "1-8", "1-11", "1-13", "1-15", "1-18",
                "1-23", "1-26", "1-31", "1-32"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // (!(~-5 == 10) && True || FALSE)
              Operation operation = assertInstanceOf(Operation.class, commands.get(0));
              // Constant value location test - FALSE
              ParserTest.assertLocationEquals(
                  1, 26, 1, 31, operation.getRightHandSide().getLocation());

              // !(~-5 == 10) && True
              operation = assertInstanceOf(Operation.class, operation.getLeftHandSide());
              // Operation location test - operation with lhs + oper + rhs
              ParserTest.assertLocationEquals(1, 2, 1, 22, operation.getLocation());
              // Constant value location test - True
              ParserTest.assertLocationEquals(
                  1, 18, 1, 22, operation.getRightHandSide().getLocation());

              // !(~-5 == 10)
              operation = assertInstanceOf(Operation.class, operation.getLeftHandSide());
              // Operation location test - operation with oper + lhs
              ParserTest.assertLocationEquals(1, 2, 1, 14, operation.getLocation());
            }),
        valid(
            "File name constant",
            // __FILE__ is a case-sensitive constant that bears the value of the current file's
            // name.
            "__FILE__",
            Arrays.asList("__FILE__"),
            Arrays.asList("1-1"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Value.Constant file = assertInstanceOf(Value.Constant.class, commands.get(0));
              ParserTest.assertLocationEquals(1, 1, 1, 9, file.getLocation());
            }),
        invalid(
            "Incorrect file name constant",
            "__file__",
            "Unknown variable '__file__'",
            "char 1 to char 9"),
        invalid("Interrupted ! expression", "(!", "Value expected", "char 3"),
        invalid(
            "Non-boolean ! expression",
            "(!'abc');",
            "\"!\" operator requires a boolean value",
            "char 2 to char 8"),
        invalid("Interrupted ~ expression", "(~", "Value expected", "char 3"),
        invalid(
            "Non-boolean/integer ~ expression",
            "(~'abc');",
            "\"~\" operator requires an integer or boolean value",
            "char 2 to char 8"),
        invalid("Interrupted - expression", "(-", "Value expected", "char 3"),
        invalid(
            "Non-integer/float - expression",
            "(-'abc');",
            "\"-\" operator requires an integer or float value",
            "char 2 to char 8"),
        invalid("Interrupted expression after operator", "(1 +", "Value expected", "char 5"),
        invalid(
            "Invalid expression coercion",
            "(true + 1);",
            "Cannot apply operator + to true (boolean) and 1 (int)",
            "char 2 to char 10"),
        valid(
            "Unary minus",
            "int x; (-x);",
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-9", "1-10", "1-11", "1-12"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Parenthesized expression location test
              Operation operation = assertInstanceOf(Operation.class, commands.get(1));
              // An evaluable surrounded by parenthesis gets those parenthesis added to its location
              ParserTest.assertLocationEquals(1, 8, 1, 12, operation.getLocation());

              // Operator location test
              Operator oper = operation.getOperator();
              ParserTest.assertLocationEquals(1, 9, 1, 10, oper.getLocation());
            }),
        invalid(
            "non-coercible value mismatch",
            "(true ? 1 : $item[none];",
            "Cannot choose between 1 (int) and none (item)",
            "char 9 to char 24"),
        invalid(
            "unclosed parenthetical expression",
            "(true",
            "Expected ), found end of file",
            "char 6"),
        invalid(
            "improper remove",
            "int i; remove i;",
            "Aggregate reference expected",
            "char 15 to char 16"),
        valid(
            "proper remove",
            "int[] map; remove map[0];",
            Arrays.asList("int", "[", "]", "map", ";", "remove", "map", "[", "0", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-7", "1-10", "1-12", "1-19", "1-22", "1-23", "1-24", "1-25"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Operation operation = assertInstanceOf(Operation.class, commands.get(1));
              Operator oper = operation.getOperator();
              ParserTest.assertLocationEquals(1, 12, 1, 18, oper.getLocation());
            }),
        valid(
            "proper remove 2",
            // 'remove' is case-insensitive
            "int[] map; ReMoVe map[0];",
            Arrays.asList("int", "[", "]", "map", ";", "ReMoVe", "map", "[", "0", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-7", "1-10", "1-12", "1-19", "1-22", "1-23", "1-24",
                "1-25")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
