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

public class SwitchTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid(
            "switch without condition or block",
            "switch true {}",
            "Expected ( or {, found true",
            "char 8 to char 12"),
        invalid(
            "switch with empty condition",
            "switch ()",
            "\"switch ()\" requires an expression",
            "char 9 to char 10"),
        invalid(
            "switch with unclosed condition",
            "switch (true",
            "Expected ), found end of file",
            "char 13"),
        invalid(
            "switch with condition but no block",
            "switch (true)",
            "Expected {, found end of file",
            "char 14"),
        invalid(
            "switch with condition but unclosed block",
            "switch (true) {",
            "Expected }, found end of file",
            "char 16"),
        valid(
            "switch with block and no condition",
            "switch { }",
            Arrays.asList("switch", "{", "}"),
            Arrays.asList("1-1", "1-8", "1-10")),
        valid(
            "switch with block, non-const label",
            "boolean x; switch { case x: }",
            Arrays.asList("boolean", "x", ";", "switch", "{", "case", "x", ":", "}"),
            Arrays.asList("1-1", "1-9", "1-10", "1-12", "1-19", "1-21", "1-26", "1-27", "1-29")),
        valid(
            "switch with block, label expression",
            "boolean x; switch { case !x: }",
            Arrays.asList("boolean", "x", ";", "switch", "{", "case", "!", "x", ":", "}"),
            Arrays.asList(
                "1-1", "1-9", "1-10", "1-12", "1-19", "1-21", "1-26", "1-27", "1-28", "1-30")),
        valid(
            "switch with block, nested variable",
            "switch { case true: int x; }",
            Arrays.asList("switch", "{", "case", "true", ":", "int", "x", ";", "}"),
            Arrays.asList("1-1", "1-8", "1-10", "1-15", "1-19", "1-21", "1-25", "1-26", "1-28"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Switch location test
              Switch switchCommand = assertInstanceOf(Switch.class, commands.get(0));
              // From the "switch" up to the end of its scope
              ParserTest.assertLocationEquals(1, 1, 1, 29, switchCommand.getLocation());

              // Scope location test
              SwitchScope switchScope = switchCommand.getScope();
              ParserTest.assertLocationEquals(1, 8, 1, 29, switchScope.getLocation());

              // Implicit value location test
              Value.Constant condition =
                  assertInstanceOf(Value.Constant.class, switchCommand.getCondition());
              // Zero-width location made at the beginning of the token following the "switch"
              ParserTest.assertLocationEquals(1, 8, 1, 8, condition.getLocation());
            }),
        invalid(
            "switch with block, nested type but no variable",
            "switch { case true: int; }",
            "Type given but not used to declare anything",
            "char 21 to char 25"),
        invalid(
            "switch with block, nested variable but missing semicolon",
            "switch { case true: int x }",
            "Expected ;, found }",
            "char 27 to char 28"),
        invalid(
            "switch, case label without expression",
            "switch { case: }",
            "Case label needs to be followed by an expression",
            "char 14 to char 15"),
        invalid(
            "switch case not terminated by colon",
            "switch { case true; }",
            "Expected :, found ;",
            "char 19 to char 20"),
        invalid(
            "switch default not terminated by colon",
            "switch { default true; }",
            "Expected :, found true",
            "char 18 to char 22"),
        invalid(
            "switch type mismatch",
            "switch (1) { case true: }",
            "Switch conditional has type int but label expression has type boolean",
            "char 19 to char 23"),
        invalid(
            "switch block type mismatch",
            // Note that the implicit switch type is boolean here.
            "switch { case 1: }",
            "Switch conditional has type boolean but label expression has type int",
            "char 15 to char 16"),
        invalid(
            "duplicate switch label",
            "switch (1) { case 0: case 0: }",
            "Duplicate case label: 0",
            "char 27 to char 28"),
        invalid(
            "switch, multiple default labels",
            "switch (1) { default: default: }",
            "Only one default label allowed in a switch statement",
            "char 23 to char 30"),
        invalid(
            "switch block, multiple default labels",
            "switch { default: default: }",
            "Only one default label allowed in a switch statement",
            "char 19 to char 26"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
