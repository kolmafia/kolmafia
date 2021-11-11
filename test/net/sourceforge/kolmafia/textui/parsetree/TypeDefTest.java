package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class TypeDefTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
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
        invalid("unterminated typedef", "typedef int foo\n foo bar;", "Expected ;, found foo"),
        invalid("typedef with no type", "typedef;", "Missing data type for typedef"),
        invalid("typedef with no name", "typedef int;", "Type name expected"),
        invalid("typedef with non-identifier name", "typedef int 1;", "Invalid type name '1'"),
        invalid(
            "typedef with reserved name",
            "typedef int remove;",
            "Reserved word 'remove' cannot be a type name"),
        invalid(
            "typedef with existing name",
            "typedef float double; typedef int double;",
            "Type name 'double' is already defined"),
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
