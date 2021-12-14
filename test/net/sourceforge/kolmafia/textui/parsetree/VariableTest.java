package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class VariableTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "unterminated top-level declaration",
            "int x\nint y",
            "Expected ;, found int",
            "line 2, char 1 to char 4"),
        invalid(
            "type but no declaration",
            "int;",
            "Type given but not used to declare anything",
            "char 1 to char 5"),
        invalid(
            "declaration with non-identifier name",
            "int 1;",
            "Type given but not used to declare anything",
            "char 1 to char 6"),
        invalid(
            "declaration with reserved name",
            "int class;",
            "Reserved word 'class' cannot be a variable name",
            "char 5 to char 10"),
        invalid(
            "multiple-definition",
            "int x = 1; int x = 2;",
            "Variable x is already defined",
            "char 16 to char 17"),
        invalid(
            "variable declaration followed by definition",
            // One might expect this to be acceptable.
            "int x; int x = 1;",
            "Variable x is already defined",
            "char 12 to char 13"),
        valid(
            "simultaneous variable declaration",
            "int a, b = 10, c, d = b;",
            Arrays.asList("int", "a", ",", "b", "=", "10", ",", "c", ",", "d", "=", "b", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-6", "1-8", "1-10", "1-12", "1-14", "1-16", "1-17", "1-19", "1-21",
                "1-23", "1-24"),
            scope -> {
              Iterator<Variable> variables = scope.getVariables().iterator();

              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 5, 1, 6, variables.next().getLocation());
              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 8, 1, 9, variables.next().getLocation());
              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 16, 1, 17, variables.next().getLocation());
              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 19, 1, 20, variables.next().getLocation());
              assertFalse(variables.hasNext());
            }));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
