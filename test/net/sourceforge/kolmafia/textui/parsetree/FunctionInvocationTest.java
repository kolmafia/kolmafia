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

public class FunctionInvocationTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "function invocation interrupted",
            "call",
            "Variable reference expected for function name"),
        invalid(
            "function invocation non-string expression",
            "call (2)()",
            "String expression expected for function name"),
        invalid(
            "function invocation interrupted after name expression",
            "call ('foo')",
            "Expected (, found end of file"),
        valid(
            "function invocation with non-void function",
            // ummm this should insist that the variable is a string...
            "int x; call string x('foo')",
            Arrays.asList("int", "x", ";", "call", "string", "x", "(", "'foo'", ")"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-13", "1-20", "1-21", "1-22", "1-27"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              FunctionInvocation invocation =
                  assertInstanceOf(FunctionInvocation.class, commands.get(1));
              // From the "call" up to the closing ")" of the parameters
              ParserTest.assertLocationEquals(1, 8, 1, 28, invocation.getLocation());
            }));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
