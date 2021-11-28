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

public class LoopBreakTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "break outside loop",
            "break;",
            "Encountered 'break' outside of loop",
            "char 1 to char 6"),
        valid(
            "break inside while-loop",
            "while (true) break;",
            Arrays.asList("while", "(", "true", ")", "break", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-12", "1-14", "1-19"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              WhileLoop whileLoop = assertInstanceOf(WhileLoop.class, commands.get(0));
              Scope whileScope = whileLoop.getScope();
              LoopBreak loopBreak =
                  assertInstanceOf(LoopBreak.class, whileScope.getCommandList().get(0));
              ParserTest.assertLocationEquals(1, 14, 1, 19, loopBreak.getLocation());
            }),
        valid(
            "break inside switch",
            "switch (true) { default: break; }",
            Arrays.asList("switch", "(", "true", ")", "{", "default", ":", "break", ";", "}"),
            Arrays.asList(
                "1-1", "1-8", "1-9", "1-13", "1-15", "1-17", "1-24", "1-26", "1-31", "1-33")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
