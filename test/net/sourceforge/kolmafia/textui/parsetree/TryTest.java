package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
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
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-16", "1-17")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
