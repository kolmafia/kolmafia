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

public class RecordValueTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("standalone new", "new;", "Expected Record name, found ;"),
        invalid("new non-record", "int x = new int();", "'int' is not a record type"),
        valid(
            "new record without parens",
            // Yields a default-constructed record.
            "record r {int a;}; new r;",
            Arrays.asList("record", "r", "{", "int", "a", ";", "}", ";", "new", "r", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-10", "1-11", "1-15", "1-16", "1-17", "1-18", "1-20", "1-24",
                "1-25"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Value.Constant newRecord = assertInstanceOf(Value.Constant.class, commands.get(0));
              assertInstanceOf(TypeInitializer.class, newRecord.value);
              // From the "new" up to the record's name
              ParserTest.assertLocationEquals(1, 20, 1, 25, newRecord.getLocation());
            }),
        invalid(
            "new record with semicolon",
            "record r {int a;}; new r(;",
            "Expression expected for field #1 (a)"),
        valid(
            "new with aggregate field",
            "record r {int[] a;}; new r({1,2});",
            Arrays.asList(
                "record", "r", "{", "int", "[", "]", "a", ";", "}", ";", "new", "r", "(", "{", "1",
                ",", "2", "}", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-10", "1-11", "1-14", "1-15", "1-17", "1-18", "1-19", "1-20",
                "1-22", "1-26", "1-27", "1-28", "1-29", "1-30", "1-31", "1-32", "1-33", "1-34"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Value.Constant newRecord = assertInstanceOf(Value.Constant.class, commands.get(0));
              assertInstanceOf(RecordInitializer.class, newRecord.value);
              // From the "new" up to the parameters's closing ")"
              ParserTest.assertLocationEquals(1, 22, 1, 34, newRecord.getLocation());
            }),
        invalid(
            "new with unexpected aggregate field",
            "record r {int a;}; new r({1,2});",
            "Aggregate literal found when int expected for field #1 (a)"),
        invalid(
            "new with field type mismatch",
            "record r {int a;}; new r('str');",
            "string found when int expected for field #1 (a)"),
        valid(
            "new with void fields",
            "record r {int a; string b; boolean c; int d;}; new r(2, , true);",
            Arrays.asList(
                "record", "r", "{", "int", "a", ";", "string", "b", ";", "boolean", "c", ";", "int",
                "d", ";", "}", ";", "new", "r", "(", "2", ",", ",", "true", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-10", "1-11", "1-15", "1-16", "1-18", "1-25", "1-26", "1-28",
                "1-36", "1-37", "1-39", "1-43", "1-44", "1-45", "1-46", "1-48", "1-52", "1-53",
                "1-54", "1-55", "1-57", "1-59", "1-63", "1-64"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Value.Constant newRecord = assertInstanceOf(Value.Constant.class, commands.get(0));
              RecordInitializer recordValue =
                  assertInstanceOf(RecordInitializer.class, newRecord.value);
              List<Evaluable> params = recordValue.getParams();
              // Zero-width location made at the beginning of the following comma (or parenthesis)
              ParserTest.assertLocationEquals(1, 57, 1, 57, params.get(1).getLocation());
            }),
        invalid(
            "new with too many void fields",
            "record r {int a;}; new r(,,,,,,,);",
            "Too many field initializers for record r"),
        invalid(
            "new without closing paren",
            "record r {int a;}; new r(",
            "Expected ), found end of file"),
        invalid(
            "new without closing paren 2",
            "record r {int a;}; new r(4",
            "Expected , or ), found end of file"),
        invalid(
            "new without closing comma separator",
            "record r {int a; int b;}; new r(4 5)",
            "Expected , or ), found 5"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
