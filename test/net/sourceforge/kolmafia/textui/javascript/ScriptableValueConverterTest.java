package net.sourceforge.kolmafia.textui.javascript;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class ScriptableValueConverterTest {
  private static Context cx;
  private static final Scriptable scope =
      new Scriptable() {
        @Override
        public String getClassName() {
          return null;
        }

        @Override
        public Object get(String name, Scriptable start) {
          return null;
        }

        @Override
        public Object get(int index, Scriptable start) {
          return null;
        }

        @Override
        public boolean has(String name, Scriptable start) {
          return false;
        }

        @Override
        public boolean has(int index, Scriptable start) {
          return false;
        }

        @Override
        public void put(String name, Scriptable start, Object value) {}

        @Override
        public void put(int index, Scriptable start, Object value) {}

        @Override
        public void delete(String name) {}

        @Override
        public void delete(int index) {}

        @Override
        public Scriptable getPrototype() {
          return null;
        }

        @Override
        public void setPrototype(Scriptable prototype) {}

        @Override
        public Scriptable getParentScope() {
          return null;
        }

        @Override
        public void setParentScope(Scriptable parent) {}

        @Override
        public Object[] getIds() {
          return new Object[0];
        }

        @Override
        public Object getDefaultValue(Class<?> hint) {
          return null;
        }

        @Override
        public boolean hasInstance(Scriptable instance) {
          return false;
        }
      };

  @BeforeAll
  static void beforeAll() {
    cx = Context.enter();
  }

  @AfterAll
  static void afterAll() {
    Context.exit();
  }

  @Test
  void asJavaFromJavaConvertWithoutDataLoss() {
    Value longMaxInt = new Value(Integer.MAX_VALUE - 1);
    Calendar timestamp = new GregorianCalendar();
    Value nowTime = new Value(timestamp.getTimeInMillis());
    Value longMaxLong = new Value(Long.MAX_VALUE);
    Value boolTrue = new Value(true);
    Value floatValue = new Value(Float.MAX_VALUE);

    ScriptableValueConverter vc = new ScriptableValueConverter(cx, scope);
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(longMaxInt)),
        longMaxInt,
        longMaxInt + " did not convert to/from Java");
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(longMaxLong)),
        longMaxLong,
        longMaxLong + " did not convert to/from Java");
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(nowTime)), nowTime, nowTime + " did not convert to/from Java");
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(boolTrue)), boolTrue, boolTrue + " did not convert to/from Java");
    Assertions.assertEquals(
        vc.fromJava(vc.asJava(floatValue)),
        floatValue,
        floatValue + " did not convert to/from Java");
  }

  @Nested
  class AsJava {
    @Test
    public void testNullValue() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      assertThat(converter.asJava(null), nullValue());
    }

    @Test
    public void testVoidType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value value = new Value();
      assertThat(converter.asJava(value), nullValue());
    }

    @Test
    public void testBooleanTypeTrue() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value value = DataTypes.makeBooleanValue(true);
      assertThat(converter.asJava(value), is(true));
    }

    @Test
    public void testBooleanTypeFalse() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value value = DataTypes.makeBooleanValue(false);
      assertThat(converter.asJava(value), is(false));
    }

    @Test
    public void testIntType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value value = DataTypes.makeIntValue(123);
      assertThat(converter.asJava(value), is(123L));
    }

    @Test
    public void testFloatType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value value = DataTypes.makeFloatValue(3.14);
      assertThat(converter.asJava(value), is(3.14));
    }

    @Test
    public void testStringType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value value = DataTypes.makeStringValue("Hello World");
      assertThat(converter.asJava(value), is("Hello World"));
    }

    @Test
    public void testStrictStringType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value value = new Value(DataTypes.STRICT_STRING_TYPE, "Strict Hello");
      assertThat(converter.asJava(value), is("Strict Hello"));
    }

    @Test
    public void testBufferType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value value = new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer("Buffer Content"));
      assertThat(converter.asJava(value), is("Buffer Content"));
    }

    @Test
    public void testMapType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Map<Value, Value> map =
          Map.of(
              DataTypes.makeStringValue("a"),
              DataTypes.makeIntValue(1),
              DataTypes.makeStringValue("b"),
              DataTypes.makeIntValue(2));
      Value value = new MapValue(DataTypes.STRING_TO_INT_TYPE, map);
      Scriptable converted = (Scriptable) converter.asJava(value);
      assertThat(ScriptableObject.getProperty(converted, "a"), is(1L));
      assertThat(ScriptableObject.getProperty(converted, "b"), is(2L));
    }

    @Test
    public void testArrayType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      List<String> list = List.of("a", "b", "d");
      Value value = DataTypes.makeStringArrayValue(list);
      Scriptable converted = (Scriptable) converter.asJava(value);
      assertThat(ScriptableObject.getProperty(converted, "length"), is(3.0));
      assertThat(ScriptableObject.getProperty(converted, 0), is("a"));
      assertThat(ScriptableObject.getProperty(converted, 1), is("b"));
      assertThat(ScriptableObject.getProperty(converted, 2), is("d"));
    }

    @Test
    public void testRecordType() {
      ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
      Value[] array = new Value[] {DataTypes.makeIntValue(1), DataTypes.makeStringValue("c")};
      Value value =
          new RecordValue(
              new RecordType(
                  "test",
                  new String[] {"a", "b"},
                  new Type[] {DataTypes.INT_TYPE, DataTypes.STRING_TYPE}));
      value.content = array;
      Scriptable converted = (Scriptable) converter.asJava(value);
      assertThat(ScriptableObject.getProperty(converted, "a"), is(1L));
      assertThat(ScriptableObject.getProperty(converted, "b"), is("c"));
    }
  }

  @Test
  public void findMatchingFunctionConvertArgsStripsUndefinedArguments() {
    ScriptableValueConverter converter = new ScriptableValueConverter(cx, scope);
    var args = List.of(42.0d, Undefined.SCRIPTABLE_UNDEFINED, Undefined.SCRIPTABLE_UNDEFINED);
    var functionWithArgs =
        converter.findMatchingFunctionConvertArgs(
            RuntimeLibrary.getFunctions(), "truncate", args.toArray());
    assertThat(functionWithArgs, notNullValue());
    assertThat(functionWithArgs.function().name, is("truncate"));
    assertThat(functionWithArgs.ashArgs(), is(List.of(DataTypes.makeFloatValue(42.0d))));
  }
}
