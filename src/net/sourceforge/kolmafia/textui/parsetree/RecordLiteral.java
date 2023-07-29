package net.sourceforge.kolmafia.textui.parsetree;

import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.textui.AshRuntime;

public class RecordLiteral extends RecordValue {
  private RecordValue rec = null;
  private final List<String> keys;
  private final List<Evaluable> values;

  public RecordLiteral(
      final RecordType type, final List<String> keys, final List<Evaluable> values) {
    super(type);
    this.keys = keys;
    this.values = values;
  }

  @Override
  public Value aref(final Value key, final AshRuntime interpreter) {
    return null;
  }

  @Override
  public Value aref(final int index, final AshRuntime interpreter) {
    return null;
  }

  @Override
  public void aset(final Value key, final Value val, final AshRuntime interpreter) {
    throw interpreter.runtimeException("Cannot assign to a record literal field");
  }

  @Override
  public void aset(final int index, final Value val, final AshRuntime interpreter) {
    throw interpreter.runtimeException("Cannot assign to a record literal field");
  }

  @Override
  public Value remove(final Value key, final AshRuntime interpreter) {
    throw interpreter.runtimeException("Cannot assign to a record literal field");
  }

  @Override
  public void clear() {}

  @Override
  public Value execute(final AshRuntime interpreter) {
    this.rec = (RecordValue) this.type.initialValue();

    Iterator<String> keyIterator = this.keys.iterator();
    Iterator<Evaluable> valIterator = this.values.iterator();

    while (keyIterator.hasNext() && valIterator.hasNext()) {
      Value key = this.getRecordType().getFieldIndex(keyIterator.next());
      Value val = valIterator.next().execute(interpreter);
      this.rec.aset(key, val);
    }

    return this.rec;
  }
}
