package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;

public abstract class ValueConverter<ObjectType> {
  public static class ValueConverterException extends RuntimeException {
    public ValueConverterException(String message) {
      super(message);
    }
  }

  protected abstract ObjectType asJavaObject(MapValue mapValue) throws ValueConverterException;

  protected abstract ObjectType asJavaObject(RecordValue recordValue)
      throws ValueConverterException;

  protected abstract ObjectType asJavaArray(ArrayValue arrayValue) throws ValueConverterException;

  protected abstract ObjectType asJavaArray(PluralValue arrayValue) throws ValueConverterException;

  public Object asJava(Value value) {
    if (value == null) return null;
    else if (value.getType().equals(DataTypes.VOID_TYPE)) {
      return null;
    } else if (value.getType().equals(DataTypes.BOOLEAN_TYPE)) {
      return value.contentLong != 0;
    } else if (value.getType().equals(DataTypes.INT_TYPE)) {
      return value.contentLong;
    } else if (value.getType().equals(DataTypes.FLOAT_TYPE)) {
      return value.floatValue();
    } else if (value.getType().equals(DataTypes.STRING_TYPE)
        || value.getType().equals(DataTypes.STRICT_STRING_TYPE)) {
      return value.contentString;
    } else if (value.getType().equals(DataTypes.BUFFER_TYPE)) {
      return value.content.toString();
    } else if (value.getType().equals(DataTypes.MATCHER_TYPE)) {
      // This should not happen.
      return null;
    } else if (value instanceof MapValue) {
      return asJavaObject((MapValue) value);
    } else if (value instanceof ArrayValue) {
      return asJavaArray((ArrayValue) value);
    } else if (value instanceof RecordValue) {
      return asJavaObject((RecordValue) value);
    } else if (value instanceof PluralValue) {
      return asJavaArray((PluralValue) value);
    } else if (value.asProxy() instanceof RecordValue proxyValue) {
      return asJavaObject(proxyValue);
    } else {
      // record type, ...?
      throw new ValueConverterException("Unrecognized Value of type " + value.getType().toString());
    }
  }

  private Value coerce(Value value, Type targetType) {
    Type valueType = value.getType();
    if (targetType.equals(valueType)) {
      return value;
    } else if (targetType.equals(TypeSpec.STRING)) {
      return value.toStringValue();
    } else if (targetType.equals(TypeSpec.INT) && valueType.equals(TypeSpec.FLOAT)) {
      return value.toIntValue();
    } else if (targetType.equals(TypeSpec.FLOAT) && valueType.equals(TypeSpec.INT)) {
      return value.toFloatValue();
    } else {
      return null;
    }
  }

  private MapValue convertJavaMap(Map<?, ?> javaMap, Type typeHint) {
    if (javaMap.size() == 0) {
      if (typeHint instanceof AggregateType aggregateTypeHint && aggregateTypeHint.getSize() < 0) {
        return new MapValue(
            new AggregateType(aggregateTypeHint.getDataType(), aggregateTypeHint.getIndexType()));
      } else {
        return new MapValue(new AggregateType(DataTypes.ANY_TYPE, DataTypes.ANY_TYPE));
      }
    }

    Type dataType = null;
    Type indexType = null;
    for (Object entryObject : javaMap.entrySet()) {
      var entry = (Entry<?, ?>) entryObject;
      Value key = fromJava(entry.getKey());
      Value value = fromJava(entry.getValue());

      dataType = value != null ? value.getType() : null;
      indexType = key != null ? key.getType() : null;
      if (TypeSpec.FLOAT.equals(indexType)) {
        // Convert float index to int, since it doesn't make sense in JS anyway.
        indexType = DataTypes.INT_TYPE;
      }
      break;
    }
    if (dataType == null) {
      dataType = DataTypes.ANY_TYPE;
    }
    if (indexType == null) {
      indexType = DataTypes.ANY_TYPE;
    }

    Map<Value, Value> underlyingMap = new TreeMap<>();
    for (Object entryObject : javaMap.entrySet()) {
      var entry = (Entry<?, ?>) entryObject;
      Value key = fromJava(entry.getKey());
      Value value = fromJava(entry.getValue());
      if (key == null) {
        // This will likely never execute, since if your key is `null`, then it
        // will turn into the string "null".
        throw new ValueConverterException(
            "Null / undefined keys in JS objects cannot be converted to ASH.");
      }
      if (value == null) {
        throw new ValueConverterException(
            "Null / undefined values in JS objects cannot be converted to ASH.");
      }
      Value keyCoerced = coerce(key, indexType);

      underlyingMap.put(keyCoerced, value);
    }

    return new MapValue(new AggregateType(dataType, indexType), underlyingMap);
  }

  private ArrayValue convertJavaArray(List<?> javaArray, Type typeHint) {
    if (javaArray.size() == 0) {
      if (typeHint instanceof AggregateType aggregateTypeHint && aggregateTypeHint.getSize() >= 0) {
        return new ArrayValue(new AggregateType(aggregateTypeHint.getDataType(), 0));
      } else {
        return new ArrayValue(new AggregateType(DataTypes.ANY_TYPE, 0));
      }
    }

    Value firstElement = fromJava(javaArray.get(0));
    Type elementType = firstElement == null ? DataTypes.ANY_TYPE : firstElement.getType();
    List<Value> result = new ArrayList<>();
    for (Object element : javaArray) {
      if (element == null) {
        throw new ValueConverterException(
            "Null / undefined values in JS arrays cannot be converted to ASH.");
      }

      result.add(fromJava(element));
    }
    return new ArrayValue(new AggregateType(elementType, javaArray.size()), result);
  }

  public Value fromJava(Object object, Type typeHint) {
    if (object == null) return null;
    else if (object instanceof Boolean) {
      return DataTypes.makeBooleanValue((Boolean) object);
    } else if (object instanceof Byte
        || object instanceof Short
        || object instanceof Integer
        || object instanceof Long
        || object instanceof Double d && JavascriptNumbers.isDoubleSafeInteger(d)) {
      return DataTypes.makeIntValue(((Number) object).longValue());
    } else if (object instanceof Number) {
      return DataTypes.makeFloatValue(((Number) object).doubleValue());
    } else if (object instanceof String) {
      return DataTypes.makeStringValue((String) object);
    } else if (object instanceof StringBuffer) {
      return DataTypes.makeStringValue(object.toString());
    } else if (object instanceof MonsterData) {
      return DataTypes.makeMonsterValue((MonsterData) object);
    } else if (object instanceof EnumeratedWrapper) {
      return ((EnumeratedWrapper) object).getWrapped();
    } else if (object instanceof AshStub) {
      return DataTypes.makeStringValue("[function " + ((AshStub) object).getFunctionName() + "]");
    } else if (object instanceof Map) {
      return convertJavaMap((Map<?, ?>) object, typeHint);
    } else if (object instanceof List) {
      return convertJavaArray((List<?>) object, typeHint);
    } else if (object instanceof Value) {
      return (Value) object;
    } else {
      return DataTypes.makeStringValue(object.toString());
    }
  }

  public Value fromJava(Object object) {
    return fromJava(object, null);
  }
}
