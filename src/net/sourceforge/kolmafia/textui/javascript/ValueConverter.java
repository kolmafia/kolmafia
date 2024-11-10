package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.mozilla.javascript.Undefined;

public abstract class ValueConverter<ObjectType> {
  public static record FunctionWithArgs(Function function, List<Value> ashArgs) {}

  // These need special treatment. They accept a buffer as a first parameter, but we should cast
  // string to buffer in that position. These are the only functions that take a buffer as an
  // argument at the time of this comment.
  private static final List<String> bufferFunctions = List.of("write_ccs", "buffer_to_file");

  public static class ValueConverterException extends RuntimeException {
    public ValueConverterException(String message) {
      super(message);
    }
  }

  protected abstract ObjectType asJavaObject(MapValue mapValue);

  protected abstract ObjectType asJavaObject(RecordValue recordValue);

  protected abstract ObjectType asJavaArray(ArrayValue arrayValue);

  protected abstract ObjectType asJavaArray(PluralValue arrayValue);

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
    } else if (value instanceof MapValue mapValue) {
      return asJavaObject(mapValue);
    } else if (value instanceof ArrayValue arrayValue) {
      return asJavaArray(arrayValue);
    } else if (value instanceof RecordValue recordValue) {
      return asJavaObject(recordValue);
    } else if (value instanceof PluralValue pluralValue) {
      return asJavaArray(pluralValue);
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
      if (indexType != null && indexType.equals(DataTypes.FLOAT_TYPE)) {
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
    } else if (object instanceof CharSequence && Objects.equals(typeHint, DataTypes.BUFFER_TYPE)) {
      return new Value(
          DataTypes.BUFFER_TYPE,
          null,
          object instanceof StringBuffer ? object : new StringBuffer(object.toString()));
    } else if (object instanceof CharSequence) {
      return DataTypes.makeStringValue(object.toString());
    } else if (object instanceof MonsterData) {
      return DataTypes.makeMonsterValue((MonsterData) object);
    } else if (object instanceof Map) {
      return convertJavaMap((Map<?, ?>) object, typeHint);
    } else if (object instanceof List) {
      return convertJavaArray((List<?>) object, typeHint);
    } else if (object instanceof Value value) {
      return value;
    } else {
      throw new ValueConverterException(
          "Unrecognized Java object of class " + object.getClass().getName() + ".");
    }
  }

  public Value fromJava(Object object) {
    return fromJava(object, null);
  }

  public FunctionWithArgs findMatchingFunctionConvertArgs(
      FunctionList functions, String functionName, Object[] args) {
    if (bufferFunctions.contains(functionName)) {
      // Manually convert string to buffer, since findMatchingFunction cannot match a string
      // argument to a
      // buffer parameter.
      if (args.length > 0 && args[0] instanceof CharSequence cs) {
        args = args.clone();
        args[0] = new Value(DataTypes.BUFFER_TYPE, cs.toString(), new StringBuffer(cs));
      }
    }

    // strip trailing undefined arguments, allowing to pass undefined for optional arguments
    // If we ever support undefined values, this will have to become a bit more elaborate, but for
    // now every undefined value leads to an error anyway, so this is fine.
    int definedArgs = args.length;
    while (definedArgs > 0 && Undefined.isUndefined(args[definedArgs - 1])) {
      definedArgs -= 1;
    }

    // Find library function matching arguments, in two stages.
    // First, designate any arguments where we can't determine type (or aggregate type) from JS
    // as ANY_TYPE, which will be replaced in findMatchingFunction with the target type
    // to force a match. This is mainly relevant for empty arrays and records.
    List<Value> ashArgs = new ArrayList<>();
    for (int i = 0; i < definedArgs; i++) {
      Object original = args[i];
      if (Undefined.isUndefined(original)) {
        throw new ValueConverterException("Passing undefined to an ASH function is not supported.");
      }
      if (original == null) {
        throw new ValueConverterException("Passing null to an ASH function is not supported.");
      }
      Value coerced = fromJava(original);
      if (coerced == null
          || (coerced.getType() instanceof AggregateType agg
              && agg.getDataType().equals(DataTypes.ANY_TYPE))) {
        coerced = new Value(DataTypes.ANY_TYPE);
      }
      ashArgs.add(coerced);
    }

    Function function = functions.findMatchingFunction(functionName, ashArgs, true);
    if (function == null) return null;

    // Second, infer the type for any ANY_TYPE arguments from the closest function match.
    boolean argsChanged = false;
    for (int i = 0; i < ashArgs.size(); i++) {
      if (ashArgs.get(i).getType() != DataTypes.ANY_TYPE) {
        continue;
      }
      Object original = args[i];
      // Try again, this time with a type hint.
      Type typeHint = function.getVariableReferences().get(i).getType();
      Value coerced = fromJava(original, typeHint);
      if (coerced == null || coerced.getType() == DataTypes.ANY_TYPE) {
        throw new ValueConverterException("Could not coerce argument to valid ASH value.");
      }
      ashArgs.set(i, coerced);
      argsChanged = true;
    }
    if (argsChanged) {
      function = functions.findMatchingFunction(functionName, ashArgs, false);
    }

    return new FunctionWithArgs(function, ashArgs);
  }
}
