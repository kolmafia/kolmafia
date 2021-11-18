package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class DirectiveTest {
  @BeforeEach
  public void setRevision() {
    StaticEntity.overrideRevision(10000);
  }

  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("interrupted script directive", "script", "Expected <, found end of file"),
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
        invalid("Unterminated script directive", "script \"zlib.ash", "No closing \" found"),
        valid(
            "since passes for low revision",
            "since r100;",
            Arrays.asList("since", "r100", ";"),
            Arrays.asList("1-1", "1-7", "1-11")),
        invalid(
            "since fails for high revision",
            "since r2000000000;",
            "requires revision r2000000000 of kolmafia or higher"),
        invalid("Invalid since version", "since 10;", "invalid 'since' format"),
        valid(
            "since passes for low version",
            "since 1.0;",
            Arrays.asList("since", "1.0", ";"),
            Arrays.asList("1-1", "1-7", "1-10")),
        invalid("since fails for high version", "since 2000000000.0;", "final point release"),
        invalid("since fails for not-a-number", "since yesterday;", "invalid 'since' format"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
