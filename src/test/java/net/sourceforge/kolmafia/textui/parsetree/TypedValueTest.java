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

public class TypedValueTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("Typed constant, no bracket", "$int", "Expected [, found end of file", "char 5"),
        valid(
            "Typed constant non-successive characters",
            "$		boolean 	 [ 		false ]",
            Arrays.asList("$", "boolean", "[", "false ", "]"),
            Arrays.asList("1-1", "1-4", "1-14", "1-18", "1-24")),
        valid(
            "Typed constant escaped characters",
            "$boolean[\\f\\a\\l\\s\\e]",
            Arrays.asList("$", "boolean", "[", "\\f\\a\\l\\s\\e", "]"),
            Arrays.asList("1-1", "1-2", "1-9", "1-10", "1-20")),
        invalid(
            "Typed constant bad typecast",
            "$boolean['']",
            "Bad boolean value: \"''\"",
            "char 10 to char 12",
            "$bool['']",
            "Unknown type bool",
            "char 2 to char 6"),
        invalid("Typed constant, unknown type", "$foo[]", "Unknown type foo", "char 2 to char 5"),
        invalid(
            "Typed constant, non-primitive type",
            "record r {int i;}; $r[]",
            "Non-primitive type r",
            "char 21 to char 22",
            "record r; $r[]",
            "Expected {, found ;",
            "char 9 to char 10"),
        invalid("Typed constant multiline", "$boolean[\n]", "No closing ] found", "char 10"),
        invalid(
            "Typed constant, escaped multiline",
            "$boolean[\\\n]",
            "No closing ] found",
            "char 10 to char 11"),
        valid(
            "Typed constant, nested brackets, proper",
            "$item[[8042]rock]",
            Arrays.asList("$", "item", "[", "[8042]rock", "]"),
            Arrays.asList("1-1", "1-2", "1-6", "1-7", "1-17"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Value.Constant typedValue = assertInstanceOf(Value.Constant.class, commands.get(0));
              // From the "$" up to the closing "]"
              ParserTest.assertLocationEquals(1, 1, 1, 18, typedValue.getLocation());
            }),
        invalid(
            "Typed constant, nested brackets, improper",
            "$item[[abc]]",
            "Bad item value: \"[abc]\"",
            "char 7 to char 12"),
        valid(
            "Typed constant, numeric literal",
            "$item[1]",
            Arrays.asList("$", "item", "[", "1", "]"),
            Arrays.asList("1-1", "1-2", "1-6", "1-7", "1-8")),
        valid(
            "Typed constant literal correction",
            "$item[rock]; $effect[buy!]; $monster[eldritch]; $skill[boon]; $location[dire warren]",
            Arrays.asList(
                "$",
                "item",
                "[",
                "rock",
                "]",
                ";",
                "$",
                "effect",
                "[",
                "buy!",
                "]",
                ";",
                "$",
                "monster",
                "[",
                "eldritch",
                "]",
                ";",
                "$",
                "skill",
                "[",
                "boon",
                "]",
                ";",
                "$",
                "location",
                "[",
                "dire warren",
                "]"),
            Arrays.asList(
                "1-1", "1-2", "1-6", "1-7", "1-11", "1-12", "1-14", "1-15", "1-21", "1-22", "1-26",
                "1-27", "1-29", "1-30", "1-37", "1-38", "1-46", "1-47", "1-49", "1-50", "1-55",
                "1-56", "1-60", "1-61", "1-63", "1-64", "1-72", "1-73", "1-84")),
        valid(
            "empty typed constant",
            "$string[]",
            Arrays.asList("$", "string", "[", "]"),
            Arrays.asList("1-1", "1-2", "1-8", "1-9")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
