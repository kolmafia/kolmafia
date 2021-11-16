package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FunctionTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "function parameter coercion to ANY_TYPE",
            "dump('foo', 'bar');",
            Arrays.asList("dump", "(", "'foo'", ",", "'bar'", ")", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-11", "1-13", "1-18", "1-19")),
        valid(
            "function parameter no typedef coercion",
            "typedef int foo; foo a = 1; void bar(int x, foo y) {} bar(a, 1);",
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "void", "bar", "(", "int",
                "x", ",", "foo", "y", ")", "{", "}", "bar", "(", "a", ",", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-13", "1-16", "1-18", "1-22", "1-24", "1-26", "1-27", "1-29",
                "1-34", "1-37", "1-38", "1-42", "1-43", "1-45", "1-49", "1-50", "1-52", "1-53",
                "1-55", "1-58", "1-59", "1-60", "1-62", "1-63", "1-64")),
        valid(
            "function parameter typedef-to-simple typedef coercion",
            // Mmh... there's no real way to "prove" the function was used other than
            // seeing it checked from clover...
            "typedef int foo; foo a = 1; int to_int(foo x) {return 1;} void bar(int x) {} bar(a);",
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "int", "to_int", "(",
                "foo", "x", ")", "{", "return", "1", ";", "}", "void", "bar", "(", "int", "x", ")",
                "{", "}", "bar", "(", "a", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-13", "1-16", "1-18", "1-22", "1-24", "1-26", "1-27", "1-29",
                "1-33", "1-39", "1-40", "1-44", "1-45", "1-47", "1-48", "1-55", "1-56", "1-57",
                "1-59", "1-64", "1-67", "1-68", "1-72", "1-73", "1-75", "1-76", "1-78", "1-81",
                "1-82", "1-83", "1-84")),
        valid(
            "function parameter simple-to-typedef typedef coercion",
            "typedef int foo; foo a = 1; foo to_foo(int x) {return a;} void bar(foo x) {} bar(1);",
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "foo", "to_foo", "(",
                "int", "x", ")", "{", "return", "a", ";", "}", "void", "bar", "(", "foo", "x", ")",
                "{", "}", "bar", "(", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-13", "1-16", "1-18", "1-22", "1-24", "1-26", "1-27", "1-29",
                "1-33", "1-39", "1-40", "1-44", "1-45", "1-47", "1-48", "1-55", "1-56", "1-57",
                "1-59", "1-64", "1-67", "1-68", "1-72", "1-73", "1-75", "1-76", "1-78", "1-81",
                "1-82", "1-83", "1-84")),
        valid(
            "record function match",
            "record rec {int i;}; void foo(rec x) {} foo(new rec());",
            Arrays.asList(
                "record", "rec", "{", "int", "i", ";", "}", ";", "void", "foo", "(", "rec", "x",
                ")", "{", "}", "foo", "(", "new", "rec", "(", ")", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-12", "1-13", "1-17", "1-18", "1-19", "1-20", "1-22", "1-27",
                "1-30", "1-31", "1-35", "1-36", "1-38", "1-39", "1-41", "1-44", "1-45", "1-49",
                "1-52", "1-53", "1-54", "1-55")),
        valid(
            "coerced function match",
            "void foo(float x) {} foo(1);",
            Arrays.asList(
                "void", "foo", "(", "float", "x", ")", "{", "}", "foo", "(", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-16", "1-17", "1-19", "1-20", "1-22", "1-25", "1-26",
                "1-27", "1-28")),
        valid(
            "vararg function match",
            "void foo(int... x) {} foo(1, 2, 3);",
            Arrays.asList(
                "void", "foo", "(", "int", "...", "x", ")", "{", "}", "foo", "(", "1", ",", "2",
                ",", "3", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-13", "1-17", "1-18", "1-20", "1-21", "1-23", "1-26",
                "1-27", "1-28", "1-30", "1-31", "1-33", "1-34", "1-35")),
        valid(
            "coerced vararg function match",
            "void foo(float... x) {} foo(1, 2, 3);",
            Arrays.asList(
                "void", "foo", "(", "float", "...", "x", ")", "{", "}", "foo", "(", "1", ",", "2",
                ",", "3", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-15", "1-19", "1-20", "1-22", "1-23", "1-25", "1-28",
                "1-29", "1-30", "1-32", "1-33", "1-35", "1-36", "1-37")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
