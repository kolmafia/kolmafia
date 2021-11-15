package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AggregateTypeTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("unterminated plural aggregate type", "int[", "Missing index token"),
        // TODO: add tests for size == 0, size < 0, which ought to fail.
        valid(
            "array with explicit size",
            "int[2] x;",
            Arrays.asList("int", "[", "2", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-8", "1-9")),
        valid(
            "array with unspecified size",
            "int[] x;",
            Arrays.asList("int", "[", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-7", "1-8")),
        valid(
            "multidimensional array with partially specified size (1)",
            "int[2][] x;",
            Arrays.asList("int", "[", "2", "]", "[", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11")),
        valid(
            "multidimensional array with partially specified size (2)",
            "int[][2] x;",
            Arrays.asList("int", "[", "]", "[", "2", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11")),
        valid(
            "multidimensional array with explicit size",
            "int[2, 2] x;",
            Arrays.asList("int", "[", "2", ",", "2", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-8", "1-9", "1-11", "1-12")),
        // TODO: `typedef int[] intArray` and aggregate shouldn't be valid keys.
        invalid(
            "map with non-primitive key type",
            "record a{int b;}; int[a] x;",
            "Index type 'a' is not a primitive type"),
        invalid(
            "multidimensional map with partially specified comma-separated type",
            "int[2,] x;",
            "Missing index token"),
        invalid(
            "multidimensional map with unspecified comma-separated type",
            "int[,,,] x;",
            "Missing index token"),
        invalid(
            "multidimensional map with partially-specified comma-separated type",
            "int[int,] x;",
            "Missing index token"),
        invalid("abruptly terminated 1-d map", "int[int", "Expected , or ], found end of file"),
        invalid("abruptly terminated 1-d array", "int[4", "Expected , or ], found end of file"),
        invalid("map with unknown type", "int[a] x;", "Invalid type name 'a'"),
        invalid("map containing aggregate type", "int[int[]] x;", "Expected , or ], found ["),
        valid(
            "multidimensional map with comma-separated type",
            "int[int, int] x;",
            Arrays.asList("int", "[", "int", ",", "int", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-8", "1-10", "1-13", "1-15", "1-16"),
            scope -> {
              Iterator<Variable> variables = scope.getVariables().iterator();

              assertTrue(variables.hasNext());
              AggregateType array =
                  assertInstanceOf(AggregateType.class, variables.next().getType());
              assertFalse(variables.hasNext());

              // int[int, int]
              ParserTest.assertLocationEquals(1, 1, 1, 14, array.getLocation());
              // Aggregates don't have a "definition"
              assertNull(array.getDefinitionLocation());

              // int[..., int]
              array = assertInstanceOf(AggregateType.class, array.getDataType());
              // The locations of the various dimensions of a multidimensional map are all the same.
              // This is due to the keys being listed from left to right, but the final value being
              // at the very left.
              //
              // Imagine this example:
              // value[key1][key2][key3]
              // It can be represented as
              // >> value | [key1] >> [key2] >> [key3] vv
              // ^^<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
              //
              // The whole map is key1>>key2>>key3>>value, so its location is the whole thing.
              // One layer deep is key2>>key3>>value. We start at key2 and end at value, but pass by
              // key3
              // Two layers deep is key3>>value. We start at key3 and end at value. Same thing.
              ParserTest.assertLocationEquals(1, 1, 1, 14, array.getLocation());
              assertNull(array.getDefinitionLocation());
            }),
        valid(
            "multidimensional map with chained brackets with types",
            "int[int][int] x;",
            Arrays.asList("int", "[", "int", "]", "[", "int", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-8", "1-9", "1-10", "1-13", "1-15", "1-16")),
        valid(
            "multidimensional map with unspecified type in chained empty brackets",
            "int[][] x;",
            Arrays.asList("int", "[", "]", "[", "]", "x", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-6", "1-7", "1-9", "1-10")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
