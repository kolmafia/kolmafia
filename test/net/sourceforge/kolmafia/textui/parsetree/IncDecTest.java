package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class IncDecTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "preincrement with non-numeric variable",
            "string x; ++x;",
            "++X requires a numeric variable reference"),
        invalid("preincrement requires a variable", "++1;", "Variable reference expected"),
        valid(
            "predecrement with float variable",
            "float x; --x;",
            Arrays.asList("float", "x", ";", "--", "x", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-10", "1-12", "1-13")),
        invalid(
            "postincrement with non-numeric variable",
            "string x; x++;",
            "X++ requires a numeric variable reference"),
        /* Currently fails with "Expected ;, found ++" which is asymmetric.
        invalid(
            "postincrement requires a variable",
            "1++;",
            "Variable reference expected"),
        */
        valid(
            "postdecrement with float variable",
            "float x; x--;",
            Arrays.asList("float", "x", ";", "x", "--", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-10", "1-11", "1-13")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
