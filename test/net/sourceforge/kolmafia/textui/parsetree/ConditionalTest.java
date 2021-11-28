package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
                "1-58", "1-59"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Conditional location test - whole
              Conditional conditional = assertInstanceOf(If.class, commands.get(0));
              // From the "if" up to the end of the last conditional's scope
              ParserTest.assertLocationEquals(1, 1, 1, 60, conditional.getLocation());

              // Conditional location test - If
              // Currently unavailable
              // From the "if" up to the end of the first conditional's scope
              // ParserTest.assertLocationEquals(1, 1, 1, 14, conditional.getLocation());

              // Scope location test - If
              Scope conditionalScope = conditional.getScope();
              ParserTest.assertLocationEquals(1, 12, 1, 14, conditionalScope.getLocation());

              // Conditional location test - ElseIf 1
              Iterator<Conditional> elseConditionals = ((If) conditional).getElseLoopIterator();
              assertTrue(elseConditionals.hasNext());
              conditional = assertInstanceOf(ElseIf.class, elseConditionals.next());
              // From the first "else" up to the end of the second conditional's scope
              ParserTest.assertLocationEquals(1, 15, 1, 33, conditional.getLocation());

              // Scope location test - ElseIf 1
              conditionalScope = conditional.getScope();
              ParserTest.assertLocationEquals(1, 31, 1, 33, conditionalScope.getLocation());

              // Conditional location test - ElseIf 2
              assertTrue(elseConditionals.hasNext());
              conditional = assertInstanceOf(ElseIf.class, elseConditionals.next());
              // From the second "else" up to the end of the third conditional's scope
              ParserTest.assertLocationEquals(1, 34, 1, 52, conditional.getLocation());

              // Scope location test - ElseIf 2
              conditionalScope = conditional.getScope();
              ParserTest.assertLocationEquals(1, 50, 1, 52, conditionalScope.getLocation());

              // Conditional location test - Else
              assertTrue(elseConditionals.hasNext());
              conditional = assertInstanceOf(Else.class, elseConditionals.next());
              assertFalse(elseConditionals.hasNext());
              // From the third "else" up to the end of the fourth conditional's scope
              ParserTest.assertLocationEquals(1, 53, 1, 60, conditional.getLocation());

              // Scope location test - Else
              conditionalScope = conditional.getScope();
              ParserTest.assertLocationEquals(1, 58, 1, 60, conditionalScope.getLocation());

              // Implicit value location test
              Value.Constant condition =
                  assertInstanceOf(Value.Constant.class, conditional.getCondition());
              // Zero-width location made at the beginning of the token following the "else"
              ParserTest.assertLocationEquals(1, 58, 1, 58, condition.getLocation());
            }),
        invalid(
            "Multiple else",
            "if (false) {} else {} else {}",
            "Else without if",
            "char 23 to char 27"),
        invalid(
            "else-if after else",
            "if (false) {} else {} else if (true) {}",
            "Else without if",
            "char 23 to char 27"),
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
        invalid("if without condition", "if true", "Expected (, found true", "char 4 to char 8"),
        invalid("if with empty condition", "if ()", "Expression expected", "char 5 to char 6"),
        invalid(
            "if with incorrect condition",
            "if (1)",
            "\"if\" requires a boolean conditional expression",
            "char 5 to char 6"),
        invalid(
            "if with unclosed condition", "if (true", "Expected ), found end of file", "char 9"),
        // These probably shouldn't need to be separate test cases...
        invalid(
            "else if without condition",
            "if (false); else if true",
            "Expected (, found true",
            "char 21 to char 25"),
        invalid(
            "else if with empty condition",
            "if (false); else if ()",
            "Expression expected",
            "char 22 to char 23"),
        invalid(
            "else if with incorrect condition",
            "if (false); else if (2)",
            "\"if\" requires a boolean conditional expression",
            "char 22 to char 23"),
        invalid(
            "else if with unclosed condition",
            "if (false); else if (true",
            "Expected ), found end of file",
            "char 26"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
