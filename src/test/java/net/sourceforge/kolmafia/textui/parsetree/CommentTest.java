package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class CommentTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Mid-line // comment",
            "int x = // interrupting comment\n  5;",
            Arrays.asList("int", "x", "=", "// interrupting comment", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "2-3", "2-4")),
        valid(
            "Empty mid-line // comment",
            "int x = //\n  5;",
            Arrays.asList("int", "x", "=", "//", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "2-3", "2-4")),
        valid(
            "Mid-line # comment",
            // This ought to only accept full-line comments, but it's incorrectly implemented,
            // and at this point, widely used enough that this isn't feasible to change.
            "int x = # interrupting comment\n  5;",
            Arrays.asList("int", "x", "=", "# interrupting comment", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "2-3", "2-4")),
        valid(
            "Empty mid-line # comment",
            "int x = #\n  5;",
            Arrays.asList("int", "x", "=", "#", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "2-3", "2-4")),
        valid(
            "Multiline comment",
            "int x =/* this\n    is a comment\n   */ 5;",
            // Note that this drops some leading whitespace.
            Arrays.asList("int", "x", "=", "/* this", "is a comment", "*/", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-8", "2-5", "3-4", "3-7", "3-8")),
        valid(
            "Empty multiline comment",
            "int x =/*\n\n*/ 5;",
            Arrays.asList("int", "x", "=", "/*", "*/", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-8", "3-1", "3-4", "3-5")),
        valid(
            "Multiline comment on one line",
            "int x =/* this is a comment */ 5;",
            Arrays.asList("int", "x", "=", "/* this is a comment */", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-8", "1-32", "1-33")),
        valid(
            "Empty multiline comment on one line",
            "int x =/**/ 5;",
            Arrays.asList("int", "x", "=", "/**/", "5", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-8", "1-13", "1-14")),
        invalid(
            "Empty multiline comment on one line, single asterisk",
            "int x =/*/ 5;",
            "Expression expected",
            "char 14"),
        valid(
            "Script with bom",
            "\ufeff    'hello world'",
            Arrays.asList("'hello world'"),
            Arrays.asList("1-6")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
