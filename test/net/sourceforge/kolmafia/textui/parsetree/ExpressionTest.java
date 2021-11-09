package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
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
                "1-23", "1-26", "1-31", "1-32")),
        invalid("Interrupted ! expression", "(!", "Value expected"),
        invalid("Non-boolean ! expression", "(!'abc');", "\"!\" operator requires a boolean value"),
        invalid("Interrupted ~ expression", "(~", "Value expected"),
        invalid(
            "Non-boolean/integer ~ expression",
            "(~'abc');",
            "\"~\" operator requires an integer or boolean value"),
        invalid("Interrupted - expression", "(-", "Value expected"),
        invalid("Interrupted expression after operator", "(1 +", "Value expected"),
        invalid(
            "Invalid expression coercion",
            "(true + 1);",
            "Cannot apply operator + to true (boolean) and 1 (int)"),
        valid(
            "unary negation",
            "int x; (-x);",
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-9", "1-10", "1-11", "1-12")),
        valid(
            "Unary minus",
            "int x; (-x);",
            Arrays.asList("int", "x", ";", "(", "-", "x", ")", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-9", "1-10", "1-11", "1-12")),
        invalid(
            "non-coercible value mismatch",
            "(true ? 1 : $item[none];",
            "Cannot choose between 1 (int) and none (item)"),
        invalid("unclosed parenthetical expression", "(true", "Expected ), found end of file"),
        invalid("improper remove", "int i; remove i;", "Aggregate reference expected"),
        valid(
            "proper remove",
            "int[] map; remove map[0];",
            Arrays.asList("int", "[", "]", "map", ";", "remove", "map", "[", "0", "]", ";"),
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
