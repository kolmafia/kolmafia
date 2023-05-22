package net.sourceforge.kolmafia.textui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.VarArgType;
import net.sourceforge.kolmafia.textui.parsetree.Variable;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import org.eclipse.lsp4j.Location;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AshRuntimeTest {
  @Nested
  class RequestUserParams {
    AshRuntime runtime = new AshRuntime();

    // boolean requestUserParams(
    //     final Function targetFunction,
    //     final Object[] parameters,
    //     Object[] values)
    //
    // parameters is an array of Strings from user input
    // values is an array of parsed Values from parameters
    // values[0] is the AshRuntime interpreter

    VariableReference makeVariableReference(Type type, String name) {
      Location location = null;
      Variable variable = new Variable(name, type, location);
      return new VariableReference(location, variable);
    }

    @Nested
    class VarArgs {
      // void main(string command, int... args)
      Function main1 =
          new UserDefinedFunction(
              "main",
              DataTypes.VOID_TYPE,
              List.of(
                  makeVariableReference(DataTypes.STRING_TYPE, "command"),
                  makeVariableReference(new VarArgType(DataTypes.INT_TYPE), "args")),
              null);

      @Test
      void requiredParameterAndZeroOptionalParameters() {
        Object[] parameters = {"list"};
        Object[] values = new Object[3];
        values[0] = runtime;
        boolean result = runtime.requestUserParams(main1, parameters, values);
        assertTrue(result);
        assertTrue(values[1] instanceof Value);
        Value value1 = (Value) values[1];
        assertEquals(DataTypes.STRING_TYPE, value1.getType());
        assertEquals("list", value1.toString());
        assertTrue(values[2] instanceof Value);
        Value value2 = (Value) values[2];
        assertTrue(value2.getType() instanceof VarArgType);
        assertTrue(value2.content instanceof Value[]);
        Value[] content = (Value[]) value2.content;
        assertEquals(0, content.length);
      }

      @Test
      void requiredParameterAndOptionalParameters() {
        Object[] parameters = {"list", "10", "20", "30"};
        Object[] values = new Object[3];
        values[0] = runtime;
        boolean result = runtime.requestUserParams(main1, parameters, values);
        assertTrue(result);
        assertTrue(values[1] instanceof Value);
        Value value1 = (Value) values[1];
        assertEquals(DataTypes.STRING_TYPE, value1.getType());
        assertEquals("list", value1.toString());
        assertTrue(values[2] instanceof Value);
        Value value2 = (Value) values[2];
        assertTrue(value2.getType() instanceof VarArgType);
        assertTrue(value2.content instanceof Value[]);
        Value[] content = (Value[]) value2.content;
        assertEquals(3, content.length);
        assertEquals(10, content[0].contentLong);
        assertEquals(20, content[1].contentLong);
        assertEquals(30, content[2].contentLong);
      }

      // void main(string... params)
      Function main2 =
          new UserDefinedFunction(
              "main",
              DataTypes.VOID_TYPE,
              List.of(makeVariableReference(new VarArgType(DataTypes.STRING_TYPE), "params")),
              null);

      @Test
      void noRequiredParameterAndZeroOptionalParameters() {
        Object[] parameters = {};
        Object[] values = new Object[2];
        values[0] = runtime;
        boolean result = runtime.requestUserParams(main2, parameters, values);
        assertTrue(result);
        assertTrue(values[1] instanceof Value);
        Value value1 = (Value) values[1];
        assertTrue(value1.getType() instanceof VarArgType);
        assertTrue(value1.content instanceof Value[]);
        Value[] content = (Value[]) value1.content;
        assertEquals(0, content.length);
      }

      @Test
      void noRequiredParameterAndOneOptionalParameters() {
        Object[] parameters = {"foo bar baz"};
        Object[] values = new Object[2];
        values[0] = runtime;
        boolean result = runtime.requestUserParams(main2, parameters, values);
        assertTrue(result);
        assertTrue(values[1] instanceof Value);
        Value value1 = (Value) values[1];
        assertTrue(value1.getType() instanceof VarArgType);
        assertTrue(value1.content instanceof Value[]);
        Value[] content = (Value[]) value1.content;
        assertEquals(1, content.length);
        assertEquals("foo bar baz", content[0].contentString);
      }

      @Test
      void noRequiredParameterAndThreeOptionalParameters() {
        Object[] parameters = {"foo", "bar", "baz"};
        Object[] values = new Object[2];
        values[0] = runtime;
        boolean result = runtime.requestUserParams(main2, parameters, values);
        assertTrue(result);
        assertTrue(values[1] instanceof Value);
        Value value1 = (Value) values[1];
        assertTrue(value1.getType() instanceof VarArgType);
        assertTrue(value1.content instanceof Value[]);
        Value[] content = (Value[]) value1.content;
        assertEquals(3, content.length);
        assertEquals("foo", content[0].contentString);
        assertEquals("bar", content[1].contentString);
        assertEquals("baz", content[2].contentString);
      }

      // void main(int... args)
      Function main3 =
          new UserDefinedFunction(
              "main",
              DataTypes.VOID_TYPE,
              List.of(makeVariableReference(new VarArgType(DataTypes.ITEM_TYPE), "args")),
              null);

      @Test
      void noParameterAndBogusOptionalParameter() {
        Object[] parameters = {"bogus"};
        Object[] values = new Object[2];
        values[0] = runtime;
        ByteArrayOutputStream ostream = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(ostream, true)) {
          RequestLogger.openCustom(out);
          boolean result = runtime.requestUserParams(main3, parameters, values);
          String output = ostream.toString().trim();
          RequestLogger.closeCustom();
          assertFalse(result);
          assertEquals("Bad item value: \"bogus\"", output);
        }
      }
    }
  }
}
