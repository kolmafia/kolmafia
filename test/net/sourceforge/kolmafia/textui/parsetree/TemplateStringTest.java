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

public class TemplateStringTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "Unterminated string template",
            "`{`",
            // The parser tries to start a new string template inside the expression
            "No closing ` found"),
        invalid(
            "Abruptly unterminated string template, expecting expression",
            "`{",
            "Expression expected"),
        invalid(
            "Abruptly unterminated string template, parsed expression",
            "`{1",
            "Expected }, found end of file"),
        valid(
            "basic template string",
            "`this is some math: {4 + 7}`",
            Arrays.asList("`this is some math: {", "4", "+", "7", "}`"),
            Arrays.asList("1-1", "1-22", "1-24", "1-26", "1-27"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Concatenate location test
              Concatenate conc = assertInstanceOf(Concatenate.class, commands.get(0));
              ParserTest.assertLocationEquals(1, 1, 1, 29, conc.getLocation());
            }),
        invalid(
            "template string with a new variable",
            "`this is some math: {int x = 4; x + 7}`",
            "Unknown variable 'int'"),
        valid(
            "template string with predefined variable",
            "int x; `this is some math: {(x = 4) + 7}`",
            Arrays.asList(
                "int", "x", ";", "`this is some math: {", "(", "x", "=", "4", ")", "+", "7", "}`"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-8", "1-29", "1-30", "1-32", "1-34", "1-35", "1-37", "1-39",
                "1-40")),
        invalid(
            "template string with unclosed comment",
            "`this is some math: {7 // what determines the end?}`",
            "Expected }, found end of file"),
        valid(
            "template string with terminated comment",
            "`this is some math: {7 // turns out you need a newline\r\n\t}`",
            Arrays.asList(
                "`this is some math: {", "7",
                "// turns out you need a newline", "}`"),
            Arrays.asList(
                "1-1", "1-22",
                "1-24", "2-2")),
        valid(
            "template string with multiple templates",
            "`{'hello'} {'world'}`",
            Arrays.asList("`{", "'hello'", "} {", "'world'", "}`"),
            Arrays.asList("1-1", "1-3", "1-10", "1-13", "1-20")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
