package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TypeDefTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "simple typedef",
            "typedef int number; number foo;",
            Arrays.asList("typedef", "int", "number", ";", "number", "foo", ";"),
            Arrays.asList("1-1", "1-9", "1-13", "1-19", "1-21", "1-28", "1-31"),
            scope -> {
              // Test the definition
              Iterator<Type> types = scope.getTypes().iterator();

              assertTrue(types.hasNext());
              TypeDef number = assertInstanceOf(TypeDef.class, types.next());
              assertFalse(types.hasNext());

              // From the "typedef" up to the new type name
              ParserTest.assertLocationEquals(1, 1, 1, 19, number.getLocation());
              assertEquals(number.getLocation(), number.getDefinitionLocation());

              ParserTest.assertLocationEquals(1, 9, 1, 12, number.getBaseType().getLocation());
              // Primitive types don't have a definition location
              assertNull(number.getBaseType().getDefinitionLocation());

              // Test the reference
              Iterator<Variable> variables = scope.getVariables().iterator();

              assertTrue(variables.hasNext());
              Variable foo = variables.next();
              TypeDef numberReference = assertInstanceOf(TypeDef.class, foo.getType());
              assertFalse(variables.hasNext());

              ParserTest.assertLocationEquals(1, 21, 1, 27, numberReference.getLocation());
              ParserTest.assertLocationEquals(1, 1, 1, 19, numberReference.getDefinitionLocation());
            }),
        valid(
            // Why is this allowed...? This is the only way I could think of to exercise the
            // rewind functionality.
            "Typedef of existing variable",
            "int foo; typedef int foo; (\nfoo\n\n + 2);",
            Arrays.asList(
                "int", "foo", ";", "typedef", "int", "foo", ";", "(", "foo", "+", "2", ")", ";"),
            Arrays.asList(
                "1-1", "1-5", "1-8", "1-10", "1-18", "1-22", "1-25", "1-27", "2-1", "4-2", "4-4",
                "4-5", "4-6")),
        invalid(
            "unterminated typedef",
            "typedef int foo\n foo bar;",
            "Expected ;, found foo",
            "line 2, char 2 to char 5"),
        invalid(
            "typedef with no type",
            "typedef;",
            "Missing data type for typedef",
            "char 1 to char 9"),
        invalid("typedef with no name", "typedef int;", "Type name expected", "char 12 to char 13"),
        invalid(
            "typedef with non-identifier name",
            "typedef int 1;",
            "Invalid type name '1'",
            "char 13 to char 14"),
        invalid(
            "typedef with reserved name",
            "typedef int remove;",
            "Reserved word 'remove' cannot be a type name",
            "char 13 to char 19"),
        invalid(
            "typedef with existing name",
            "typedef float double; typedef int double;",
            "Type name 'double' is already defined",
            "char 35 to char 41"),
        valid(
            "equivalent typedef redefinition",
            "typedef float double; typedef float double;",
            Arrays.asList("typedef", "float", "double", ";", "typedef", "float", "double", ";"),
            Arrays.asList("1-1", "1-9", "1-15", "1-21", "1-23", "1-31", "1-37", "1-43")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
