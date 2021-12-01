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

public class VariableReferenceTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "indexed variable reference",
            "int[5] x; x[0];",
            Arrays.asList("int", "[", "5", "]", "x", ";", "x", "[", "0", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-8", "1-9", "1-11", "1-12", "1-13", "1-14", "1-15")),
        invalid(
            "indexed primitive",
            "int x; x[0];",
            "Variable 'x' cannot be indexed",
            "char 8 to char 10"),
        invalid(
            "over-indexed variable reference",
            "int[5] x; x[0,1];",
            "Too many keys for 'x'",
            "char 11 to char 15"),
        invalid(
            "empty indexed variable reference",
            "int[5] x; x[];",
            "Index for 'x' expected",
            "char 13 to char 14"),
        invalid(
            "unterminated aggregate variable reference",
            "int[5] x; x[0",
            "Expected ], found end of file",
            "char 14"),
        invalid(
            "type-mismatched indexed variable reference",
            "int[5] x; x['str'];",
            "Index for 'x' has wrong data type (expected int, got string)",
            "char 13 to char 18"),
        invalid(
            "type-mismatched indexed composite reference",
            "int[5, 5] x; x[0]['str'];",
            "Index for 'x[]' has wrong data type (expected int, got string)",
            "char 19 to char 24"),
        valid(
            "multidimensional comma-separated array index",
            "int[5,5] x; x[0,1];",
            Arrays.asList(
                "int", "[", "5", ",", "5", "]", "x", ";", "x", "[", "0", ",", "1", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11", "1-13", "1-14", "1-15",
                "1-16", "1-17", "1-18", "1-19")),
        valid(
            "multidimensional bracket-separated array index",
            "int[5,5] x; x[0][1];",
            Arrays.asList(
                "int", "[", "5", ",", "5", "]", "x", ";", "x", "[", "0", "]", "[", "1", "]", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11", "1-13", "1-14", "1-15",
                "1-16", "1-17", "1-18", "1-19", "1-20"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              CompositeReference reference =
                  assertInstanceOf(CompositeReference.class, commands.get(1));
              // From the first variable reference, to the last index
              ParserTest.assertLocationEquals(1, 13, 1, 20, reference.getLocation());
            }),
        invalid(
            "non-record property reference", "int i; i.a;", "Record expected", "char 8 to char 9"),
        valid(
            "record field reference",
            "record {int a;} r; r.a;",
            Arrays.asList("record", "{", "int", "a", ";", "}", "r", ";", "r", ".", "a", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-9", "1-13", "1-14", "1-15", "1-17", "1-18", "1-20", "1-21", "1-22",
                "1-23"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // CompositeReference location test
              CompositeReference reference =
                  assertInstanceOf(CompositeReference.class, commands.get(1));
              ParserTest.assertLocationEquals(1, 20, 1, 23, reference.getLocation());

              // Implicit value location test
              List<Evaluable> indices = reference.getIndices();
              ParserTest.assertLocationEquals(1, 22, 1, 23, indices.get(0).getLocation());
            }),
        invalid(
            "record field reference without field",
            "record {int a;} r; r.",
            "Field name expected",
            "char 22"),
        invalid(
            "record unknown field reference",
            "record {int a;} r; r.b;",
            "Invalid field name 'b'",
            "char 22 to char 23"),
        valid(
            "record field reference from function",
            "my_class().primestat;",
            Arrays.asList("my_class", "(", ")", ".", "primestat", ";"),
            Arrays.asList("1-1", "1-9", "1-10", "1-11", "1-12", "1-21"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              CompositeReference reference =
                  assertInstanceOf(CompositeReference.class, commands.get(0));
              ParserTest.assertLocationEquals(1, 1, 1, 21, reference.getLocation());
            }));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
