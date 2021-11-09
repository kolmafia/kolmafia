package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;

import java.util.Arrays;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RecordTypeTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("record with no body", "record a;", "Expected {, found ;"),
        invalid("record with no name or body", "record;", "Record name expected"),
        invalid(
            "record with non-identifier name", "record 1 { int a;};", "Invalid record name '1'"),
        invalid(
            "record with reserved name",
            "record int { int a;};",
            "Reserved word 'int' cannot be a record name"),
        invalid(
            "record redefinition",
            "record a { int a;}; record a { int a;};",
            "Record name 'a' is already defined"),
        invalid("record without fields", "record a {};", "Record field(s) expected"),
        invalid(
            "record with void field type", "record a { void a;};", "Non-void field type expected"),
        invalid("record with unknown field type", "record a { foo a;};", "Type name expected"),
        invalid("record with no field name", "record a { int;};", "Field name expected"),
        invalid(
            "record with non-identifier field name",
            "record a { int 1;};",
            "Invalid field name '1'"),
        invalid(
            "record with reserved field name",
            "record a { int class;};",
            "Reserved word 'class' cannot be used as a field name"),
        invalid(
            "record with repeated field name",
            "record a { int b; int b;};",
            "Field name 'b' is already defined"),
        invalid(
            "record with missing semicolon",
            "record a { int b\n float c;};",
            "Expected ;, found float"),
        invalid("unterminated record", "record a { int b;", "Expected }, found end of file"),
        invalid(
            "Illegal record creation",
            "void f( record foo {int a; int b;} bar )",
            "Existing type expected for function parameter"),
        valid(
            "array of record",
            "record {int a;}[] r;",
            Arrays.asList("record", "{", "int", "a", ";", "}", "[", "]", "r", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-9", "1-13", "1-14", "1-15", "1-16", "1-17", "1-19", "1-20")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
