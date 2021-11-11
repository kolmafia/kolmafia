package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CatchTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            // Catch in ASH is comparable to a function that catches and
            // returns the text of any errors thrown by its block.
            "catch without try",
            "catch {}",
            Arrays.asList("catch", "{", "}"),
            Arrays.asList("1-1", "1-7", "1-8")),
        valid(
            "catch value block",
            "string error_message = catch {}",
            Arrays.asList("string", "error_message", "=", "catch", "{", "}"),
            Arrays.asList("1-1", "1-8", "1-22", "1-24", "1-30", "1-31")),
        valid(
            "catch value expression",
            "string error_message = catch ( print(__FILE__) )",
            Arrays.asList(
                "string", "error_message", "=", "catch", "(", "print", "(", "__FILE__", ")", ")"),
            Arrays.asList(
                "1-1", "1-8", "1-22", "1-24", "1-30", "1-32", "1-37", "1-38", "1-46", "1-48")),
        invalid(
            "catch value interrupted",
            "string error_message = catch",
            "\"catch\" requires a block or an expression"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
