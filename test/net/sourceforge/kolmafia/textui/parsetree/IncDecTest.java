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

public class IncDecTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "preincrement with non-numeric variable",
            "string x; ++x;",
            "++X requires a numeric variable reference",
            "char 13 to char 14",
            "++x;",
            "Unknown variable 'x'",
            "char 3 to char 4"),
        invalid(
            "preincrement requires a variable",
            "++1;",
            "Variable reference expected",
            "char 3 to char 4"),
        invalid(
            "preincrement requires a variable 2",
            "int x; ++x.to_int();",
            "Variable reference expected",
            "char 10 to char 20"),
        valid(
            "predecrement with float variable",
            "float x; --x;",
            Arrays.asList("float", "x", ";", "--", "x", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-10", "1-12", "1-13"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              IncDec preIncDec = assertInstanceOf(IncDec.class, commands.get(1));
              // "--" + the variable reference
              ParserTest.assertLocationEquals(1, 10, 1, 13, preIncDec.getLocation());
            }),
        invalid(
            "postincrement with non-numeric variable",
            "string x; x++;",
            "X++ requires a numeric variable reference",
            "char 11 to char 12",
            "x++;",
            "Unknown variable 'x'",
            "char 1 to char 2"),
        /* Currently fails with "Expected ;, found ++" which is asymmetric.
        invalid(
            "postincrement requires a variable",
            "1++;",
            "Variable reference expected",
            "char 1 to char 2"),
        */
        valid(
            "postdecrement with float variable",
            "float x; x--;",
            Arrays.asList("float", "x", ";", "x", "--", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-10", "1-11", "1-13"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              IncDec postIncDec = assertInstanceOf(IncDec.class, commands.get(1));
              // The variable reference + "--"
              ParserTest.assertLocationEquals(1, 10, 1, 13, postIncDec.getLocation());
            }));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
