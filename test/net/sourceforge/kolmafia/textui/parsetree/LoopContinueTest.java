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

public class LoopContinueTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("continue outside loop", "continue;", "Encountered 'continue' outside of loop"),
        valid(
            "continue inside while-loop",
            "while (true) continue;",
            Arrays.asList("while", "(", "true", ")", "continue", ";"),
            Arrays.asList("1-1", "1-7", "1-8", "1-12", "1-14", "1-22"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              WhileLoop whileLoop = assertInstanceOf(WhileLoop.class, commands.get(0));
              Scope whileScope = whileLoop.getScope();
              LoopContinue loopContinue =
                  assertInstanceOf(LoopContinue.class, whileScope.getCommandList().get(0));
              ParserTest.assertLocationEquals(1, 14, 1, 22, loopContinue.getLocation());
            }),
        invalid(
            "continue inside switch",
            "switch (true) { default: continue; }",
            "Encountered 'continue' outside of loop"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
