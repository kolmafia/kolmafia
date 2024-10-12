package net.sourceforge.kolmafia.textui.javascript;

import java.util.Arrays;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.parsetree.ArrayValue;
import net.sourceforge.kolmafia.textui.parsetree.MapValue;
import net.sourceforge.kolmafia.textui.parsetree.PluralValue;
import net.sourceforge.kolmafia.textui.parsetree.RecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;

public class ScriptableValueConverter extends ValueConverter<Scriptable> {
  /* This is the closest thing that we have to undefined in our Value type system. Since javascript
   * void functions return undefined, and we currently don't support undefined values as parameters,
   * this is good enough for now.
   */
  private static final Type UNDEFINED_TYPE = new Type("undefined", DataTypes.TypeSpec.VOID);
  private static final Value UNDEFINED =
      new Value(UNDEFINED_TYPE) {
        @Override
        public String toString() {
          return "undefined";
        }
      };

  private final Context cx;
  private final Scriptable scope;

  public ScriptableValueConverter(Context cx, Scriptable scope) {
    this.cx = cx;
    this.scope = scope;
  }

  @Override
  protected Scriptable asJavaObject(MapValue mapValue) {
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
        throw new ValueConverterException(
            "Maps may only have keys of type string, int or an enumerated type.");
      }
    }
    return result;
  }

  @Override
  protected Scriptable asJavaObject(RecordValue recordValue) {
    Scriptable result = cx.newObject(scope);
    for (Value key : recordValue.keys()) {
      Value value = recordValue.aref(key);
      String keyString = key.contentString;
      if (key.getType().equals(DataTypes.INT_TYPE)) {
        keyString = Long.toString(key.contentLong);
      } else if (!key.getType().equals(DataTypes.STRING_TYPE)) {
        throw new ValueConverterException("Records may only have string keys.");
      }
      ScriptableObject.putProperty(result, keyString, asJava(value));
    }
    return result;
  }

  @Override
  protected Scriptable asJavaArray(ArrayValue arrayValue) {
    return cx.newArray(
        scope, Arrays.stream((Value[]) arrayValue.content).map(this::asJava).toArray());
  }

  @Override
  protected Scriptable asJavaArray(PluralValue arrayValue) {
    return cx.newArray(
        scope, Arrays.stream((Value[]) arrayValue.content).map(this::asJava).toArray());
  }

  @Override
  public Value fromJava(Object object, Type typeHint) {
    if (Undefined.isUndefined(object)) {
      return UNDEFINED;
    } else if (object instanceof ConsString) {
      return DataTypes.makeStringValue(object.toString());
    } else {
      return super.fromJava(object, typeHint);
    }
  }

  @Override
  public Object asJava(Value value) {
    if (DataTypes.enumeratedTypes.contains(value.getType())) {
      return EnumeratedWrapper.wrap(scope, value.asProxy().getClass(), value);
    } else {
      return super.asJava(value);
    }
  }
}
