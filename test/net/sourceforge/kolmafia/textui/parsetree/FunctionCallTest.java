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

public class FunctionCallTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Standard function call.",
            "to_string( 123 );",
            Arrays.asList("to_string", "(", "123", ")", ";"),
            Arrays.asList("1-1", "1-10", "1-12", "1-16", "1-17"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              FunctionCall call = assertInstanceOf(FunctionCall.class, commands.get(0));
              // From the function name to the end of the parameters' parenthesis
              ParserTest.assertLocationEquals(1, 1, 1, 17, call.getLocation());
            }),
        valid(
            "Int literal with method call.",
            "123.to_string();",
            Arrays.asList("123", ".", "to_string", "(", ")", ";"),
            Arrays.asList("1-1", "1-4", "1-5", "1-14", "1-15", "1-16"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              FunctionCall call = assertInstanceOf(FunctionCall.class, commands.get(0));
              // From the first parameter to the end of the parameters' parenthesis
              ParserTest.assertLocationEquals(1, 1, 1, 16, call.getLocation());
            }),
        invalid(
            "undefined function call",
            "prin();",
            "Function 'prin( )' undefined.  This script may require a more recent version of KoLmafia and/or its supporting scripts."),
        invalid("function call interrupted", "print(", "Expected ), found end of file"),
        invalid(
            "function call interrupted after comma",
            "print(1,",
            "Expected parameter, found end of file"),
        invalid("function call closed after comma", "print(1,);", "Expected parameter, found )"),
        invalid(
            "function call interrupted after param", "print(1", "Expected ), found end of file"),
        invalid("function call with non-comma separator", "print(1; 2);", "Expected ), found ;"),
        valid(
            "method call of primitive var",
            "string x = 'hello'; x.print();",
            Arrays.asList("string", "x", "=", "'hello'", ";", "x", ".", "print", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-10", "1-12", "1-19", "1-21", "1-22", "1-23", "1-28", "1-29",
                "1-30")),
        valid(
            "method call of aggregate index",
            "string[2] x; x[0].print();",
            Arrays.asList(
                "string", "[", "2", "]", "x", ";", "x", "[", "0", "]", ".", "print", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-7", "1-8", "1-9", "1-11", "1-12", "1-14", "1-15", "1-16", "1-17", "1-18",
                "1-19", "1-24", "1-25", "1-26")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
