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

public class BasicScriptTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Multiline cli_execute script",
            "cli_execute {\n  echo hello world;\n  echo sometimes we don't have a semicolon\n}",
            Arrays.asList(
                "cli_execute",
                "{",
                "echo hello world;",
                "echo sometimes we don't have a semicolon",
                "}"),
            Arrays.asList("1-1", "1-13", "2-3", "3-3", "4-1"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              BasicScript basicScript = assertInstanceOf(BasicScript.class, commands.get(0));
              // From the "cli_execute" up to the closing "}"
              ParserTest.assertLocationEquals(1, 1, 4, 2, basicScript.getLocation());
            }),
        invalid("Interrupted cli_execute script", "cli_execute {", "Expected }, found end of file"),
        valid(
            "Non-basic-script cli_execute",
            "int cli_execute; cli_execute++",
            Arrays.asList("int", "cli_execute", ";", "cli_execute", "++"),
            Arrays.asList("1-1", "1-5", "1-16", "1-18", "1-29")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
