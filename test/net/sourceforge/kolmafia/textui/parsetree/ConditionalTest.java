package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ConditionalTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Chained if/else-if/else",
            "if (false) {} else if (false) {} else if (false) {} else {}",
            Arrays.asList(
                "if", "(", "false", ")", "{", "}", "else", "if", "(", "false", ")", "{", "}",
                "else", "if", "(", "false", ")", "{", "}", "else", "{", "}"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-10", "1-12", "1-13", "1-15", "1-20", "1-23", "1-24", "1-29",
                "1-31", "1-32", "1-34", "1-39", "1-42", "1-43", "1-48", "1-50", "1-51", "1-53",
                "1-58", "1-59")),
        invalid("Multiple else", "if (false) {} else {} else {}", "Else without if"),
        invalid("else-if after else", "if (false) {} else {} else if (true) {}", "Else without if"),
        invalid("else without if", "else {}", "Unknown variable 'else'"),
        valid(
            "single-command if",
            "if (true) print('msg');",
            Arrays.asList("if", "(", "true", ")", "print", "(", "'msg'", ")", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-9", "1-11", "1-16", "1-17", "1-22", "1-23")),
        valid(
            "empty if",
            "if (true);",
            Arrays.asList("if", "(", "true", ")", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-9", "1-10")),
        invalid("if without condition", "if true", "Expected (, found true"),
        invalid("if with empty condition", "if ()", "\"if\" requires a boolean condition"),
        invalid("if with numeric condition", "if (1)", "\"if\" requires a boolean condition"),
        invalid("if with unclosed condition", "if (true", "Expected ), found end of file"),
        // These probably shouldn't need to be separate test cases...
        invalid("else if without condition", "if (false); else if true", "Expected (, found true"),
        invalid(
            "else if with empty condition",
            "if (false); else if ()",
            "\"if\" requires a boolean condition"),
        invalid(
            "else if with unclosed condition",
            "if (false); else if (true",
            "Expected ), found end of file"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
