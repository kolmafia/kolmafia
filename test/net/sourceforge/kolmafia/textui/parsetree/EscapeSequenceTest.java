package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class EscapeSequenceTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "string with uninteresting escape",
            "'\\z'",
            Arrays.asList("'\\z'"),
            Arrays.asList("1-1")),
        valid(
            "string with unclearly terminated octal escape",
            "'\\1131'",
            // \113 is char code 75, which maps to 'K'
            Arrays.asList("'\\1131'"),
            Arrays.asList("1-1")),
        invalid(
            "string with insufficient octal digits",
            "'\\11'",
            "Octal character escape requires 3 digits",
            "char 2 to char 6"),
        invalid(
            "string with invalid octal digits",
            "'\\118'",
            "Octal character escape requires 3 digits",
            "char 2 to char 6"),
        valid(
            "string with hex digits",
            "'\\x3fgh'",
            Arrays.asList("'\\x3fgh'"),
            Arrays.asList("1-1")),
        invalid(
            "string with invalid hex digits",
            "'\\xhello'",
            "Hexadecimal character escape requires 2 digits",
            "char 2 to char 6"),
        invalid(
            "string with insufficient hex digits",
            "'\\x1'",
            "Hexadecimal character escape requires 2 digits",
            "char 2 to char 6"),
        valid(
            "string with unicode digits",
            "'\\u0041'",
            Arrays.asList("'\\u0041'"),
            Arrays.asList("1-1")),
        invalid(
            "string with invalid unicode digits",
            "'\\uzzzz'",
            "Unicode character escape requires 4 digits",
            "char 2 to char 8"),
        invalid(
            "string with insufficient unicode digits",
            "'\\u1'",
            "Unicode character escape requires 4 digits",
            "char 2 to char 6"),
        invalid("string with escaped eof", "'\\", "No closing ' found", "char 1 to char 3"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
