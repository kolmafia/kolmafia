package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;

import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RecordLiteralTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "Record fields expected",
            "record r {int x;} rec = {}",
            "Record field(s) expected",
            "char 26 to char 27"),
        invalid(
            "Valid field name expected",
            "record r {int x;} rec = {123",
            "Invalid field name '123'",
            "char 26 to char 29"),
        invalid(
            "Record field name expected",
            "record r {int x;} rec = {:1}",
            "Field name expected",
            "char 26 to char 27"),
        invalid(
            "Defined record fields expected",
            "record r {int x;} rec = {y:12}",
            "Field name 'y' is not valid",
            "char 26 to char 27"),
        invalid(
            "Non-duplicate record fields expected",
            "record r {int x;} rec = {x:1, x:2}",
            "Field name 'x' is already set",
            "char 31 to char 32"),
        invalid(
            "Colon expected",
            "record r {int x;} rec = {x}",
            "Expected :, found }",
            "char 27 to char 28"),
        invalid(
            "Value expected",
            "record r {int x;} rec = {x:}",
            "Script parsing error; couldn't figure out value of record field value",
            "char 28 to char 29"),
        invalid(
            "Coercable value expected",
            "record r {int x;} rec = {x:\"foo\"}",
            "Invalid record literal; cannot assign value of type string to field of type int",
            "char 28 to char 33"),
        invalid(
            "Comma or } expected",
            "record r {int x;} rec = {x:1 y}",
            "Expected }, found y",
            "char 30 to char 31"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
