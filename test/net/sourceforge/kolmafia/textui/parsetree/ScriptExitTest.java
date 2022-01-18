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

public class ScriptExitTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "top-level exit",
            "exit;",
            Arrays.asList("exit", ";"),
            Arrays.asList("1-1", "1-5"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              ScriptExit scriptExit = assertInstanceOf(ScriptExit.class, commands.get(0));
              ParserTest.assertLocationEquals(1, 1, 1, 5, scriptExit.getLocation());
            }),
        invalid("exit with parameter", "exit 1;", "Expected ;, found 1", "char 6 to char 7"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
