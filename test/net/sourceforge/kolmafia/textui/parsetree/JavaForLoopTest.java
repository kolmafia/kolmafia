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

public class JavaForLoopTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("Unterminated Java for-loop", "for (", "Expected ), found end of file", "char 6"),
        invalid(
            "Java for-loop, bad incrementer",
            "for (;;1)",
            "Variable reference expected",
            "char 8 to char 9"),
        valid(
            "valid empty Java for-loop",
            "for (;;) {}",
            Arrays.asList("for", "(", ";", ";", ")", "{", "}"),
            Arrays.asList("1-1", "1-5", "1-6", "1-7", "1-8", "1-10", "1-11"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Scope location test
              JavaForLoop forLoop = assertInstanceOf(JavaForLoop.class, commands.get(0));
              Scope loopScope = forLoop.getScope();
              ParserTest.assertLocationEquals(1, 10, 1, 12, loopScope.getLocation());

              // Implicit value location test
              Value.Constant condition =
                  assertInstanceOf(Value.Constant.class, forLoop.getCondition());
              // Zero-width location made at the beginning of the semi-colon
              ParserTest.assertLocationEquals(1, 7, 1, 7, condition.getLocation());
            }),
        valid(
            "Java for-loop with new variable",
            "for (int i = 0; i < 5; ++i) {}",
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "++", "i", ")", "{",
                "}"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-12", "1-14", "1-15", "1-17", "1-19", "1-21", "1-22",
                "1-24", "1-26", "1-27", "1-29", "1-30")),
        valid(
            "javaFor with multiple declarations",
            "for (int i=0, int length=5; i < length; i++);",
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ",", "int", "length", "=", "5", ";", "i", "<",
                "length", ";", "i", "++", ")", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-11", "1-12", "1-13", "1-15", "1-19", "1-25", "1-26",
                "1-27", "1-29", "1-31", "1-33", "1-39", "1-41", "1-42", "1-44", "1-45"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              JavaForLoop forLoop = assertInstanceOf(JavaForLoop.class, commands.get(0));
              Scope loopScope = forLoop.getScope();
              Iterator<Variable> variables = loopScope.getVariables().iterator();

              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 10, 1, 11, variables.next().getLocation());
              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 19, 1, 25, variables.next().getLocation());
              assertFalse(variables.hasNext());
            }),
        invalid(
            "javaFor with empty initializer",
            "for (int i=0,; i < 5; ++i);",
            "Identifier expected",
            "char 14 to char 15"),
        valid(
            "javaFor with compound increment",
            "for (int i=0; i < 5; i+=1);",
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "i", "+=", "1", ")",
                ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-11", "1-12", "1-13", "1-15", "1-17", "1-19", "1-20",
                "1-22", "1-23", "1-25", "1-26", "1-27")),
        valid(
            "javaFor with existing variable",
            "int i; for (i=0; i < 5; i++);",
            Arrays.asList(
                "int", "i", ";", "for", "(", "i", "=", "0", ";", "i", "<", "5", ";", "i", "++", ")",
                ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-8", "1-12", "1-13", "1-14", "1-15", "1-16", "1-18", "1-20",
                "1-22", "1-23", "1-25", "1-26", "1-28", "1-29"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Loop location test
              JavaForLoop forLoop = assertInstanceOf(JavaForLoop.class, commands.get(1));
              // From the "for" up to the end of its scope
              ParserTest.assertLocationEquals(1, 8, 1, 30, forLoop.getLocation());

              // Scope location test
              Scope loopScope = forLoop.getScope();
              ParserTest.assertLocationEquals(1, 29, 1, 30, loopScope.getLocation());

              // Variable location test
              Iterator<Variable> variables = loopScope.getVariables().iterator();
              assertFalse(variables.hasNext());

              variables = loopScope.getParentScope().getVariables().iterator();
              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 5, 1, 6, variables.next().getLocation());
              assertFalse(variables.hasNext());
            }),
        invalid(
            "javaFor with unknown existing variable",
            "for (i=0; i < 5; i++);",
            "Unknown variable 'i'",
            "char 6 to char 7"),
        invalid(
            "javaFor with redefined existing variable",
            "int i; for (int i=0; i < 5; i++);",
            "Variable 'i' already defined",
            "char 17 to char 18"),
        invalid(
            "javaFor with not-an-increment",
            "for (int i=0; i < 5; i==1);",
            "Variable 'i' not incremented",
            "char 22 to char 23"),
        valid(
            "javaFor with constant assignment",
            // I guess this technically works... but will be an infinite
            // loop in practice.
            "for (int i=0; i < 5; i=1);",
            Arrays.asList(
                "for", "(", "int", "i", "=", "0", ";", "i", "<", "5", ";", "i", "=", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-11", "1-12", "1-13", "1-15", "1-17", "1-19", "1-20",
                "1-22", "1-23", "1-24", "1-25", "1-26")),
        invalid(
            "javaFor missing initial identifier",
            "for (0; i < 5; i++);",
            "Identifier required",
            "char 6 to char 7"),
        invalid(
            "javaFor missing initializer expression",
            "for (int i =; i < 5; i++);",
            "Expression expected",
            "char 13 to char 14"),
        invalid(
            "javaFor invalid assignment",
            "for (int i ='abc'; i < 5; i++);",
            "Cannot store string in i of type int",
            "char 13 to char 18"),
        invalid(
            "javaFor null condition", // "nothing" is evaluated as true, so we have to get creative
            "for (int i; ?; i++);",
            "Expression expected",
            "char 13 to char 14"),
        invalid(
            "javaFor non-boolean condition",
            "for (int i; i + 5; i++);",
            "\"for\" requires a boolean conditional expression",
            "char 13 to char 18",
            "for (; i; i++);",
            "Unknown variable 'i'",
            "char 8 to char 9"),
        valid(
            "javaFor multiple increments",
            "for (int i, int j; i + j < 5; i++, j++);",
            Arrays.asList(
                "for", "(", "int", "i", ",", "int", "j", ";", "i", "+", "j", "<", "5", ";", "i",
                "++", ",", "j", "++", ")", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-10", "1-11", "1-13", "1-17", "1-18", "1-20", "1-22", "1-24",
                "1-26", "1-28", "1-29", "1-31", "1-32", "1-34", "1-36", "1-37", "1-39", "1-40")),
        invalid(
            "javaFor interrupted multiple increments",
            "for (int i; i < 5; i++,);",
            "Identifier expected",
            "char 24 to char 25"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
