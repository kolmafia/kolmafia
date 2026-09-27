package net.sourceforge.kolmafia.textui.parsetree;

import static net.sourceforge.kolmafia.textui.ScriptData.valid;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ParserTest;
import net.sourceforge.kolmafia.textui.ScriptData;
import net.sourceforge.kolmafia.textui.parsetree.Function.MatchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FunctionTest {
  public static Stream<ScriptData> data() {
    return Stream.of(
        valid(
            "function parameter coercion to ANY_TYPE",
            "dump('foo', 'bar');",
            Arrays.asList("dump", "(", "'foo'", ",", "'bar'", ")", ";"),
            Arrays.asList("1-1", "1-5", "1-6", "1-11", "1-13", "1-18", "1-19")),
        valid(
            "function parameter no typedef coercion",
            "typedef int foo; foo a = 1; void bar(int x, foo y) {} bar(a, 1);",
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "void", "bar", "(", "int",
                "x", ",", "foo", "y", ")", "{", "}", "bar", "(", "a", ",", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-13", "1-16", "1-18", "1-22", "1-24", "1-26", "1-27", "1-29",
                "1-34", "1-37", "1-38", "1-42", "1-43", "1-45", "1-49", "1-50", "1-52", "1-53",
                "1-55", "1-58", "1-59", "1-60", "1-62", "1-63", "1-64")),
        valid(
            "function parameter typedef-to-base typedef coercion",
            "typedef int[] list; typedef list foo; foo a = int[] {1, 2}; list to_list(foo x) {return int[] {1};} void bar(list x) {} bar(a);",
            Arrays.asList(
                "typedef", "int", "[", "]", "list", ";", "typedef", "list", "foo", ";", "foo", "a",
                "=", "int", "[", "]", "{", "1", ",", "2", "}", ";", "list", "to_list", "(", "foo",
                "x", ")", "{", "return", "int", "[", "]", "{", "1", "}", ";", "}", "void", "bar",
                "(", "list", "x", ")", "{", "}", "bar", "(", "a", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-12", "1-13", "1-15", "1-19", "1-21", "1-29", "1-34", "1-37",
                "1-39", "1-43", "1-45", "1-47", "1-50", "1-51", "1-53", "1-54", "1-55", "1-57",
                "1-58", "1-59", "1-61", "1-66", "1-73", "1-74", "1-78", "1-79", "1-81", "1-82",
                "1-89", "1-92", "1-93", "1-95", "1-96", "1-97", "1-98", "1-99", "1-101", "1-106",
                "1-109", "1-110", "1-115", "1-116", "1-118", "1-119", "1-121", "1-124", "1-125",
                "1-126", "1-127"),
            scope -> {
              List<Command> commands = scope.getCommandList();

              // Coercion function call location test
              FunctionCall barCall = assertInstanceOf(FunctionCall.class, commands.get(1));
              // Just making sure we have the right one
              ParserTest.assertLocationEquals(1, 121, 1, 127, barCall.getLocation());
              List<Evaluable> barParams = barCall.getParams();
              // Instead of simply being a VariableReference to "a", the parameter is a function
              // call to to_list()
              FunctionCall coercionCall = assertInstanceOf(FunctionCall.class, barParams.get(0));
              ParserTest.assertLocationEquals(1, 125, 1, 126, coercionCall.getLocation());
              ParserTest.assertLocationEquals(1, 66, 1, 80, coercionCall.getTarget().getLocation());
            }),
        valid(
            "function parameter base-to-typedef typedef coercion",
            "typedef int foo; foo a = 1; foo to_foo(int x) {return a;} void bar(foo x) {} bar(1);",
            Arrays.asList(
                "typedef", "int", "foo", ";", "foo", "a", "=", "1", ";", "foo", "to_foo", "(",
                "int", "x", ")", "{", "return", "a", ";", "}", "void", "bar", "(", "foo", "x", ")",
                "{", "}", "bar", "(", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-9", "1-13", "1-16", "1-18", "1-22", "1-24", "1-26", "1-27", "1-29",
                "1-33", "1-39", "1-40", "1-44", "1-45", "1-47", "1-48", "1-55", "1-56", "1-57",
                "1-59", "1-64", "1-67", "1-68", "1-72", "1-73", "1-75", "1-76", "1-78", "1-81",
                "1-82", "1-83", "1-84")),
        valid(
            "record function match",
            "record rec {int i;}; void foo(rec x) {} foo(new rec());",
            Arrays.asList(
                "record", "rec", "{", "int", "i", ";", "}", ";", "void", "foo", "(", "rec", "x",
                ")", "{", "}", "foo", "(", "new", "rec", "(", ")", ")", ";"),
            Arrays.asList(
                "1-1", "1-8", "1-12", "1-13", "1-17", "1-18", "1-19", "1-20", "1-22", "1-27",
                "1-30", "1-31", "1-35", "1-36", "1-38", "1-39", "1-41", "1-44", "1-45", "1-49",
                "1-52", "1-53", "1-54", "1-55")),
        valid(
            "coerced function match",
            "void foo(float x) {} foo(1);",
            Arrays.asList(
                "void", "foo", "(", "float", "x", ")", "{", "}", "foo", "(", "1", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-16", "1-17", "1-19", "1-20", "1-22", "1-25", "1-26",
                "1-27", "1-28")),
        valid(
            "vararg function match",
            "void foo(int... x) {} foo(1, 2, 3);",
            Arrays.asList(
                "void", "foo", "(", "int", "...", "x", ")", "{", "}", "foo", "(", "1", ",", "2",
                ",", "3", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-13", "1-17", "1-18", "1-20", "1-21", "1-23", "1-26",
                "1-27", "1-28", "1-30", "1-31", "1-33", "1-34", "1-35")),
        valid(
            "coerced vararg function match",
            "void foo(float... x) {} foo(1, 2, 3);",
            Arrays.asList(
                "void", "foo", "(", "float", "...", "x", ")", "{", "}", "foo", "(", "1", ",", "2",
                ",", "3", ")", ";"),
            Arrays.asList(
                "1-1", "1-6", "1-9", "1-10", "1-15", "1-19", "1-20", "1-22", "1-23", "1-25", "1-28",
                "1-29", "1-30", "1-32", "1-33", "1-35", "1-36", "1-37")));
  }

  @ParameterizedTest
  @MethodSource("data")
  public void testScriptValidity(ScriptData script) {
    ParserTest.testScriptValidity(script);
  }

  @Nested
  class ParamsMatch {
    // public boolean paramsMatch(final Function that, final boolean base)
    //
    // Check if this function's parameters match that function's parameters
    //
    // This is used (only) by UserDefineFunction.overridesLibraryFunction
    // *** This is not tested here (yet)

    // public boolean paramsMatch(final List<? extends TypedNode> params, MatchType match, boolean
    // vararg)
    //
    // Check if a supplied list of values match this function's parameters.
    //
    // MatchType.EXACT  - the value and the function parameter must be identical
    // MatchType.BASE   - either the value or the function parameter can be typedefs
    // MatchType.COERCE - the value must be coercable("parameter")
    //
    // This is used by BasicScope.findFunction -> which needs its own tests.

    private static List<VariableReference> params;
    private static List<Value> values;
    private static Scope scope;

    @BeforeEach
    public void beforeEach() {
      params = new ArrayList<>();
      values = new ArrayList<>();
      scope = new Scope(null);
    }

    private void addParameter(Type vartype) {
      int n = params.size();
      Variable v = new Variable("V" + String.valueOf(++n), vartype, null);
      scope.addVariable(v);
      VariableReference vr = new VariableReference(null, v);
      params.add(vr);
    }

    private void addValue(Type vtype) {
      Value v = new Value(vtype);
      values.add(v);
    }

    private Function makeFunction(String name, Type valType, Type... paramTypes) {
      for (Type type : paramTypes) {
        addParameter(type);
      }
      return new UserDefinedFunction(name, valType, params, null);
    }

    private Type vararg(Type type) {
      return new VarArgType(type);
    }

    private Type array(Type data) {
      return new AggregateType(data, 0);
    }

    private Type aggregate(Type index, Type data) {
      return new AggregateType(data, index);
    }

    private void makeValues(Type... valueTypes) {
      for (Type type : valueTypes) {
        addValue(type);
      }
    }

    @Nested
    class BadVarargs {
      // Tests for functions with bad varargs.
      //
      // These should be disallowed by the Parser, but paramsMatch
      // must not get confused by them.
      //
      // These do not depend on match type

      // The function has two varargs at the end
      @Test
      void bad_varg_varg_int() {
        Function f =
            makeFunction(
                "f",
                DataTypes.VOID_TYPE,
                DataTypes.STRING_TYPE,
                vararg(DataTypes.INT_TYPE),
                vararg(DataTypes.INT_TYPE));
        makeValues(DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE);
        assertFalse(f.paramsMatch(values, MatchType.EXACT));
      }

      // The function has a varargs followed by another parameter
      @Test
      void bad_varg_string_int() {
        Function f =
            makeFunction(
                "f",
                DataTypes.VOID_TYPE,
                DataTypes.STRING_TYPE,
                vararg(DataTypes.INT_TYPE),
                DataTypes.STRING_TYPE);
        makeValues(DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.STRING_TYPE);
        assertFalse(f.paramsMatch(values, MatchType.EXACT));
      }
    }

    @Nested
    class ExactMatch {
      @Nested
      class NoVarArgs {
        // The function does not have a vararg parameter

        // One int is required, one int is provided
        @Test
        void int_int() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.EXACT));
        }

        // Two ints are required, one int is provided
        @Test
        void int2_int() {
          Function f =
              makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // One int is required, two are provided
        @Test
        void int_int2() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // int is required, typedef int is provided
        @Test
        void int_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(td);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // float is required, int is provided
        @Test
        void float_int() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.FLOAT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // typedef int is required, int is provided
        @Test
        void typedef_int() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td);
          makeValues(DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // typedef int is required, same typedef int is provided
        @Test
        void typedef_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td);
          makeValues(td);
          assertTrue(f.paramsMatch(values, MatchType.EXACT));
        }

        // typedef int is required, different typedef int is provided
        @Test
        void typedef1_typedef2() {
          TypeDef td1 = new TypeDef("td1", DataTypes.INT_TYPE, null);
          TypeDef td2 = new TypeDef("td2", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td1);
          makeValues(td2);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }
      }

      @Nested
      class VarArgs {
        // The function does have a vararg parameter
        // Tests for functions with valid varargs.

        // vararg allowed, no matching args provided
        @Test
        void int_int0() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE);
          // We are not testing whether function argument types match,
          // but whether this function will accept these values.
          // A vararg is happy to have size zero.
          assertTrue(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, one matching arg provided
        @Test
        void int_int1() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, typedef int is provided
        @Test
        void int_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(td);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, two matching args provided
        @Test
        void int_int2() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        @Test
        void int_string1() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.STRING_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        @Test
        void missing_value_for_param_before_vararg() {
          Function f =
              makeFunction(
                  "f",
                  DataTypes.VOID_TYPE,
                  DataTypes.STRING_TYPE,
                  DataTypes.STRING_TYPE,
                  vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, no matching args provided, args before vararg mismatch
        @Test
        void mismatched_value_for_param_before_vararg() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE, vararg(DataTypes.STRING_TYPE));
          makeValues(DataTypes.STRING_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }
      }

      @Disabled("behaviour is currently wrong")
      @Nested
      class DirectVarArgs {
        // The function does have a vararg parameter
        // Tests for functions with direct vararg argument.

        // vararg allowed, an array of matching ints is provided
        @Test
        void int_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, array(DataTypes.INT_TYPE));
          assertTrue(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, an array of matching typedef ints is provided
        @Test
        void typedef_int_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          makeValues(DataTypes.STRING_TYPE, array(td));
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, a map of matching ints is provided
        @Test
        void int_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, aggregate(DataTypes.INT_TYPE, DataTypes.INT_TYPE));
          assertTrue(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, a map of matching ints is provided
        @Test
        void typedef_int_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          makeValues(DataTypes.STRING_TYPE, aggregate(DataTypes.INT_TYPE, td));
          assertFalse(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, a typedef array of matching ints is provided
        @Test
        void int_typedef_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef tda = new TypeDef("tda", array(DataTypes.INT_TYPE), null);
          makeValues(DataTypes.STRING_TYPE, tda);
          // Since we are matching parameters, any aggregate that holds the
          // appropriate data type is fine.
          assertTrue(f.paramsMatch(values, MatchType.EXACT));
        }

        // vararg allowed, a typedef map of matching ints is provided
        @Test
        void int_typedef_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef tdm = new TypeDef("tdm", aggregate(DataTypes.INT_TYPE, DataTypes.INT_TYPE), null);
          makeValues(DataTypes.STRING_TYPE, tdm);
          // Since we are matching parameters, any aggregate that holds the
          // appropriate data type is fine.
          assertTrue(f.paramsMatch(values, MatchType.EXACT));
        }
      }
    }

    @Nested
    class BaseMatch {
      @Nested
      class NoVarArgs {
        // The function does not have a vararg parameter

        // One int is required, one int is provided
        @Test
        void int_int() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        // Two ints are required, one int is provided
        @Test
        void int2_int() {
          Function f =
              makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // One int is required, two are provided
        @Test
        void int_int2() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // int is required, typedef int is provided
        @Test
        void int_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(td);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        // float is required, int is provided
        @Test
        void float_int() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.FLOAT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // typedef int is required, int is provided
        @Test
        void typedef_int() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td);
          makeValues(DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        // typedef int is required, same typedef int is provided
        @Test
        void typedef_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td);
          makeValues(td);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        // typedef int is required, different typedef int is provided
        @Test
        void typedef1_typedef2() {
          TypeDef td1 = new TypeDef("td1", DataTypes.INT_TYPE, null);
          TypeDef td2 = new TypeDef("td2", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td1);
          makeValues(td2);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }
      }

      @Nested
      class VarArgs {
        // The function does have a vararg parameter
        // Tests for functions with valid varargs.

        // vararg allowed, no matching args provided
        @Test
        void int_int0() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE);
          // We are not testing whether function argument types match,
          // but whether this function will accept these values.
          // A vararg is happy to have size zero.
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, one matching arg provided
        @Test
        void int_int1() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, two matching args provided
        @Test
        void int_int2() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, typedef int is provided
        @Test
        void int_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(td);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        @Test
        void int_string1() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.STRING_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, no matching args provided, args before vararg mismatch
        @Test
        void mismatched_value_for_param_before_vararg() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE, vararg(DataTypes.STRING_TYPE));
          makeValues(DataTypes.STRING_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }
      }

      @Disabled("behaviour is currently wrong")
      @Nested
      class DirectVarArgs {
        // vararg allowed, an array of matching ints is provided
        @Test
        void int_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, array(DataTypes.INT_TYPE));
          // *** This is EXACT. Why does BASE match not accept this?
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, an array of matching typedef ints is provided
        @Test
        void typedef_int_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          makeValues(DataTypes.STRING_TYPE, array(td));
          // *** Why does BASE match not accept this?
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, a map of matching ints is provided
        @Test
        void int_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, aggregate(DataTypes.INT_TYPE, DataTypes.INT_TYPE));
          // *** This is EXACT. Why does BASE match not accept this?
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, a map of matching ints is provided
        @Test
        void typedef_int_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          makeValues(DataTypes.STRING_TYPE, aggregate(DataTypes.INT_TYPE, td));
          // *** Why does BASE match not accept this?
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, a typedef array of matching ints is provided
        @Test
        void int_typedef_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef tda = new TypeDef("tda", array(DataTypes.INT_TYPE), null);
          makeValues(DataTypes.STRING_TYPE, tda);
          // *** Why does BASE match not accept this?
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }

        // vararg allowed, a typedef map of matching ints is provided
        @Test
        void int_typedef_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef tdm = new TypeDef("tdm", aggregate(DataTypes.INT_TYPE, DataTypes.INT_TYPE), null);
          makeValues(DataTypes.STRING_TYPE, tdm);
          // *** Why does BASE match not accept this?
          assertFalse(f.paramsMatch(values, MatchType.BASE));
        }
      }
    }

    @Nested
    class CoerceMatch {
      @Nested
      class NoVarArgs {
        // The function does not have a vararg parameter

        // One int is required, one int is provided
        @Test
        void int_int() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }

        // Two ints are required, one int is provided
        @Test
        void int2_int() {
          Function f =
              makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }

        // One int is required, two are provided
        @Test
        void int_int2() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }

        // int is required, typedef int is provided
        @Test
        void int_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(td);
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }

        // float is required, int is provided
        @Test
        void float_int() {
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.FLOAT_TYPE);
          makeValues(DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }

        // typedef int is required, int is provided
        @Test
        void typedef_int() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td);
          makeValues(DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }

        // typedef int is required, same typedef int is provided
        @Test
        void typedef_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td);
          makeValues(td);
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }

        // typedef int is required, different typedef int is provided
        @Test
        void typedef1_typedef2() {
          TypeDef td1 = new TypeDef("td1", DataTypes.INT_TYPE, null);
          TypeDef td2 = new TypeDef("td2", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, td1);
          makeValues(td2);
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }
      }

      @Nested
      class VarArgs {
        // The function does have a vararg parameter
        // Tests for functions with valid varargs.

        // vararg allowed, no matching args provided
        @Test
        void int_int0() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE);
          // We are not testing whether function argument types match,
          // but whether this function will accept these values.
          // A vararg is happy to have size zero.
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, one matching arg provided
        @Test
        void int_int1() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, two matching args provided
        @Test
        void int_int2() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.INT_TYPE, DataTypes.INT_TYPE);
          assertTrue(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, typedef int is provided
        @Test
        void int_typedef() {
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          Function f = makeFunction("f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE);
          makeValues(td);
          assertTrue(f.paramsMatch(values, MatchType.BASE));
        }

        @Test
        void int_string1() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, DataTypes.STRING_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, no matching args provided, args before vararg mismatch
        @Test
        void mismatched_value_for_param_before_vararg() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.INT_TYPE, vararg(DataTypes.STRING_TYPE));
          makeValues(DataTypes.STRING_TYPE);
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }
      }

      @Disabled("behaviour is currently wrong")
      @Nested
      class DirectVarArgs {

        // vararg allowed, an array of matching ints is provided
        @Test
        void int_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, array(DataTypes.INT_TYPE));
          // *** aggregates are not coercable.
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, an array of matching typedef ints is provided
        @Test
        void typedef_int_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          makeValues(DataTypes.STRING_TYPE, array(td));
          // *** aggregates are not coercable.
          // *** Why does COERCE match not accept this?
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, a map of matching ints is provided
        @Test
        void int_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          makeValues(DataTypes.STRING_TYPE, aggregate(DataTypes.INT_TYPE, DataTypes.INT_TYPE));
          // *** aggregates are not coercable.
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, a map of matching ints is provided
        @Test
        void typedef_int_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef td = new TypeDef("td", DataTypes.INT_TYPE, null);
          makeValues(DataTypes.STRING_TYPE, aggregate(DataTypes.INT_TYPE, td));
          // *** aggregates are not coercable.
          // *** Why does COERCE match not accept this?
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, a typedef array of matching ints is provided
        @Test
        void int_typedef_array() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef tda = new TypeDef("tda", array(DataTypes.INT_TYPE), null);
          makeValues(DataTypes.STRING_TYPE, tda);
          // *** aggregates are not coercable.
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }

        // vararg allowed, a typedef map of matching ints is provided
        @Test
        void int_typedef_map() {
          Function f =
              makeFunction(
                  "f", DataTypes.VOID_TYPE, DataTypes.STRING_TYPE, vararg(DataTypes.INT_TYPE));
          TypeDef tdm = new TypeDef("tdm", aggregate(DataTypes.INT_TYPE, DataTypes.INT_TYPE), null);
          makeValues(DataTypes.STRING_TYPE, tdm);
          // *** aggregates are not coercable.
          assertFalse(f.paramsMatch(values, MatchType.COERCE));
        }
      }
    }
  }
}
