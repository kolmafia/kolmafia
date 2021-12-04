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

public class WhileLoopTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "while statement",
            "while (true) print('hello');",
            Arrays.asList("while", "(", "true", ")", "print", "(", "'hello'", ")", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-12", "1-14", "1-19", "1-20", "1-27", "1-28"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Loop location test
              WhileLoop whileLoop = assertInstanceOf(WhileLoop.class, commands.get(0));
              // From the "while" up to the end of its scope
              ParserTest.assertLocationEquals(1, 1, 1, 29, whileLoop.getLocation());

              // Scope location test
              Scope loopScope = whileLoop.getScope();
              ParserTest.assertLocationEquals(1, 14, 1, 29, loopScope.getLocation());
            }),
        invalid(
            "while without condition", "while true", "Expected (, found true", "char 7 to char 11"),
        invalid(
            "while with empty condition", "while ()", "Expression expected", "char 8 to char 9"),
        invalid(
            "while with incorrect condition",
            "while (2)",
            "\"while\" requires a boolean conditional expression",
            "char 8 to char 9"),
        invalid(
            "while with unclosed condition",
            "while (true",
            "Expected ), found end of file",
            "char 12"),
        invalid(
            "while with unclosed loop",
            "while (true) {",
            "Expected }, found end of file",
            "char 15"),
        invalid(
            "while with multiple statements but no semicolon",
            "while (true) print(5)\nprint(6)",
            "Expected ;, found print",
            "line 2, char 1 to char 6"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
