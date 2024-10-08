package net.sourceforge.kolmafia.textui.javascript;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordType;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class JSONValueConverterTest {
  private final JSONValueConverter converter = new JSONValueConverter();

  @Nested
  class AsJava {

    @Test
    public void testNullValue() {
      assertThat(converter.asJava(null), is(nullValue()));
    }

    @Test
    public void testVoidType() {
      Value value = new Value();
      assertThat(converter.asJava(value), is(nullValue()));
    }

    @Test
    public void testBooleanTypeTrue() {
      Value value = DataTypes.makeBooleanValue(true);
      assertThat(converter.asJava(value), is(true));
    }

    @Test
    public void testBooleanTypeFalse() {
      Value value = DataTypes.makeBooleanValue(false);
      assertThat(converter.asJava(value), is(false));
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
}
