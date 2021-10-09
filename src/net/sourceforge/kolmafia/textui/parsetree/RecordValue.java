package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;

public class RecordValue extends CompositeValue {
  public RecordValue(final RecordType type) {
    super(type);

    Type[] DataTypes = type.getFieldTypes();
    int size = DataTypes.length;
    Value[] content = new Value[size];
    for (int i = 0; i < size; ++i) {
      content[i] = DataTypes[i].initialValue();
    }
    this.content = content;
  }

  public RecordType getRecordType() {
    return (RecordType) this.type;
  }

  public Type getDataType(final Value key) {
    return ((RecordType) this.type).getDataType(key);
  }

  @Override
  public Value aref(final Value key, final AshRuntime interpreter) {
    int index = ((RecordType) this.type).indexOf(key);
    if (index < 0) {
      throw interpreter.runtimeException("Internal error: field index out of bounds");
    }
    Value[] array = (Value[]) this.content;
    return array[index];
  }

  public Value aref(final int index, final AshRuntime interpreter) {
    RecordType type = (RecordType) this.type;
    int size = type.fieldCount();
    if (index < 0 || index >= size) {
      throw interpreter.runtimeException("Internal error: field index out of bounds");
    }
    Value[] array = (Value[]) this.content;
    return array[index];
  }

  @Override
  public void aset(final Value key, final Value val, final AshRuntime interpreter) {
    int index = ((RecordType) this.type).indexOf(key);
    if (index < 0) {
      throw interpreter.runtimeException("Internal error: field index out of bounds");
    }

    this.aset(index, val, interpreter);
  }

  public void aset(final int index, final Value val, final AshRuntime interpreter) {
    RecordType type = (RecordType) this.type;
    int size = type.fieldCount();
    if (index < 0 || index >= size) {
      throw interpreter.runtimeException("Internal error: field index out of bounds");
    }

    Value[] array = (Value[]) this.content;

    if (array[index].getType().equals(val.getType())) {
      array[index] = val;
    } else if (array[index].getType().equals(DataTypes.TYPE_STRING)) {
      array[index] = val.toStringValue();
    } else if (array[index].getType().equals(DataTypes.TYPE_INT)
        && val.getType().equals(DataTypes.TYPE_FLOAT)) {
      array[index] = val.toIntValue();
    } else if (array[index].getType().equals(DataTypes.TYPE_FLOAT)
        && val.getType().equals(DataTypes.TYPE_INT)) {
      array[index] = val.toFloatValue();
    } else {
      throw interpreter.runtimeException(
          "Internal error: Cannot assign " + val.getType() + " to " + array[index].getType());
    }
  }

  @Override
  public Value remove(final Value key, final AshRuntime interpreter) {
    int index = ((RecordType) this.type).indexOf(key);
    if (index < 0) {
      throw interpreter.runtimeException("Internal error: field index out of bounds");
    }
    Value[] array = (Value[]) this.content;
    Value result = array[index];
    array[index] = this.getDataType(key).initialValue();
    return result;
  }

  @Override
  public void clear() {
    Type[] DataTypes = ((RecordType) this.type).getFieldTypes();
    Value[] array = (Value[]) this.content;
    for (int index = 0; index < array.length; ++index) {
      array[index] = DataTypes[index].initialValue();
    }
  }

  @Override
  public Value[] keys() {
    return ((RecordType) this.type).getFieldIndices();
  }

  @Override
  public void dump(final PrintStream writer, final String prefix, boolean compact) {
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
    int size = ((RecordType) this.type).getFieldTypes().length;
    for (int i = 0; i < size; ++i) {
      Value value = this.aref(i, null);
      if (i > 0) {
        writer.print("\t");
      }
      value.dumpValue(writer);
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

    Type[] types = ((RecordType) this.type).getFieldTypes();
    Value[] array = (Value[]) this.content;

    int size = Math.min(types.length, data.length - index);
    int first = index;

    // Consume remaining data values and store them
    for (int offset = 0; offset < size; ++offset) {
      Type valType = types[offset];
      if (valType instanceof RecordType) {
        RecordValue rec = (RecordValue) array[offset];
        index += rec.read(data, index, true, filename, line);
      }
      // The only Aggregates that handle compact mode are
      // fixed-length arrays
      else if (valType instanceof AggregateType) {
        ArrayValue agg = (ArrayValue) array[offset];
        index += agg.read(data, index, true, filename, line);
      } else {
        array[offset] = Value.readValue(valType, data[index], filename, line);
        index += 1;
      }
    }

    // assert index == data.length
    return index - first;
  }

  @Override
  public String toString() {
    return "record " + this.type.toString();
  }
}
