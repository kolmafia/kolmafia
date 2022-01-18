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

public class StaticScopeTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "single static declaration",
            "static int x;",
            Arrays.asList("static", "int", "x", ";"),
            Arrays.asList("1-1", "1-8", "1-12", "1-13")),
        valid(
            "single static definition",
            "static int x = 1;",
            Arrays.asList("static", "int", "x", "=", "1", ";"),
            Arrays.asList("1-1", "1-8", "1-12", "1-14", "1-16", "1-17")),
        valid(
            "multiple static definition",
            "static int x = 1, y = 2;",
            Arrays.asList("static", "int", "x", "=", "1", ",", "y", "=", "2", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-12", "1-14", "1-16", "1-17", "1-19", "1-21", "1-23", "1-24"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              StaticScope staticScope = assertInstanceOf(StaticScope.class, commands.get(0));
              ParserTest.assertLocationEquals(1, 1, 1, 25, staticScope.getLocation());
            }),
        invalid(
            "static type but no declaration",
            "static int;",
            "Type given but not used to declare anything",
            "char 8 to char 12"),
        valid(
            "static command",
            "static print('hello world');",
            Arrays.asList("static", "print", "(", "'hello world'", ")", ";"),
            Arrays.asList("1-1", "1-8", "1-13", "1-14", "1-27", "1-28")),
        valid(
            "simple static block",
            "static { int x; }",
            Arrays.asList("static", "{", "int", "x", ";", "}"),
            Arrays.asList("1-1", "1-8", "1-10", "1-14", "1-15", "1-17"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              StaticScope staticScope = assertInstanceOf(StaticScope.class, commands.get(0));
              ParserTest.assertLocationEquals(1, 1, 1, 18, staticScope.getLocation());
            }),
        invalid(
            "unterminated static declaration",
            "static int x\nint y;",
            "Expected ;, found int",
            "line 2, char 1 to char 4"),
        invalid("unterminated static block", "static {", "Expected }, found end of file", "char 9"),
        invalid("lone static", "static;", "command or declaration required", "char 7 to char 8"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
