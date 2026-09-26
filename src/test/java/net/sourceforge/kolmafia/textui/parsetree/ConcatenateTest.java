package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ConcatenateTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "String concatenation with left-side coercion",
            "(1 + 'abc');",
            Arrays.asList("(", "1", "+", "'abc'", ")", ";"),
            Arrays.asList("1-1", "1-2", "1-4", "1-6", "1-11", "1-12")),
        valid(
            "String concatenation with right-side coercion",
            "('abc' + 1);",
            Arrays.asList("(", "'abc'", "+", "1", ")", ";"),
            Arrays.asList("1-1", "1-2", "1-8", "1-10", "1-11", "1-12")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
