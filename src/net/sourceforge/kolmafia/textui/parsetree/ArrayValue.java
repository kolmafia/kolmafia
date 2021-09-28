package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.List;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import org.json.JSONArray;
import org.json.JSONException;

public class ArrayValue extends AggregateValue {
  public ArrayValue(final AggregateType type) {
    super(type);

    int size = type.getSize();
    Value[] content = new Value[size];

    Type dataType = type.getDataType();
    for (int i = 0; i < size; ++i) {
      content[i] = dataType.initialValue();
    }
    this.content = content;
  }

  public ArrayValue(final AggregateType type, final List<Value> values) {
    super(type);

    int size = values.size();
    type.setSize(size);
    Value[] content = new Value[size];

    int index = 0;
    for (Value val : values) {
      content[index++] = val;
    }
    this.content = content;
  }

  @Override
  public Value aref(final Value key, final AshRuntime interpreter) {
    Value[] array = (Value[]) this.content;
    int index = (int) key.intValue();
    if (index < 0 || index >= array.length) {
      throw interpreter.runtimeException(
          "Array index [" + index + "] out of bounds (" + array.length + ")");
    }
    return array[index];
  }

  @Override
  public void aset(final Value key, final Value val, final AshRuntime interpreter) {
    Value[] array = (Value[]) this.content;
    int index = (int) key.intValue();
    if (index < 0 || index >= array.length) {
      throw interpreter.runtimeException(
          "Array index [" + index + "] out of bounds (" + array.length + ")");
    }

    Type dataType = array[index].getType();
    Type baseType = dataType.getBaseType();
    Type valType = val.getType();

    if (baseType.equals(valType)) {
      array[index] = val;
    } else if (baseType.equals(DataTypes.TYPE_STRING)) {
      array[index] = val.toStringValue();
    } else if (baseType.equals(DataTypes.TYPE_INT) && valType.equals(DataTypes.TYPE_FLOAT)) {
      array[index] = val.toIntValue();
    } else if (baseType.equals(DataTypes.TYPE_FLOAT) && valType.equals(DataTypes.TYPE_INT)) {
      array[index] = val.toFloatValue();
    } else {
      throw interpreter.runtimeException(
          "Internal error: Cannot assign " + valType + " to " + dataType);
    }
  }

  @Override
  public Value remove(final Value key, final AshRuntime interpreter) {
    Value[] array = (Value[]) this.content;
    int index = (int) key.intValue();
    if (index < 0 || index >= array.length) {
      throw interpreter.runtimeException(
          "Array index [" + index + "] out of bounds (" + array.length + ")");
    }
    Value result = array[index];
    array[index] = this.getDataType().initialValue();
    return result;
  }

  @Override
  public void clear() {
    Value[] array = (Value[]) this.content;
    for (int i = 0; i < array.length; ++i) {
      array[i] = this.getDataType().initialValue();
    }
  }

  @Override
  public int count() {
    Value[] array = (Value[]) this.content;
    return array.length;
  }

  @Override
  public boolean contains(final Value key) {
    Value[] array = (Value[]) this.content;
    int index = (int) key.intValue();
    return index >= 0 && index < array.length;
  }

  @Override
  public Value[] keys() {
    int size = ((Value[]) this.content).length;
    Value[] result = new Value[size];
    for (int i = 0; i < size; ++i) {
      result[i] = new Value(i);
    }
    return result;
  }

  @Override
  public void dump(final PrintStream writer, final String prefix, final boolean compact) {
    if (!compact || this.type.dataValues() < 0) {
      super.dump(writer, prefix, compact);
      return;
    }

    writer.print(prefix);
    this.dumpValue(writer);
    writer.println();
  }

  @Override
  public void dumpValue(final PrintStream writer) {
    Value[] array = (Value[]) this.content;
    int count = array.length;

    if (count == 0) {
      return;
    }

    for (int i = 0; i < count; ++i) {
      if (i > 0) {
        writer.print("\t");
      }
      array[i].dumpValue(writer);
    }
  }

  @Override
  public int read(
      final String[] data,
      int index,
      final boolean compact,
      final String filename,
      final int line) {
    if (!compact || this.type.dataValues() < 0) {
      return super.read(data, index, compact, filename, line);
    }

    Value[] array = (Value[]) this.content;
    int count = array.length;

    // *** Zero-length array reading done in CompositeValue
    if (count == 0) {
      return 0;
    }

    int size = Math.min(count, data.length - index);
    int first = index;
    Type valType = this.getDataType();

    // Consume remaining data values and store them
    for (int offset = 0; offset < size; ++offset) {
      if (valType instanceof RecordType) {
        RecordValue rec = (RecordValue) array[offset];
        index += rec.read(data, index, true, filename, line);
      } else {
        array[offset] = Value.readValue(valType, data[index], filename, line);
        index += 1;
      }
    }

    // assert index == data.length
    return index - first;
  }

  @Override
  public Object toJSON() throws JSONException {
    JSONArray obj = new JSONArray();

    Value[] array = (Value[]) this.content;

    for (int i = 0; i < array.length; ++i) {
      obj.put(array[i].toJSON());
    }

    return obj;
  }
}
