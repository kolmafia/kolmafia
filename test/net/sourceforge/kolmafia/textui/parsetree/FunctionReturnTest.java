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

public class FunctionReturnTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            // This has been permitted historically...
            "return outside function",
            "return;",
            // "Return needs null value",
            "Cannot return when outside of a function",
            "char 1 to char 7"),
        // Arrays.asList("return", ";"),
        // Arrays.asList("1-1", "1-7")),
        valid(
            "return void from void function",
            "void f() { return; }",
            Arrays.asList("void", "f", "(", ")", "{", "return", ";", "}"),
            Arrays.asList("1-1", "1-6", "1-7", "1-8", "1-10", "1-12", "1-18", "1-20"),
            scope -> {
              Iterator<Function> functions = scope.getFunctions().iterator();

              assertTrue(functions.hasNext());
              UserDefinedFunction function =
                  assertInstanceOf(UserDefinedFunction.class, functions.next());
              assertFalse(functions.hasNext());

              List<Command> commands = function.getScope().getCommandList();
              FunctionReturn functionReturn =
                  assertInstanceOf(FunctionReturn.class, commands.get(0));
              // Only the "return"
              ParserTest.assertLocationEquals(1, 12, 1, 18, functionReturn.getLocation());
            }),
        valid(
            "return int from int function",
            "int f() { return 1 + 1; }",
            Arrays.asList("int", "f", "(", ")", "{", "return", "1", "+", "1", ";", "}"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-7", "1-9", "1-11", "1-18", "1-20", "1-22", "1-23", "1-25"),
            scope -> {
              Iterator<Function> functions = scope.getFunctions().iterator();

              assertTrue(functions.hasNext());
              UserDefinedFunction function =
                  assertInstanceOf(UserDefinedFunction.class, functions.next());
              assertFalse(functions.hasNext());

              List<Command> commands = function.getScope().getCommandList();
              FunctionReturn functionReturn =
                  assertInstanceOf(FunctionReturn.class, commands.get(0));
              // From the "return" up to the end of the expression (without the semi-colon)
              ParserTest.assertLocationEquals(1, 11, 1, 23, functionReturn.getLocation());
            }),
        invalid(
            "no return from int function",
            "int f() {}",
            "Missing return value",
            "char 5 to char 8"),
        invalid(
            "return void from int function",
            "int f() { return; }",
            "Return needs int value",
            "char 11 to char 17"),
        invalid(
            "return string from int function",
            "int f() { return 'str'; }",
            "Cannot return string value from int function",
            "char 18 to char 23"),
        invalid(
            "return int from void function",
            "void f() { return 1; }",
            "Cannot return a value from a void function",
            "char 19 to char 20"),
        invalid(
            "non-expression return",
            "int f() { return ?; }",
            "Expression expected",
            "char 18 to char 19"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
