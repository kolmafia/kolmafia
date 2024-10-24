package net.sourceforge.kolmafia.textui.javascript;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class JSONValueConverterTest {
  private final JSONValueConverter converter = new JSONValueConverter();

  @Nested
  class AsJava {
    @Test
    public void testNullValue() {
      assertThat(converter.asJava(null), nullValue());
    }

    @Test
    public void testVoidType() {
      Value value = new Value();
      assertThat(converter.asJava(value), nullValue());
    }

    @Test
    public void testBooleanTypeTrue() {
      Value value = DataTypes.makeBooleanValue(true);
      assertThat(converter.asJava(value), is(true));
    }

    @Test
    public void testBooleanTypeFalse() {
      Value value = DataTypes.makeBooleanValue(true);
      assertThat(converter.asJava(value), is(true));
    }

    @Test
    public void testIntType() {
      Value value = DataTypes.makeIntValue(123);
      assertThat(converter.asJava(value), is(123L));
    }

    @Test
    public void testFloatType() {
      Value value = DataTypes.makeFloatValue(3.14);
      assertThat(converter.asJava(value), is(3.14));
    }

    @Test
    public void testStringType() {
      Value value = DataTypes.makeStringValue("Hello World");
      assertThat(converter.asJava(value), is("Hello World"));
    }

    @Test
    public void testStrictStringType() {
      Value value = new Value(DataTypes.STRICT_STRING_TYPE, "Strict Hello");
      assertThat(converter.asJava(value), is("Strict Hello"));
    }

    @Test
    public void testBufferType() {
      Value value = new Value(DataTypes.BUFFER_TYPE, "", new StringBuffer("Buffer Content"));
      assertThat(converter.asJava(value), is("Buffer Content"));
    }

    @Test
    public void testMapType() {
      Map<Value, Value> map =
          Map.of(
              DataTypes.makeStringValue("a"),
              DataTypes.makeIntValue(1),
              DataTypes.makeStringValue("b"),
              DataTypes.makeIntValue(2));
      Value value = new MapValue(DataTypes.STRING_TO_INT_TYPE, map);
      JSONObject converted = (JSONObject) converter.asJava(value);
      assertThat(converted.size(), is(2));
      assertThat(converted, allOf(hasEntry("a", 1L), hasEntry("b", 2L)));
    }

    @Test
    public void testArrayType() {
      List<String> list = List.of("a", "b", "d");
      Value value = DataTypes.makeStringArrayValue(list);
      JSONArray converted = (JSONArray) converter.asJava(value);
      assertThat(converted, contains("a", "b", "d"));
    }

    @Test
    public void testRecordType() {
      Value[] array = new Value[] {DataTypes.makeIntValue(1), DataTypes.makeStringValue("c")};
      Value value =
          new RecordValue(
              new RecordType(
                  "test",
                  new String[] {"a", "b"},
                  new Type[] {DataTypes.INT_TYPE, DataTypes.STRING_TYPE}));
      value.content = array;
      JSONObject converted = (JSONObject) converter.asJava(value);
      assertThat(converted.size(), is(2));
      assertThat(converted, hasEntry("a", 1L));
      assertThat(converted, hasEntry("b", "c"));
    }
  }

  @Nested
  class FromJava {
    @Test
    public void testNullValue() {
      Value result = JSONValueConverter.fromJSON(null, null);
      assertThat(result, nullValue());
    }

    @Test
    public void testBooleanValue() {
      Value result = JSONValueConverter.fromJSON(JSON.parse("true"), null);
      assertThat(result.getType(), is(DataTypes.BOOLEAN_TYPE));
      assertThat(result.contentLong, is(1L));
      assertThat(result.content, nullValue());
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testIntegerValue() {
      Value result = JSONValueConverter.fromJSON(JSON.parse("22"), null);
      assertThat(result.getType(), is(DataTypes.INT_TYPE));
      assertThat(result.contentLong, is(22L));
      assertThat(result.content, nullValue());
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testDoubleSafeIntegerValue() {
      Value result = JSONValueConverter.fromJSON(JSON.parse("22.0"), null);
      assertThat(result.getType(), is(DataTypes.FLOAT_TYPE));
      assertThat(result.contentLong, is(Double.doubleToRawLongBits(22.0d)));
      assertThat(result.content, nullValue());
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testDoubleUnsafeIntegerValue() {
      Value result = JSONValueConverter.fromJSON(22.22d, null);
      assertThat(result.getType(), is(DataTypes.FLOAT_TYPE));
      assertThat(result.contentLong, is(Double.doubleToRawLongBits(22.22d)));
      assertThat(result.content, nullValue());
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testStringBufferWithBufferHint() {
      Value result = JSONValueConverter.fromJSON("test buffer", DataTypes.BUFFER_TYPE);
      assertThat(result.getType(), is(DataTypes.BUFFER_TYPE));
      assertThat(result.contentLong, is(0L));
      assertThat(result.content, instanceOf(StringBuffer.class));
      assertThat(result.content.toString(), is("test buffer"));
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testStringBufferWithoutBufferHint() {
      Value result = JSONValueConverter.fromJSON("test string", null);
      assertThat(result.getType(), is(DataTypes.STRING_TYPE));
      assertThat(result.contentLong, is(0L));
      assertThat(result.content, nullValue());
      assertThat(result.contentString, is("test string"));
    }

    @Test
    public void testStringWithBufferHint() {
      Value result = JSONValueConverter.fromJSON("test buffer", DataTypes.BUFFER_TYPE);
      assertThat(result.getType(), is(DataTypes.BUFFER_TYPE));
      assertThat(result.contentLong, is(0L));
      assertThat(result.content, instanceOf(StringBuffer.class));
      assertThat(result.content.toString(), is("test buffer"));
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testStringWithoutBufferHint() {
      Value result = JSONValueConverter.fromJSON("test string", null);
      assertThat(result.getType(), is(DataTypes.STRING_TYPE));
      assertThat(result.contentLong, is(0L));
      assertThat(result.content, nullValue());
      assertThat(result.contentString, is("test string"));
    }

    @Test
    public void testJavaMap() {
      Value result = JSONValueConverter.fromJSON(JSON.parse("{ \"key\": 22 }"), null);
      assertThat(result.getType(), instanceOf(AggregateType.class));

      AggregateType aggregateType = (AggregateType) result.getType();
      assertThat(aggregateType.getIndexType(), is(DataTypes.STRING_TYPE));
      assertThat(aggregateType.getDataType(), is(DataTypes.INT_TYPE));

      assertThat(result.contentLong, is(0L));
      assertThat((Map<?, ?>) result.content, aMapWithSize(1));
      assertThat(
          ((Map<?, ?>) result.content).get(DataTypes.makeStringValue("key")),
          is(DataTypes.makeIntValue(22)));
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testEmptyMap() {
      Value result = JSONValueConverter.fromJSON(JSON.parse("{}"), null);
      assertThat(result.getType(), instanceOf(AggregateType.class));

      AggregateType aggregateType = (AggregateType) result.getType();
      assertThat(aggregateType.getIndexType(), is(DataTypes.ANY_TYPE));
      assertThat(aggregateType.getDataType(), is(DataTypes.ANY_TYPE));

      assertThat(result.contentLong, is(0L));
      assertThat((Map<?, ?>) result.content, aMapWithSize(0));
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testEmptyMapWithHint() {
      Value result = JSONValueConverter.fromJSON(JSON.parse("{}"), DataTypes.STRING_TO_INT_TYPE);
      assertThat(result.getType(), instanceOf(AggregateType.class));

      AggregateType aggregateType = (AggregateType) result.getType();
      assertThat(aggregateType.getIndexType(), is(DataTypes.STRING_TYPE));
      assertThat(aggregateType.getDataType(), is(DataTypes.INT_TYPE));

      assertThat(result.contentLong, is(0L));
      assertThat((Map<?, ?>) result.content, aMapWithSize(0));
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testJavaList() {
      Value result = JSONValueConverter.fromJSON(JSON.parse("[\"a\", \"b\", \"c\"]"), null);
      assertThat(result.getType(), instanceOf(AggregateType.class));

      AggregateType aggregateType = (AggregateType) result.getType();
      assertThat(aggregateType.getDataType(), is(DataTypes.STRING_TYPE));
      assertThat(aggregateType.getSize(), is(3));

      assertThat(result.contentLong, is(0L));
      assertThat(result.content, instanceOf(Value[].class));
      assertThat(
          List.of((Value[]) result.content),
          contains(
              DataTypes.makeStringValue("a"),
              DataTypes.makeStringValue("b"),
              DataTypes.makeStringValue("c")));
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testEmptyList() {
      Value result = JSONValueConverter.fromJSON(JSON.parse("[]"), null);
      assertThat(result.getType(), instanceOf(AggregateType.class));

      AggregateType aggregateType = (AggregateType) result.getType();
      assertThat(aggregateType.getDataType(), is(DataTypes.ANY_TYPE));
      assertThat(aggregateType.getSize(), is(0));

      assertThat(result.contentLong, is(0L));
      assertThat(result.content, instanceOf(Value[].class));
      assertThat(((Value[]) result.content), emptyArray());
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testEmptyListWithHint() {
      Value result =
          JSONValueConverter.fromJSON(
              JSON.parse("[]"), new AggregateType(DataTypes.STRING_TYPE, 0));
      assertThat(result.getType(), instanceOf(AggregateType.class));

      AggregateType aggregateType = (AggregateType) result.getType();
      assertThat(aggregateType.getDataType(), is(DataTypes.STRING_TYPE));
      assertThat(aggregateType.getSize(), is(0));

      assertThat(result.contentLong, is(0L));
      assertThat(result.content, instanceOf(Value[].class));
      assertThat(((Value[]) result.content), emptyArray());
      assertThat(result.contentString, nullValue());
    }

    @Test
    public void testDefaultCase() {
      Object unknownObject = new Object();
      Value result = JSONValueConverter.fromJSON(unknownObject, null);
      assertThat(result.getType(), is(DataTypes.STRING_TYPE));
      assertThat(result.contentLong, is(0L));
      assertThat(result.content, nullValue());
      assertThat(result.contentString, is(unknownObject.toString()));
    }
  }

  @Nested
  class FindMatchingFunction {
    private static List<Arguments> argumentsSource() {
      return List.of(
          // No arguments.
          Arguments.of("my_meat", "[]", List.of()),
          // Basic arguments.
          Arguments.of("truncate", "[22.0]", List.of(DataTypes.makeFloatValue(22.0d))),
          Arguments.of("url_decode", "[\"abc\"]", List.of(DataTypes.makeStringValue("abc"))),
          // Overloaded argument.
          Arguments.of("to_string", "[22]", List.of(DataTypes.makeIntValue(22L))),
          // Coerced argument.
          Arguments.of("truncate", "[22]", List.of(DataTypes.makeFloatValue(22.0d))),
          // Array argument.
          Arguments.of(
              "count", "[[]]", List.of(new ArrayValue(new AggregateType(DataTypes.ANY_TYPE, 0)))),
          Arguments.of(
              "count",
              "[[22]]",
              List.of(
                  new ArrayValue(
                      new AggregateType(DataTypes.INT_TYPE, 1),
                      List.of(DataTypes.makeIntValue(22L))))),
          // Varargs.
          Arguments.of(
              "max",
              "[22, 33, 44]",
              List.of(
                  DataTypes.makeIntValue(22L),
                  DataTypes.makeIntValue(33L),
                  DataTypes.makeIntValue(44L))),
          // Buffer argument.
          Arguments.of(
              "buffer_to_file",
              "[\"abc\", \"data/x.txt\"]",
              List.of(
                  new Value(DataTypes.BUFFER_TYPE, 0, null, new StringBuffer("abc")),
                  DataTypes.makeStringValue("data/x.txt"))),
          Arguments.of(
              "write_ccs",
              "[\"abc\", \"ccs/x.ccs\"]",
              List.of(
                  new Value(DataTypes.BUFFER_TYPE, 0, null, new StringBuffer("abc")),
                  DataTypes.makeStringValue("ccs/x.ccs"))));
    }

    @ParameterizedTest
    @MethodSource("argumentsSource")
    public void works(String functionName, String jsonArgs, List<Value> expected) {
      var args = JSON.parseArray(jsonArgs);
      var functionWithArgs =
          new JSONValueConverter()
              .findMatchingFunctionConvertArgs(
                  RuntimeLibrary.getFunctions(), functionName, args.toArray());
      assertThat(functionWithArgs, notNullValue());
      assertThat(functionWithArgs.function().name, is(functionName));
      assertThat(functionWithArgs.ashArgs(), is(expected));
    }

    @ParameterizedTest
    @CsvSource(
        value = {"non_existent_function; []", "my_meat; [0]", "truncate; [\"a\"]", "max; []"},
        delimiter = ';')
    public void failsIfNoMatch(String functionName, String jsonArgs) {
      var args = JSON.parseArray(jsonArgs);
      var functionWithArgs =
          new JSONValueConverter()
              .findMatchingFunctionConvertArgs(
                  RuntimeLibrary.getFunctions(), functionName, args.toArray());
      assertThat(functionWithArgs, nullValue());
    }
  }
}
