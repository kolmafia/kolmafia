package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
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
            "Key variable name expected",
            "char 9 to char 10"),
        invalid(
            "foreach with reserved key",
            "foreach item in $items[];",
            "Reserved word 'item' cannot be a key variable name",
            "char 9 to char 13"),
        invalid(
            "foreach missing `in`", "foreach it;", "Expected in, found ;", "char 11 to char 12"),
        invalid(
            "foreach missing key variable name",
            "foreach in it;",
            "Key variable name expected",
            "char 9 to char 11"),
        invalid(
            "foreach key variable named 'in'",
            "foreach in in it;",
            "Reserved word 'in' cannot be a key variable name",
            "char 9 to char 11"),
        invalid(
            "foreach key variable named 'in' 2",
            "foreach in, on, under, below, through in it;",
            "Reserved word 'in' cannot be a key variable name",
            "char 9 to char 11"),
        invalid(
            "foreach in not-a-reference",
            "foreach it in $item[none];",
            "Aggregate reference expected",
            "char 15 to char 26",
            "foreach it in it;",
            "Unknown variable 'it'",
            "char 15 to char 17"),
        invalid(
            "foreach with duplicate key",
            "foreach it, it in $items[];",
            "Key variable 'it' is already defined",
            "char 13 to char 15"),
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

              // Loop location test
              ForEachLoop forLoop = assertInstanceOf(ForEachLoop.class, commands.get(0));
              // From the "foreach" up to the end of its scope
              ParserTest.assertLocationEquals(1, 1, 1, 36, forLoop.getLocation());

              // Scope location test
              Scope loopScope = forLoop.getScope();
              ParserTest.assertLocationEquals(1, 34, 1, 36, loopScope.getLocation());

              // Variable + VariableReference location test
              Iterator<Variable> variables = loopScope.getVariables().iterator();
              List<VariableReference> references = forLoop.getVariableReferences();
              // key
              assertTrue(variables.hasNext());
              Variable var = variables.next();
              VariableReference varRef = references.get(0);
              ParserTest.assertLocationEquals(1, 9, 1, 12, var.getLocation());
              ParserTest.assertLocationEquals(1, 9, 1, 12, varRef.getLocation());
              assertSame(var, varRef.target);
              // value
              assertTrue(variables.hasNext());
              var = variables.next();
              varRef = references.get(1);
              ParserTest.assertLocationEquals(1, 14, 1, 19, var.getLocation());
              ParserTest.assertLocationEquals(1, 14, 1, 19, varRef.getLocation());
              assertSame(var, varRef.target);
              assertFalse(variables.hasNext());
            }),
        invalid(
            "foreach with too many keys",
            "foreach a, b, c in $items[];",
            "Too many key variables specified",
            "char 15 to char 16"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
