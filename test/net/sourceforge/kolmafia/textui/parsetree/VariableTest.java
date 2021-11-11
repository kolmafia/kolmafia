package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class VariableTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("unterminated top-level declaration", "int x\nint y", "Expected ;, found int"),
        invalid("type but no declaration", "int;", "Type given but not used to declare anything"),
        invalid(
            "declaration with non-identifier name",
            "int 1;",
            "Type given but not used to declare anything"),
        invalid(
            "declaration with reserved name",
            "int class;",
            "Reserved word 'class' cannot be a variable name"),
        invalid("multiple-definition", "int x = 1; int x = 2;", "Variable x is already defined"),
        invalid(
            "variable declaration followed by definition",
            // One might expect this to be acceptable.
            "int x; int x = 1;",
            "Variable x is already defined"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
