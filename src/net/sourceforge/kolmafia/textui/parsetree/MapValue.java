package net.sourceforge.kolmafia.textui.parsetree;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;

public class MapValue extends AggregateValue {
  public MapValue(final AggregateType type) {
    super(type);
    this.content = new TreeMap<Value, Value>();
  }

  public MapValue(final AggregateType type, boolean caseInsensitive) {
    super(type);
    this.content =
        caseInsensitive
            ? new TreeMap<Value, Value>(Value.ignoreCaseComparator)
            : new TreeMap<Value, Value>();
  }

  public MapValue(final AggregateType type, Map<?, ?> value) {
    super(type);
    this.content = value;
  }

  @SuppressWarnings("unchecked")
  private Map<Value, Value> getMap() {
    return (Map<Value, Value>) this.content;
  }

  @Override
  public Value aref(final Value key, final AshRuntime interpreter) {
    Map<Value, Value> map = this.getMap();
    return map.get(key);
  }

  @Override
  public void aset(final Value key, Value val, final AshRuntime interpreter) {
    Map<Value, Value> map = this.getMap();

    Type dataType = this.getDataType();
    Type baseType = dataType.getBaseType();
    Type valType = val.getType();

    if (baseType.equals(valType)) {
      map.put(key, val);
    } else if (baseType.equals(DataTypes.TYPE_STRING)) {
      map.put(key, val.toStringValue());
    } else if (baseType.equals(DataTypes.TYPE_INT) && valType.equals(DataTypes.TYPE_FLOAT)) {
      map.put(key, val.toIntValue());
    } else if (baseType.equals(DataTypes.TYPE_FLOAT) && valType.equals(DataTypes.TYPE_INT)) {
      map.put(key, val.toFloatValue());
    } else {
      throw interpreter.runtimeException(
          "Internal error: Cannot assign " + valType + " to " + baseType);
    }
  }

  @Override
  public Value remove(final Value key, final AshRuntime interpreter) {
    // Look through all active foreach loops since they are
    // implemented via iterators and you must use that iterator's
    // remove method on the current element only.
    for (int i = interpreter.iterators.size() - 3; i >= 0; i -= 3) {
      AggregateValue slice = (AggregateValue) interpreter.iterators.get(i + 1);
      if (slice != this) {
        continue;
      }
      Value keyValue = (Value) interpreter.iterators.get(i);
      if (!key.equals(keyValue)) {
        throw interpreter.runtimeException("Removing non-current key within foreach");
      }

      // This is removing the current element of a foreach iterator.
      // That works.

      // Return the current value
      Value rv = this.aref(key, interpreter);

      @SuppressWarnings("unchecked")
      Iterator<Value> it = (Iterator<Value>) interpreter.iterators.get(i + 2);
      it.remove();

      // NULL-out the key associated with this iterator in
      // case remove is used more than once on the same key
      interpreter.iterators.set(i, null);

      return rv;
    }

    Map<Value, Value> map = this.getMap();
    return map.remove(key);
  }

  @Override
  public void clear() {
    Map<Value, Value> map = this.getMap();
    map.clear();
  }

  @Override
  public int count() {
    Map<Value, Value> map = this.getMap();
    return map.size();
  }

  @Override
  public boolean contains(final Value key) {
    Map<Value, Value> map = this.getMap();
    return map.containsKey(key);
  }

  @Override
  public Value[] keys() {
    Set<Value> set = this.getMap().keySet();
    Value[] keys = new Value[set.size()];
    set.toArray(keys);
    return keys;
  }

  @Override
  public Iterator<Value> iterator() {
    Set<Value> set = this.getMap().keySet();
    return set.iterator();
  }
}
