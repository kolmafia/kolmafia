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

public class PluralValueTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        invalid("Plural constant, abrupt end", "$booleans[", "No closing ] found"),
        invalid("Plural constant, unknown plural type", "$kolmafia[]", "Unknown type kolmafia"),
        invalid("Plural constant, RAM-protection", "$strings[]", "Can't enumerate all strings"),
        valid(
            "Plural constant, non... \"traditional\" plurals",
            "$bounties[]; $classes[]; $phyla[]",
            Arrays.asList(
                "$",
                "bounties",
                "[",
                "]",
                ";",
                "$",
                "classes",
                "[",
                "]",
                ";",
                "$",
                "phyla",
                "[",
                "]"),
            Arrays.asList(
                "1-1", "1-2", "1-10", "1-11", "1-12", "1-14", "1-15", "1-22", "1-23", "1-24",
                "1-26", "1-27", "1-32", "1-33")),
        valid(
            "Plural constant non-successive characters",
            "$		booleans 	 [ 		 ]",
            Arrays.asList("$", "booleans", "[", "]"),
            Arrays.asList("1-1", "1-4", "1-15", "1-20")),
        valid(
            "Plural constant multiline",
            // End-of-lines can appear anywhere
            "$booleans[true\n\n\n,\nfalse, false\n,false,\ntrue,tr\nue]",
            Arrays.asList(
                "$", "booleans", "[", "true", ",", "false, false", ",false,", "true,tr", "ue", "]"),
            Arrays.asList("1-1", "1-2", "1-10", "1-11", "4-1", "5-1", "6-1", "7-1", "8-1", "8-3")),
        invalid(
            "Plural constant w/ escape characters",
            // *Escaped* end-of-lines and escaped "\n"s get added to the value
            "$booleans[tr\\\nu\\ne]",
            "Bad boolean value: \"tr\nu\ne\""),
        valid(
            "Plural constant, nested brackets, proper",
            "$items[[8042]rock]",
            Arrays.asList("$", "items", "[", "[8042]rock", "]"),
            Arrays.asList("1-1", "1-2", "1-7", "1-8", "1-18"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Value.Constant pluralValue = assertInstanceOf(Value.Constant.class, commands.get(0));
              assertInstanceOf(PluralValue.class, pluralValue.value);
              // From the "$" up to the closing "]"
              ParserTest.assertLocationEquals(1, 1, 1, 19, pluralValue.getLocation());
            }),
        invalid(
            "Plural constant, nested brackets, improper",
            "$items[[abc]]",
            "Bad item value: \"[abc]\""),
        invalid(
            "Plural constant, single slash",
            "$booleans[tr/Comment\nue]",
            "Bad boolean value: \"tr/Commentue\""),
        valid(
            "Plural constant, comment",
            "$booleans[tr//Comment\nue]",
            Arrays.asList("$", "booleans", "[", "tr", "//Comment", "ue", "]"),
            Arrays.asList("1-1", "1-2", "1-10", "1-11", "1-13", "2-1", "2-3")),
        valid(
            "Plural constant, comment at start of line",
            "$booleans[tr\n//Comment\nue]",
            Arrays.asList("$", "booleans", "[", "tr", "//Comment", "ue", "]"),
            Arrays.asList("1-1", "1-2", "1-10", "1-11", "2-1", "3-1", "3-3")),
        valid(
            "Plural constant, empty comment",
            "$booleans[tr//\nue]",
            Arrays.asList("$", "booleans", "[", "tr", "//", "ue", "]"),
            Arrays.asList("1-1", "1-2", "1-10", "1-11", "1-13", "2-1", "2-3")),
        invalid(
            "Plural constant, two line-separated slashes",
            "$booleans[tr/\n/ue]",
            "Bad boolean value: \"tr//ue\""),
        invalid(
            "Plural constant, line-separated multiline comment",
            "$booleans[tr/\n**/ue]",
            "Bad boolean value: \"tr/**/ue\""),
        invalid("plural typed constant with escaped eof", "$items[true\\", "No closing ] found"),
        valid(
            "plural typed constant with escaped space",
            "$effects[\n\tBuy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!\n]",
            Arrays.asList("$", "effects", "[", "Buy!\\ \\ Sell!\\ \\ Buy!\\ \\ Sell!", "]"),
            Arrays.asList("1-1", "1-2", "1-9", "2-2", "3-1")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
