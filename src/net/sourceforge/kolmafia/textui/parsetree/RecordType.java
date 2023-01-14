package net.sourceforge.kolmafia.textui.parsetree;

import java.util.List;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import org.eclipse.lsp4j.Location;

public class RecordType extends CompositeType {
  private final String[] fieldNames;
  private final Type[] fieldTypes;
  private final Value[] fieldIndices;

  public RecordType(final String name, final String[] fieldNames, final Type[] fieldTypes) {
    this(name, fieldNames, fieldTypes, null);
  }

  public RecordType(
      final String name,
      final String[] fieldNames,
      final Type[] fieldTypes,
      final Location location) {
    super(name, TypeSpec.RECORD, location);

    this.fieldNames = fieldNames;
    this.fieldTypes = fieldTypes;

    // Build field index values.
    // These can be either integers or strings.
    //   Integers don't require a lookup
    //   Strings make debugging easier.

    this.fieldIndices = new Value[fieldNames.length];
    for (int i = 0; i < fieldNames.length; ++i) {
      this.fieldIndices[i] = new Value(fieldNames[i]);
    }
  }

  private RecordType(
      final String name,
      final String[] fieldNames,
      final Type[] fieldTypes,
      final Value[] fieldIndices,
      final Location location) {
    super(name, TypeSpec.RECORD, location);

    this.fieldNames = fieldNames;
    this.fieldTypes = fieldTypes;
    this.fieldIndices = fieldIndices;
  }

  public String[] getFieldNames() {
    return this.fieldNames;
  }

  public Type[] getFieldTypes() {
    return this.fieldTypes;
  }

  public Value[] getFieldIndices() {
    return this.fieldIndices;
  }

  public int fieldCount() {
    return this.fieldTypes.length;
  }

  @Override
  public Type getIndexType() {
    return DataTypes.STRING_TYPE;
  }

  @Override
  public Type getDataType() {
    return null;
  }

  @Override
  public Type getDataType(final Object key) {
    if (!(key instanceof Value) && !(key instanceof Value.Constant)) {
      return new BadType(null, null);
    }

    Value value = key instanceof Value.Constant ? ((Value.Constant) key).value : (Value) key;
    int index = this.indexOf(value);
    if (index < 0 || index >= this.fieldTypes.length) {
      return new BadType(null, null);
    }
    return this.fieldTypes[index];
  }

  public Value getFieldIndex(final String field) {
    String val = field.toLowerCase();
    for (int index = 0; index < this.fieldNames.length; ++index) {
      if (val.equals(this.fieldNames[index])) {
        return this.fieldIndices[index];
      }
    }
    return null;
  }

  @Override
  public Value getKey(final Value key) {
    Type type = key.getType();

    if (type.equals(TypeSpec.INT)) {
      int index = (int) key.intValue();
      if (index < 0 || index >= this.fieldNames.length) {
        return null;
      }
      return this.fieldIndices[index];
    }

    if (type.equals(TypeSpec.STRING)) {
      String str = key.toString();
      for (int index = 0; index < this.fieldNames.length; ++index) {
        if (this.fieldNames[index].equals(str)) {
          return this.fieldIndices[index];
        }
      }
      return null;
    }

    return null;
  }

  public int indexOf(final Value key) {
    Type type = key.getType();

    if (type.equals(TypeSpec.INT)) {
      int index = (int) key.intValue();
      if (index < 0 || index >= this.fieldNames.length) {
        return -1;
      }
      return index;
    }

    if (type.equals(TypeSpec.STRING)) {
      for (int index = 0; index < this.fieldNames.length; ++index) {
        if (key == this.fieldIndices[index]) {
          return index;
        }
      }
      return -1;
    }

    return -1;
  }

  @Override
  public boolean equals(final Type o) {
    return o instanceof RecordType && this.name.equals(o.name);
  }

  @Override
  public Type simpleType() {
    return this;
  }

  @Override
  public Value initialValue() {
    return new RecordValue(this);
  }

  public Value initialValueExpression(List<Evaluable> params) {
    if (params.isEmpty()) {
      return new TypeInitializer(this);
    }

    return new RecordInitializer(this, params);
  }

  @Override
  public int dataValues() {
    int values = 0;
    for (Type type : this.fieldTypes) {
      int value = type.dataValues();
      if (value == -1) {
        return -1;
      }
      values += value;
    }
    return values;
  }

  @Override
  public RecordType reference(final Location location) {
    return new RecordTypeReference(this, location);
  }

  private class RecordTypeReference extends RecordType {
    private RecordTypeReference(final RecordType recordType, final Location location) {
      super(
          recordType.name,
          recordType.fieldNames,
          recordType.fieldTypes,
          recordType.fieldIndices,
          location);
    }

    @Override
    public Location getDefinitionLocation() {
      return RecordType.this.getDefinitionLocation();
    }
  }

  public static class BadRecordType extends RecordType implements BadNode {
    public BadRecordType(final String name, final Location location) {
      super(name, new String[] {}, new Type[] {}, location);
    }

    // Don't override isBad(). The fields don't affect whether or not
    // the record itself is recognized.
  }
}
