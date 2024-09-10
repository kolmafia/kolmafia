package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class ValueConverter {
  /* This is the closest thing that we have to undefined in our Value type system. Since javascript
   * void functions return undefined, and we currently don't support undefined values as parameters,
   * this is good enough for now.
   */
  private static final Type UNDEFINED_TYPE = new Type("undefined", TypeSpec.VOID);
  private static final Value UNDEFINED =
      new Value(UNDEFINED_TYPE) {
        @Override
        public String toString() {
          return "undefined";
        }
      };

  private final Context cx;
  private final Scriptable scope;

  public ValueConverter(Context cx, Scriptable scope) {
    this.cx = cx;
    this.scope = scope;
  }

  private Scriptable asObject(MapValue mapValue) {
    Scriptable result = cx.newObject(scope);
    for (Value key : mapValue.keys()) {
      Value value = mapValue.aref(key);
      if (key.getType().equals(DataTypes.STRING_TYPE)
          || DataTypes.enumeratedTypes.contains(key.getType()) && key.contentString.length() > 0) {
        ScriptableObject.putProperty(result, key.contentString, asJava(value));
      } else if (key.getType().equals(DataTypes.INT_TYPE)
          || DataTypes.enumeratedTypes.contains(key.getType()) && key.contentLong > 0) {
        ScriptableObject.putProperty(result, (int) key.contentLong, asJava(value));
      } else {
        throw new ScriptException(
            "Maps may only have keys of type string, int or an enumerated type.");
      }
    }
    return result;
  }

  private Scriptable asObject(RecordValue recordValue) {
    Scriptable result = cx.newObject(scope);
    for (Value key : recordValue.keys()) {
      Value value = recordValue.aref(key);
      String keyString = key.contentString;
      if (key.getType().equals(DataTypes.INT_TYPE)) {
        keyString = Long.toString(key.contentLong);
      } else if (!key.getType().equals(DataTypes.STRING_TYPE)) {
        throw new ScriptException("Maps may only have string keys.");
      }
      ScriptableObject.putProperty(result, keyString, asJava(value));
    }
    return result;
  }

  private Scriptable asNativeArray(ArrayValue arrayValue) {
    return cx.newArray(
        scope, Arrays.stream((Value[]) arrayValue.content).map(this::asJava).toArray());
  }

  private Scriptable asNativeArray(PluralValue arrayValue) {
    return cx.newArray(
        scope, Arrays.stream((Value[]) arrayValue.content).map(this::asJava).toArray());
  }

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
      return asObject((MapValue) value);
    } else if (value instanceof ArrayValue) {
      return asNativeArray((ArrayValue) value);
    } else if (DataTypes.enumeratedTypes.contains(value.getType())) {
      return EnumeratedWrapper.wrap(scope, value.asProxy().getClass(), value);
    } else if (value instanceof RecordValue) {
      return asObject((RecordValue) value);
    } else if (value instanceof PluralValue) {
      return asNativeArray((PluralValue) value);
    } else {
      // record type, ...?
      return value;
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

  private MapValue convertNativeObject(NativeObject nativeObject, Type typeHint) {
    if (nativeObject.size() == 0) {
      if (typeHint instanceof AggregateType aggregateTypeHint && aggregateTypeHint.getSize() < 0) {
        return new MapValue(
            new AggregateType(aggregateTypeHint.getDataType(), aggregateTypeHint.getIndexType()));
      } else {
        return new MapValue(new AggregateType(DataTypes.ANY_TYPE, DataTypes.ANY_TYPE));
      }
    }

    Type dataType = null;
    Type indexType = null;
    for (Entry<?, ?> entry : nativeObject.entrySet()) {
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
    for (Entry<?, ?> entry : nativeObject.entrySet()) {
      Value key = fromJava(entry.getKey());
      Value value = fromJava(entry.getValue());
      if (key == null) {
        // This will likely never execute, since if your key is `null`, then it
        // will turn into the string "null".
        throw new EvaluatorException(
            "Null / undefined keys in JS objects cannot be converted to ASH.");
      }
      if (value == null) {
        throw new EvaluatorException(
            "Null / undefined values in JS objects cannot be converted to ASH.");
      }
      Value keyCoerced = coerce(key, indexType);

      underlyingMap.put(keyCoerced, value);
    }

    return new MapValue(new AggregateType(dataType, indexType), underlyingMap);
  }

  private ArrayValue convertNativeArray(NativeArray nativeArray, Type typeHint) {
    if (nativeArray.size() == 0) {
      if (typeHint instanceof AggregateType aggregateTypeHint && aggregateTypeHint.getSize() >= 0) {
        return new ArrayValue(new AggregateType(aggregateTypeHint.getDataType(), 0));
      } else {
        return new ArrayValue(new AggregateType(DataTypes.ANY_TYPE, 0));
      }
    }

    Value firstElement = fromJava(nativeArray.get(0));
    Type elementType = firstElement == null ? DataTypes.ANY_TYPE : firstElement.getType();
    List<Value> result = new ArrayList<>();
    for (Object element : nativeArray) {
      if (element == null) {
        throw new EvaluatorException(
            "Null / undefined values in JS arrays cannot be converted to ASH.");
      }

      result.add(fromJava(element));
    }
    return new ArrayValue(new AggregateType(elementType, nativeArray.size()), result);
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
    } else if (object instanceof Float || object instanceof Double) {
      return DataTypes.makeFloatValue(((Number) object).doubleValue());
    } else if (object instanceof String) {
      return DataTypes.makeStringValue((String) object);
    } else if (object instanceof StringBuffer || object instanceof ConsString) {
      return DataTypes.makeStringValue(object.toString());
    } else if (object instanceof MonsterData) {
      return DataTypes.makeMonsterValue((MonsterData) object);
    } else if (object instanceof EnumeratedWrapper) {
      return ((EnumeratedWrapper) object).getWrapped();
    } else if (object instanceof AshStub) {
      return DataTypes.makeStringValue("[function " + ((AshStub) object).getFunctionName() + "]");
    } else if (object instanceof NativeObject) {
      return convertNativeObject((NativeObject) object, typeHint);
    } else if (object instanceof NativeArray) {
      return convertNativeArray((NativeArray) object, typeHint);
    } else if (object instanceof Value) {
      return (Value) object;
    } else if (Undefined.isUndefined(object)) {
      return UNDEFINED;
    } else {
      return DataTypes.makeStringValue(object.toString());
    }
  }

  public Value fromJava(Object object) {
    return fromJava(object, null);
  }
}
