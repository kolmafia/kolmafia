package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ForEachLoopTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "empty foreach",
            "foreach cls in $classes[];",
            Arrays.asList("foreach", "cls", "in", "$", "classes", "[", "]", ";"),
            Arrays.asList("1-1", "1-9", "1-13", "1-16", "1-17", "1-24", "1-25", "1-26")),
        invalid(
            "foreach with non-identifier key",
            "foreach 'key' in $items[];",
            "Key variable name expected"),
        invalid(
            "foreach with reserved key",
            "foreach item in $items[];",
            "Reserved word 'item' cannot be a key variable"),
        invalid("foreach missing `in`", "foreach it;", "Expected in, found ;"),
        invalid(
            "foreach missing key variable name", "foreach in it;", "Key variable name expected"),
        invalid(
            "foreach key variable named 'in'",
            "foreach in in it;",
            "Reserved word 'in' cannot be a key variable name"),
        invalid(
            "foreach key variable named 'in' 2",
            "foreach in, on, under, below, through in it;",
            "Reserved word 'in' cannot be a key variable name"),
        invalid(
            "foreach in not-a-reference",
            "foreach it in $item[none];",
            "Aggregate reference expected"),
        invalid(
            "foreach with duplicate key",
            "foreach it, it in $items[];",
            "Key variable 'it' is already defined"),
        valid(
            "foreach with multiple keys",
            "foreach key, value in int[int]{} {}",
            Arrays.asList(
                "foreach", "key", ",", "value", "in", "int", "[", "int", "]", "{", "}", "{", "}"),
            Arrays.asList(
                "1-1", "1-9", "1-12", "1-14", "1-20", "1-23", "1-26", "1-27", "1-30", "1-31",
                "1-32", "1-34", "1-35"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              ForEachLoop forLoop = assertInstanceOf(ForEachLoop.class, commands.get(0));
              Scope loopScope = forLoop.getScope();
              Iterator<Variable> variables = loopScope.getVariables().iterator();

              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 9, 1, 12, variables.next().getLocation());
              assertTrue(variables.hasNext());
              ParserTest.assertLocationEquals(1, 14, 1, 19, variables.next().getLocation());
              assertFalse(variables.hasNext());
            }),
        invalid(
            "foreach with too many keys",
            "foreach a, b, c in $items[];",
            "Too many key variables specified"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
