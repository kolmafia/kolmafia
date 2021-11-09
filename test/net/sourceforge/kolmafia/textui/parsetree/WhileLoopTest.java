package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class WhileLoopTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("while without condition", "while true", "Expected (, found true"),
        invalid("while with empty condition", "while ()", "\"while\" requires a boolean condition"),
        invalid("while with unclosed condition", "while (true", "Expected ), found end of file"),
        invalid("while with unclosed loop", "while (true) {", "Expected }, found end of file"),
        invalid(
            "while with multiple statements but no semicolon",
            "while (true) print(5)\nprint(6)",
            "Expected ;, found print"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
