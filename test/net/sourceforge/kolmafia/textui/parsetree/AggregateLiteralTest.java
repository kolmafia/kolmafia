package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AggregateLiteralTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Simple map literal",
            "int[item] { $item[seal-clubbing club]: 1, $item[helmet turtle]: 2}",
            Arrays.asList(
                "int",
                "[",
                "item",
                "]",
                "{",
                "$",
                "item",
                "[",
                "seal-clubbing club",
                "]",
                ":",
                "1",
                ",",
                "$",
                "item",
                "[",
                "helmet turtle",
                "]",
                ":",
                "2",
                "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-9", "1-11", "1-13", "1-14", "1-18", "1-19", "1-37", "1-38",
                "1-40", "1-41", "1-43", "1-44", "1-48", "1-49", "1-62", "1-63", "1-65", "1-66")),
        invalid("Unterminated aggregate literal", "int[int] {", "Expected }, found end of file"),
        invalid("Interrupted map literal", "int[int] {:", "Script parsing error"),
        valid(
            "Simple array literal",
            "int[5] { 1, 2, 3, 4, 5}",
            Arrays.asList(
                "int", "[", "5", "]", "{", "1", ",", "2", ",", "3", ",", "4", ",", "5", "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-8", "1-10", "1-11", "1-13", "1-14", "1-16", "1-17",
                "1-19", "1-20", "1-22", "1-23")),
        valid(
            "Array literal with trailing comma",
            "int[2] { 1, 2, }",
            Arrays.asList("int", "[", "2", "]", "{", "1", ",", "2", ",", "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-8", "1-10", "1-11", "1-13", "1-14", "1-16")),
        valid(
            "Array literal with variable",
            "int x = 10; int[5] { 1, 2, x, 4, 5}",
            Arrays.asList(
                "int", "x", "=", "10", ";", "int", "[", "5", "]", "{", "1", ",", "2", ",", "x", ",",
                "4", ",", "5", "}"),
            Arrays.asList(
                "1-1", "1-5", "1-7", "1-9", "1-11", "1-13", "1-16", "1-17", "1-18", "1-20", "1-22",
                "1-23", "1-25", "1-26", "1-28", "1-29", "1-31", "1-32", "1-34", "1-35")),
        /*
        invalid(
            // We ought to check for this case too, but we don't...
            "Array literal not enough elements",
            "int[10] { 1, 2, 3, 4, 5}",
            "Array has 10 elements but 5 initializers."),
        */
        invalid(
            "Array literal too many elements",
            "int[1] { 1, 2, 3, 4, 5 }",
            "Array has 1 elements but 5 initializers."),
        valid(
            "Empty multidimensional map literal",
            "int[int, int]{}",
            Arrays.asList("int", "[", "int", ",", "int", "]", "{", "}"),
            Arrays.asList("1-1", "1-4", "1-5", "1-8", "1-10", "1-13", "1-14", "1-15")),
        valid(
            "Non-empty multidimensional map literal",
            "int[int, int, int]{ {}, { 1:{2, 3} } }",
            Arrays.asList(
                "int", "[", "int", ",", "int", ",", "int", "]", "{", "{", "}", ",", "{", "1", ":",
                "{", "2", ",", "3", "}", "}", "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-8", "1-10", "1-13", "1-15", "1-18", "1-19", "1-21", "1-22",
                "1-23", "1-25", "1-27", "1-28", "1-29", "1-30", "1-31", "1-33", "1-34", "1-36",
                "1-38")),
        invalid(
            "Interrupted multidimensional map literal",
            "int[int, int] {1:",
            "Script parsing error"),
        invalid("Invalid array literal coercion", "int[int]{ 'foo' }", "Invalid array literal"),
        invalid("Invalid map literal coercion", "int[int]{ 'foo':'bar' }", "Invalid map literal"),
        invalid(
            "Ambiguity between array and map literal",
            "boolean[5]{ 0:true, 1:true, 2:false, true, 4:false }",
            "Expected :, found ,"),
        invalid(
            "Ambiguity between array and map literal 2: index and data are both integers",
            "int[5]{ 0, 1, 2, 3:3, 4 }",
            "Cannot include keys when making an array literal"),
        invalid(
            "Ambiguity between array and map literal 3: that can't be a key",
            "string[5]{ '0', '1', '2', '3':'3', '4' }",
            "Expected , or }, found :"),
        invalid(
            "Unexpected aggregate in array literal",
            "boolean[5]{ true, true, false, {true}, false }",
            "Expected an element of type boolean, found an aggregate"),
        invalid(
            "Unexpected aggregate in map literal: as a key",
            "boolean[5]{ 0:true, 1:true, 2:false, {3}:true, 4:false }",
            "Expected a key of type int, found an aggregate"),
        invalid(
            "Unexpected aggregate in map literal: as a value",
            "boolean[5]{ 0:true, 1:true, 2:false, 3:{true}, 4:false }",
            "Expected a value of type boolean, found an aggregate"),
        valid(
            // This... exercises a different code path.
            "Parenthesized map literal",
            "(int[int]{})",
            Arrays.asList("(", "int", "[", "int", "]", "{", "}", ")"),
            Arrays.asList("1-1", "1-2", "1-5", "1-6", "1-9", "1-10", "1-11", "1-12")),
        valid(
            "aggregate-initialized definition",
            "int[1] x { 1 };",
            Arrays.asList("int", "[", "1", "]", "x", "{", "1", "}", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-8", "1-10", "1-12", "1-14", "1-15")),
        invalid("aggregate literal without braces", "(int[])", "Expected {, found )"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
