package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class NumericValueTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Numeric literal split after negative",
            "-/*negative \nnumber*/1.23;",
            Arrays.asList("-", "/*negative", "number*/", "1", ".", "23", ";"),
            Arrays.asList("1-1", "1-2", "2-1", "2-9", "2-10", "2-11", "2-13")),
        valid(
            "Float literal split after decimal",
            "1./*decimal\n*/23;",
            Arrays.asList("1", ".", "/*decimal", "*/", "23", ";"),
            Arrays.asList("1-1", "1-2", "1-3", "2-1", "2-3", "2-5")),
        valid(
            "Float literal with no integral component",
            "-.123;",
            Arrays.asList("-", ".", "123", ";"),
            Arrays.asList("1-1", "1-2", "1-3", "1-6")),
        /*
        There's code for this case, but we encounter a separate error ("Record expected").
        valid(
            "Float literal with no decimal component",
            "123.;",
            Arrays.asList( "123", ".", ";" )),
        */
        invalid(
            "Float literal with no integral part, non-numeric fractional part",
            "-.123abc;",
            "Expected numeric value, found 123abc"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
