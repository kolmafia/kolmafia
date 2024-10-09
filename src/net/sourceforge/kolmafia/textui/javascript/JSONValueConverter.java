package net.sourceforge.kolmafia.textui.javascript;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public class JSONValueConverter extends ValueConverter<Object> {

  private static record TypeAndIdentifier(Type type, String identifier) {
    public TypeAndIdentifier(Value value) {
      this(value.getType(), value.contentString != null ? value.contentString : value.toString());
    }

    @Override
    public int hashCode() {
      return this.type.hashCode() ^ this.identifier.hashCode();
    }
  }

  private final Set<TypeAndIdentifier> processedObjects = new HashSet<>();

  public static Object asJSON(Value value) {
    return new JSONValueConverter().asJava(value);
  }

  public static Value fromJSON(Object json) {
    return new JSONValueConverter().fromJava(json);
  }

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

    if (recordValue instanceof ProxyRecordValue proxyRecordValue) {
      Type underlyingType = proxyRecordValue.getUnderlyingValue().getType();
      String typeName = underlyingType.getName();
      String typeNameCaps = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
      result.put("objectType", typeNameCaps);
      result.put(
          "identifierString",
          recordValue.content == null ? recordValue.contentString : recordValue.content.toString());
      if (underlyingType.isIntLike()) {
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

  @Override
  public Object asJava(Value value) {
    if (value instanceof ProxyRecordValue proxyRecordValue) {
      value = proxyRecordValue.getUnderlyingValue();
    }
    if (DataTypes.enumeratedTypes.contains(value.getType())) {
      var id = new TypeAndIdentifier(value);
      // This logic prevents circular references.
      Object result = processedObjects.add(id) ? super.asJava(value) : new JSONObject();
      processedObjects.remove(id);
      return result;
    } else {
      return super.asJava(value);
    }
  }
}
