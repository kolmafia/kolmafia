package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class UserDefinedFunctionTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Basic function with one argument",
            "void f(int a) {}",
            Arrays.asList("void", "f", "(", "int", "a", ")", "{", "}"),
            Arrays.asList("1-1", "1-6", "1-7", "1-8", "1-12", "1-13", "1-15", "1-16")),
        valid(
            "Basic function with forward reference",
            "void foo(); void bar() { foo(); } void foo() { return; } bar();",
            Arrays.asList(
                "void", "foo", "(", ")", ";", "void", "bar", "(", ")", "{", "foo", "(", ")", ";",
                "}", "void", "foo", "(", ")", "{", "return", ";", "}", "bar", "(", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-11", "1-13", "1-18", "1-21", "1-22", "1-24", "1-26",
                "1-29", "1-30", "1-31", "1-33", "1-35", "1-40", "1-43", "1-44", "1-46", "1-48",
                "1-54", "1-56", "1-58", "1-61", "1-62", "1-63"),
            scope -> {
              Iterator<Function> functions = scope.getFunctions().iterator();

              // Functions are returned alphabetically, so bar() is first
              assertTrue(functions.hasNext());
              // From the function's name (*not* including the type) to the end of the parameters
              ParserTest.assertLocationEquals(1, 18, 1, 23, functions.next().getLocation());
              assertTrue(functions.hasNext());
              ParserTest.assertLocationEquals(1, 6, 1, 11, functions.next().getLocation());
              assertFalse(functions.hasNext());
            }),
        invalid(
            "Invalid function name",
            "void float() {}",
            "Reserved word 'float' cannot be used as a function name"),
        invalid("Basic function interrupted", "void f(", "Expected ), found end of file"),
        invalid(
            "Basic function parameter interrupted",
            "void f(int",
            "Expected identifier, found end of file"),
        invalid(
            "Basic function duplicate parameter",
            "void f(int a, float a) {}",
            "Parameter a is already defined"),
        invalid(
            "Basic function missing parameter separator",
            "void f(int a float a) {}",
            "Expected ,, found float"), // Uh... doesn't quite stand out does it...
        valid(
            "Basic function with vararg",
            "void f(int a, string b, float... c) {} f(5, 'foo', 1.2, 6.3, 4.9, 10, -0)",
            Arrays.asList(
                "void", "f", "(", "int", "a", ",", "string", "b", ",", "float", "...", "c", ")",
                "{", "}", "f", "(", "5", ",", "'foo'", ",", "1", ".", "2", ",", "6", ".", "3", ",",
                "4", ".", "9", ",", "10", ",", "-", "0", ")"),
            Arrays.asList(
                "1-1", "1-6", "1-7", "1-8", "1-12", "1-13", "1-15", "1-22", "1-23", "1-25", "1-30",
                "1-34", "1-35", "1-37", "1-38", "1-40", "1-41", "1-42", "1-43", "1-45", "1-50",
                "1-52", "1-53", "1-54", "1-55", "1-57", "1-58", "1-59", "1-60", "1-62", "1-63",
                "1-64", "1-65", "1-67", "1-69", "1-71", "1-72", "1-73"),
            scope -> {
              Iterator<Function> functions = scope.getFunctions().iterator();

              assertTrue(functions.hasNext());
              UserDefinedFunction f = assertInstanceOf(UserDefinedFunction.class, functions.next());
              assertFalse(functions.hasNext());

              // VariableReference location test
              List<VariableReference> parameters = f.getVariableReferences();
              VariableReference a = parameters.get(0);
              ParserTest.assertLocationEquals(1, 12, 1, 13, a.getLocation());
              VariableReference b = parameters.get(1);
              ParserTest.assertLocationEquals(1, 22, 1, 23, b.getLocation());
              VariableReference c = parameters.get(2);
              ParserTest.assertLocationEquals(1, 34, 1, 35, c.getLocation());

              // VarArgType location test
              VarArgType varArg = assertInstanceOf(VarArgType.class, c.getType());
              // The type's location + the three dots (here: "float...")
              ParserTest.assertLocationEquals(1, 25, 1, 33, varArg.getLocation());
              // VarArgs don't have a "definition"
              assertNull(varArg.getDefinitionLocation());
            }),
        invalid(
            "Basic function with multiple varargs",
            "void f(int ... a, int ... b) {}",
            "Only one vararg parameter is allowed"),
        invalid(
            "Basic function with non-terminal vararg",
            "void f(int ... a, float b) {}",
            "The vararg parameter must be the last one"),
        invalid(
            "Basic function overrides library",
            "void round(float n) {}",
            "Function 'round(float)' overrides a library function."),
        invalid(
            "Basic function defined multiple times",
            "void f() {} void f() {}",
            "Function 'f()' defined multiple times."),
        invalid(
            "Basic function vararg clash",
            "void f(int a, int ... b) {} void f(int ... a) {}",
            "Function 'f(int ...)' clashes with existing function 'f(int, int ...)'."),
        invalid(
            "inner main", "void foo() { void main() {} }", "main method must appear at top level"),
        invalid(
            "parameter initialization", "int f(int x = 1) {}", "Cannot initialize parameter x"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
