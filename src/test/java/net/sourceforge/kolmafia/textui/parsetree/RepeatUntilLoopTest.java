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

public class RepeatUntilLoopTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "repeat statement",
            "repeat print('hello'); until(true);",
            Arrays.asList(
                "repeat", "print", "(", "'hello'", ")", ";", "until", "(", "true", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-13", "1-14", "1-21", "1-22", "1-24", "1-29", "1-30", "1-34",
                "1-35"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Loop location test
              RepeatUntilLoop repeatLoop = assertInstanceOf(RepeatUntilLoop.class, commands.get(0));
              // From the "repeat" up to the end of its "until" condition
              ParserTest.assertLocationEquals(1, 1, 1, 35, repeatLoop.getLocation());

              // Scope location test
              Scope loopScope = repeatLoop.getScope();
              ParserTest.assertLocationEquals(1, 8, 1, 23, loopScope.getLocation());
            }),
        invalid(
            "repeat without until", "repeat {}", "Expected until, found end of file", "char 10"),
        invalid(
            "repeat without condition",
            "repeat {} until true",
            "Expected (, found true",
            "char 17 to char 21"),
        invalid(
            "repeat with empty condition",
            "repeat {} until ()",
            "Expression expected",
            "char 18 to char 19"),
        invalid(
            "repeat with incorrect condition",
            "repeat {} until ('done')",
            // This should probably read as "'until' requires a
            // boolean conditional expression"...
            "\"repeat\" requires a boolean conditional expression",
            "char 18 to char 24",
            "repeat {} until (done)",
            "Unknown variable 'done'",
            "char 18 to char 22"),
        invalid(
            "repeat with unclosed condition",
            "repeat {} until (true",
            "Expected ), found end of file",
            "char 22"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
