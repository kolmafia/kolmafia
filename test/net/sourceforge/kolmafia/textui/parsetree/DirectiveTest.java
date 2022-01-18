package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DirectiveTest {
  @BeforeAll
  public static void setRevision() {
    StaticEntity.overrideRevision(10000);
  }

  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "interrupted script directive", "script", "Expected <, found end of file", "char 7"),
        valid(
            "empty script directive",
            "script;",
            Arrays.asList("script", ";"),
            Arrays.asList("1-1", "1-7")),
        valid(
            "script directive delimited with <>",
            "script <zlib.ash>;",
            Arrays.asList("script", "<zlib.ash>", ";"),
            Arrays.asList("1-1", "1-8", "1-18")),
        valid(
            "script directive delimited with \"\"",
            "script \"zlib.ash\"",
            Arrays.asList("script", "\"zlib.ash\""),
            Arrays.asList("1-1", "1-8")),
        valid(
            "script directive without delimiter",
            "script zlib.ash",
            Arrays.asList("script", "zlib.ash"),
            Arrays.asList("1-1", "1-8")),
        invalid(
            "Unterminated script directive",
            "script \"zlib.ash",
            "No closing \" found",
            "char 8 to char 17"),
        invalid(
            "Unterminated script directive 2",
            "script \"zlib.ash \n Have I told you about that time when--",
            "No closing \" found",
            "char 8 to char 17"),
        invalid(
            "Unterminated script directive 3",
            "script \"zlib.ash ; Have I told you about that time when--",
            "No closing \" found",
            "char 8 to char 18"),
        valid(
            "since passes for low revision",
            "since r100;",
            Arrays.asList("since", "r100", ";"),
            Arrays.asList("1-1", "1-7", "1-11")),
        invalid(
            "since fails for high revision",
            "since r2000000000;",
            "'null' requires revision r2000000000 of kolmafia or higher (current: r10000).  Up-to-date builds can be found at https://ci.kolmafia.us/.",
            "char 1 to char 18"),
        invalid("Invalid since version", "since 10;", "invalid 'since' format", "char 1 to char 9"),
        valid(
            "since passes for low version",
            "since 1.0;",
            Arrays.asList("since", "1.0", ";"),
            Arrays.asList("1-1", "1-7", "1-10")),
        invalid(
            "since fails for high version",
            "since 2000000000.0;",
            "invalid 'since' format (21.09 was the final point release)",
            "char 1 to char 19"),
        invalid(
            "since fails for not-a-number",
            "since yesterday;",
            "invalid 'since' format",
            "char 1 to char 16"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
