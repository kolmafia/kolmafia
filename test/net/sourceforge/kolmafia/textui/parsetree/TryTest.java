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

public class TryTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "finally without try",
            "finally {}",
            // "Encountered 'finally' without try",
            "Unknown variable 'finally'"),
        invalid(
            "try without catch or finally",
            "try {}",
            // This ought to allow catch, but it's not implemented yet.
            "\"try\" without \"finally\" is pointless"),
        invalid(
            "try-catch",
            "try {} catch {}",
            // This ought to allow catch, but it's not implemented yet.
            "\"try\" without \"finally\" is pointless"),
        valid(
            "try-finally",
            "try {} finally {}",
            Arrays.asList("try", "{", "}", "finally", "{", "}"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-16", "1-17"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Try tryCommand = assertInstanceOf(Try.class, commands.get(0));
              // From the "try" up to the last token of the "finally"'s command
              ParserTest.assertLocationEquals(1, 1, 1, 18, tryCommand.getLocation());
            }));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
