package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RecordTypeTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("record with no body", "record a;", "Expected {, found ;", "char 9 to char 10"),
        invalid(
            "record with no name or body", "record;", "Record name expected", "char 7 to char 8"),
        invalid(
            "record with non-identifier name",
            "record 1 { int a;};",
            "Invalid record name '1'",
            "char 8 to char 9"),
        invalid(
            "record with reserved name",
            "record int { int a;};",
            "Reserved word 'int' cannot be a record name",
            "char 8 to char 11"),
        invalid(
            "record redefinition",
            "record a { int a;}; record a { int a;};",
            "Record name 'a' is already defined",
            "char 28 to char 29"),
        invalid(
            "record without fields",
            "record a {};",
            "Record field(s) expected",
            "char 11 to char 12"),
        invalid(
            "record with void field type",
            "record a { void a;};",
            "Non-void field type expected",
            "char 12 to char 16"),
        invalid(
            "record with unknown field type",
            "record a { foo a;};",
            "Type name expected",
            "char 12 to char 15"),
        invalid(
            "record with no field name",
            "record a { int;};",
            "Field name expected",
            "char 15 to char 16"),
        invalid(
            "record with non-identifier field name",
            "record a { int 1;};",
            "Invalid field name '1'",
            "char 16 to char 17"),
        invalid(
            "record with reserved field name",
            "record a { int class;};",
            "Reserved word 'class' cannot be used as a field name",
            "char 16 to char 21"),
        invalid(
            "record with repeated field name",
            "record a { int b; int b;};",
            "Field name 'b' is already defined",
            "char 23 to char 24"),
        invalid(
            "record with missing semicolon",
            "record a { int b\n float c;};",
            "Expected ;, found float",
            "line 2, char 2 to char 7"),
        invalid(
            "unterminated record", "record a { int b;", "Expected }, found end of file", "char 18"),
        invalid(
            "Illegal record creation",
            "void f( record foo {int a; int b;} bar )",
            "Existing type expected for function parameter",
            "char 9 to char 35"),
        valid(
            "array of record",
            "record {int a;}[] r;",
            Arrays.asList("record", "{", "int", "a", ";", "}", "[", "]", "r", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-9", "1-13", "1-14", "1-15", "1-16", "1-17", "1-19", "1-20"),
            scope -> {
              Iterator<Variable> variables = scope.getVariables().iterator();

              assertTrue(variables.hasNext());
              AggregateType array =
                  assertInstanceOf(AggregateType.class, variables.next().getType());
              RecordType record = assertInstanceOf(RecordType.class, array.getDataType());
              // From the "record" up to the closing "}"
              ParserTest.assertLocationEquals(1, 1, 1, 16, record.getLocation());
              // Its creation was also its reference
              assertEquals(record.getLocation(), record.getDefinitionLocation());
              assertFalse(variables.hasNext());
            }));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
