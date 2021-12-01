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

public class StringTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("Multiline string, end of line not escaped", "'\n'", "No closing ' found"),
        valid(
            "Multiline string, end of line properly escaped",
            "'\\\n'",
            Arrays.asList("'\\", "'"),
            Arrays.asList("1-1", "2-1"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Value.Constant string = assertInstanceOf(Value.Constant.class, commands.get(0));
              ParserTest.assertLocationEquals(1, 1, 2, 2, string.getLocation());
            }),
        valid(
            "Multiline string, end of line properly escaped + empty lines",
            "'\\\n\n   \n\n\n'",
            Arrays.asList("'\\", "'"),
            Arrays.asList("1-1", "6-1")),
        valid(
            "Trailing spaces after string",
            "'foobar'    //",
            Arrays.asList("'foobar'", "//"),
            Arrays.asList("1-1", "1-13"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Value.Constant string = assertInstanceOf(Value.Constant.class, commands.get(0));
              ParserTest.assertLocationEquals(1, 1, 1, 9, string.getLocation());
            }),
        invalid(
            "Multiline string, end of line properly escaped + empty lines + comment",
            "'\\\n\n\n//Comment\n\n'",
            "No closing ' found"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
