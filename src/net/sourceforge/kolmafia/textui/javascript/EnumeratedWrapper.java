package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.parsetree.CompositeValue;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class EnumeratedWrapper extends ScriptableObject {
  private static final long serialVersionUID = 1L;

  // Make sure each wrapper is a singleton, so that equality comparison works in JS.
  private static final Map<Scriptable, Map<Value, EnumeratedWrapper>> registry = new HashMap<>();

  private final Class<?> recordValueClass;
  // NB: This wrapped value is NOT the proxy record type version.
  // Instead, it's the plain Value that can be turned into a proxy record via asProxy.
  private final Value wrapped;

  private EnumeratedWrapper(Class<?> recordValueClass, Value wrapped) {
    this.recordValueClass = recordValueClass;
    this.wrapped = wrapped;
  }

  public static EnumeratedWrapper wrap(Scriptable scope, Class<?> recordValueClass, Value wrapped) {
    scope = getTopLevelScope(scope);
    Scriptable proto = scope.getPrototype();
    while (proto != null && proto != getObjectPrototype(scope)) {
      scope = proto;
      proto = scope.getPrototype();
    }

    Map<Value, EnumeratedWrapper> subRegistry = registry.getOrDefault(scope, null);
    if (subRegistry == null) {
      subRegistry = new HashMap<>();
      registry.put(scope, subRegistry);
    }

    EnumeratedWrapper existing = subRegistry.getOrDefault(wrapped, null);
    if (existing == null) {
      existing = new EnumeratedWrapper(recordValueClass, wrapped);
      existing.setPrototype(
          EnumeratedWrapperPrototype.getPrototypeInstance(scope, wrapped.getType()));
      existing.sealObject();
      subRegistry.put(wrapped, existing);
    }

    return existing;
  }

  public static void cleanup(Scriptable scope) {
    registry.remove(scope);
  }

  public Value getWrapped() {
    return wrapped;
  }

  @Override
  public String getClassName() {
    return recordValueClass.getName();
  }

  @Override
  public String toString() {
    return wrapped.toString();
  }

  public static Object toJSON(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
    Scriptable scope = ScriptableObject.getTopLevelScope(thisObj);
    ValueConverter coercer = new ValueConverter(cx, scope);
    var proxy = ((EnumeratedWrapper) thisObj).wrapped.asProxy();

    if (!(proxy instanceof CompositeValue compValue)) return coercer.asJava(proxy);

    var result = cx.newObject(thisObj);

    for (Value keyObject : compValue.keys()) {
      var key = JavascriptRuntime.toCamelCase(keyObject.toString());
      var value = compValue.aref(keyObject).toJSON();
      ScriptableObject.putProperty(result, key, value);
    }

    return result;
  }

  public static Object constructDefaultValue() {
    return new EnumeratedWrapper(
        ProxyRecordValue.ItemProxy.class,
        new ProxyRecordValue.ItemProxy(DataTypes.makeIntValue(1)));
  }

  private static EnumeratedWrapper getValue(Scriptable scope, Type type, Value value) {
    Class<?> proxyRecordValueClass = null;

    for (Class<?> testRecordValueClass : ProxyRecordValue.class.getDeclaredClasses()) {
      if (testRecordValueClass.getSimpleName().toLowerCase().startsWith(type.getName())) {
        proxyRecordValueClass = testRecordValueClass;
        break;
      }
    }

    return EnumeratedWrapper.wrap(scope, proxyRecordValueClass, value);
  }

  public static EnumeratedWrapper getNone(Scriptable scope, Type type) {
    Value rawValue = type.parseValue(null, true);
    return getValue(scope, type, rawValue);
  }

  private static EnumeratedWrapper getOne(Scriptable scope, Type type, Object key) {
    Value rawValue = type.initialValue();
    if (key instanceof String || key instanceof ConsString) {
      rawValue = type.parseValue(key.toString(), false);
    } else if (key instanceof Float || key instanceof Double) {
      rawValue = type.makeValue((int) Math.round((Double) key), false);
    } else if (key instanceof Number) {
      rawValue = type.makeValue(((Number) key).intValue(), false);
    }

    if (rawValue == null) {
      throw new ScriptException("Bad " + type.getName() + " value: " + key.toString());
    }

    return getValue(scope, type, rawValue);
  }

  public static Object genericGet(
      Context cx, Scriptable thisObject, Object[] args, Function functionObject) {
    if (args.length != 1) {
      throw new ScriptException(
          "<Class>.get takes only one argument: a number/string or an array.");
    }

    String typeName = (String) ScriptableObject.getProperty(functionObject, "typeName");
    Type type = DataTypes.simpleTypes.find(typeName);

    Scriptable scope = ScriptableObject.getTopLevelScope(thisObject);

    Object arg = args[0];
    if (arg instanceof Iterable) {
      List<Object> result = new ArrayList<>();
      for (Object key : (Iterable<?>) arg) {
        result.add(getOne(scope, type, key));
      }
      return cx.newArray(scope, result.toArray());
    } else {
      return getOne(scope, type, arg);
    }
  }

  public static Object all(
      Context cx, Scriptable thisObject, Object[] args, Function functionObject) {
    if (args.length != 0) {
      throw new ScriptException("<Class>.all does not take arguments.");
    }

    String typeName = (String) ScriptableObject.getProperty(functionObject, "typeName");
    Type type = DataTypes.simpleTypes.find(typeName);

    Scriptable scope = ScriptableObject.getTopLevelScope(thisObject);
    ValueConverter coercer = new ValueConverter(cx, scope);

    return cx.newArray(
        scope, Arrays.stream((Value[]) type.allValues().content).map(coercer::asJava).toArray());
  }
}
