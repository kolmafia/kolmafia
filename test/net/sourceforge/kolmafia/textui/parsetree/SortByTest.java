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

public class SortByTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "variable definition of sort",
            "int sort = 0;",
            Arrays.asList("int", "sort", "=", "0", ";"),
            Arrays.asList("1-1", "1-5", "1-10", "1-12", "1-13")),
        valid(
            "variable declaration of sort",
            "int sort;",
            Arrays.asList("int", "sort", ";"),
            Arrays.asList("1-1", "1-5", "1-9")),
        valid(
            "function named sort",
            "void sort(){} sort();",
            Arrays.asList("void", "sort", "(", ")", "{", "}", "sort", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-10", "1-11", "1-12", "1-13", "1-15", "1-19", "1-20", "1-21")),
        invalid(
            "sort not-a-variable primitive", "sort 2 by value;", "Aggregate reference expected"),
        invalid("sort without by", "int[] x {3,2,1}; sort x;", "Expected by, found ;"),
        invalid("Sort, no sorting expression", "int[] x; sort x by", "Expression expected"),
        valid(
            "valid sort",
            "int[] x; sort x by value*3;",
            Arrays.asList("int", "[", "]", "x", ";", "sort", "x", "by", "value", "*", "3", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-7", "1-8", "1-10", "1-15", "1-17", "1-20", "1-25", "1-26",
                "1-27"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              SortBy sort = assertInstanceOf(SortBy.class, commands.get(1));
              // From the "sort" up to the token prior to the semi-colon
              ParserTest.assertLocationEquals(1, 10, 1, 27, sort.getLocation());
            }));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
