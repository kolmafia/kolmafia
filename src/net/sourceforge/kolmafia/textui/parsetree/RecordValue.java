package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;

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

  public Value[] getRecordFields() {
    return (Value[]) this.content;
  }

  public Type getDataType(final Value key) {
    return ((RecordType) this.type).getDataType(key);
  }

  // The only comparison we implement is equality; we define no
  // "natural order" for a record value.
  //
  // Value implements equals, compareTo, and compreToIgnoreCase in terms
  // of compareTo(Value value, boolean ignoreCase)
  //
  // Therefore, that is the only method we need to implement here

  @Override
  protected int compareTo(final Value o, boolean ignoreCase) {
    // Per the implementation contract of Comparable.compareTo()
    if (o == null) {
      throw new NullPointerException();
    }

    // If the objects are identical Objects, save a lot of work
    if (this == o) {
      return 0;
    }

    // Per the implementation contract of Comparable.compareTo()
    // The Parser enforces this at compile time, but...
    if (!(o instanceof RecordValue)) {
      throw new ClassCastException();
    }

    RecordValue orv = (RecordValue) o;

    // The objects must both have the same record type.
    // The Parser does not (currently) enforce this.
    // Otherwise, it could be a ClassCastException
    RecordType type = this.getRecordType();
    if (!type.equals(orv.getRecordType())) {
      return -1;
    }

    // The fields must all be equal.
    Type[] dataTypes = type.getFieldTypes();
    Value[] fields = this.getRecordFields();
    Value[] ofields = orv.getRecordFields();

    // Compare each field
    for (int index = 0; index < dataTypes.length; ++index) {
      Type dataType = dataTypes[index].getBaseType();
      Value field = fields[index];
      Value ofield = ofields[index];

      // If the fields are identical objects, easy equals
      if (field == ofield) {
        continue;
      }

      // Unless we do a deep comparison, we cannot look inside aggregates
      // Require that they have the same size, at least
      if (dataType instanceof AggregateType) {
        if (field.count() != ofield.count()) {
          return -1;
        }
        continue;
      }

      // Any other field type - including another record - has a
      // functional equality operation.
      if (field.compareTo(ofield, ignoreCase) != 0) {
        return -1;
      }
    }

    return 0;
  }

  @Override
  public int hashCode() {
    int hash = this.getRecordType().hashCode();
    Value[] fields = this.getRecordFields();
    for (int index = 0; index < fields.length; ++index) {
      hash += 31 * fields[index].hashCode();
    }
    return hash;
  }

  @Override
  public Value aref(final Value key, final AshRuntime interpreter) {
    RecordType type = (RecordType) this.type;
    int index = type.indexOf(key);
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
    } else if (array[index].getType().equals(TypeSpec.STRING)) {
      array[index] = val.toStringValue();
    } else if (array[index].getType().equals(TypeSpec.INT)
        && val.getType().equals(TypeSpec.FLOAT)) {
      array[index] = val.toIntValue();
    } else if (array[index].getType().equals(TypeSpec.FLOAT)
        && val.getType().equals(TypeSpec.INT)) {
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
