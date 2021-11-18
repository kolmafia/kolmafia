package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.invalid;
import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class AssignmentTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "Simple operator assignment",
            "int x = 3;",
            Arrays.asList("int", "x", "=", "3", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-10"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Assignment assignment = assertInstanceOf(Assignment.class, commands.get(0));
              // Assignment.oper is the operator to use *before* doing the assignment (i.e. the "=")
              assertNull(assignment.getOperator());
            }),
        valid(
            "Compound operator assignment",
            "int x; x += 3;",
            Arrays.asList("int", "x", ";", "x", "+=", "3", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-8", "1-10", "1-13", "1-14"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              Assignment declaration = assertInstanceOf(Assignment.class, commands.get(0));
              Assignment assignment = assertInstanceOf(Assignment.class, commands.get(1));

              // Variable + VariableReference location test
              VariableReference varRef1 = declaration.getLeftHandSide();
              VariableReference varRef2 = assignment.getLeftHandSide();
              ParserTest.assertLocationEquals(1, 5, 1, 6, varRef1.getLocation());
              ParserTest.assertLocationEquals(1, 8, 1, 9, varRef2.getLocation());
              ParserTest.assertLocationEquals(1, 5, 1, 6, varRef1.target.getLocation());
              assertSame(varRef1.target, varRef2.target);

              // Operator location test
              Operator oper = assignment.getOperator();
              // Assignment.oper is the operator to use *before* doing the assignment (i.e. the "=")
              assertEquals("+", oper.toString());
              ParserTest.assertLocationEquals(1, 10, 1, 11, oper.getLocation());
            }),
        invalid(
            "Aggregate assignment to primitive",
            // What is this, C?
            "int x; x = {1};",
            "Cannot use an aggregate literal for type int"),
        invalid(
            "Primitive assignment to aggregate",
            "int[4] x = 1;",
            "Cannot store int in x of type int [4]"),
        invalid(
            "Compound assignment to aggregate",
            "int[4] x; x += 1;",
            "Cannot use '+=' on an aggregate"),
        valid(
            "Aggregate assignment to aggregate",
            "int[4] x; x = {1, 2, 3, 4};",
            Arrays.asList(
                "int", "[", "4", "]", "x", ";", "x", "=", "{", "1", ",", "2", ",", "3", ",", "4",
                "}", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-6", "1-8", "1-9", "1-11", "1-13", "1-15", "1-16", "1-17",
                "1-19", "1-20", "1-22", "1-23", "1-25", "1-26", "1-27")),
        invalid(
            "brace assignment of primitive",
            "int x = {1}",
            "Cannot initialize x of type int with an aggregate literal"),
        valid(
            "brace assignment of array",
            "int[] x = {1, 2, 3};",
            Arrays.asList("int", "[", "]", "x", "=", "{", "1", ",", "2", ",", "3", "}", ";"),
            Arrays.asList(
                "1-1", "1-4", "1-5", "1-7", "1-9", "1-11", "1-12", "1-13", "1-15", "1-16", "1-18",
                "1-19", "1-20")),
        invalid("invalid coercion", "int x = 'hello';", "Cannot store string in x of type int"),
        valid(
            "float->int assignment",
            "int x = 1.0;",
            Arrays.asList("int", "x", "=", "1", ".", "0", ";"),
            Arrays.asList("1-1", "1-5", "1-7", "1-9", "1-10", "1-11", "1-12")),
        valid(
            "int->float assignment",
            "float y = 1;",
            Arrays.asList("float", "y", "=", "1", ";"),
            Arrays.asList("1-1", "1-7", "1-9", "1-11", "1-12")),
        invalid("assignment missing rhs", "int x =", "Expression expected"),
        invalid("assignment missing rhs 2", "int x; x =", "Expression expected"),
        invalid(
            "Invalid assignment coercion - logical",
            "int x; x &= true",
            "&= requires an integer or boolean expression and an integer or boolean variable reference"),
        invalid(
            "Invalid assignment coercion - integer",
            "boolean x; x >>= 1",
            ">>= requires an integer expression and an integer variable reference"),
        invalid(
            "Invalid assignment coercion - assignment",
            "boolean x; x += 'foo'",
            "Cannot store string in x of type boolean"));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }
}
