package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;

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
            // "Return needs null value"
            "Cannot return when outside of a function"
            // Arrays.asList( "return", ";" )
            ),
        invalid("no return from int function", "int f() {}", "Missing return value"),
        invalid("return void from int function", "int f() { return; }", "Return needs int value"),
        invalid(
            "return string from int function",
            "int f() { return 'str'; }",
            "Cannot return string value from int function"),
        invalid(
            "return int from void function",
            "void f() { return 1; }",
            "Cannot return a value from a void function"),
        invalid("non-expression return", "int f() { return (); }", "Expression expected"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
