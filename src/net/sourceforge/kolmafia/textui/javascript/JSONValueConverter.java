package net.sourceforge.kolmafia.textui.javascript;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.Arrays;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class JSONValueConverter extends ValueConverter<Object> {

  @Override
  protected Object asJavaObject(MapValue mapValue) {
    JSONObject result = new JSONObject();
    for (Value key : mapValue.keys()) {
      Value value = mapValue.aref(key);
      if (key.getType().equals(DataTypes.STRING_TYPE)
          || DataTypes.enumeratedTypes.contains(key.getType()) && key.contentString.length() > 0) {
        result.put(key.contentString, asJava(value));
      } else if (key.getType().equals(DataTypes.INT_TYPE)
          || DataTypes.enumeratedTypes.contains(key.getType()) && key.contentLong > 0) {
        result.put(String.valueOf(key.contentLong), asJava(value));
      } else {
        throw new ValueConverterException(
            "Maps may only have keys of type string, int or an enumerated type.");
      }
    }
    return result;
  }

  @Override
  protected Object asJavaObject(RecordValue recordValue) {
    JSONObject result = new JSONObject();
    for (Value key : recordValue.keys()) {
      Value value = recordValue.aref(key);
      String keyString = key.contentString;
      if (key.getType().equals(DataTypes.INT_TYPE)) {
        keyString = Long.toString(key.contentLong);
      } else if (!key.getType().equals(DataTypes.STRING_TYPE)) {
        throw new ValueConverterException("Records may only have string keys.");
      }
      result.put(keyString, asJava(value));
    }

    if (DataTypes.enumeratedTypes.contains(recordValue.type)) {
      result.put("objectType", recordValue.type.name);
      result.put("identifierString", recordValue.toString());
      if (DataTypes.enumeratedTypesIntLike.contains(recordValue.type)) {
        result.put("identifierNumber", recordValue.contentLong);
      }
    }
    return result;
  }

  @Override
  protected Object asJavaArray(ArrayValue arrayValue) {
    return JSONArray.from(Arrays.stream((Value[]) arrayValue.content).map(this::asJava).toList());
  }

  @Override
  protected Object asJavaArray(PluralValue arrayValue) {
    return JSONArray.from(Arrays.stream((Value[]) arrayValue.content).map(this::asJava).toList());
  }
}
