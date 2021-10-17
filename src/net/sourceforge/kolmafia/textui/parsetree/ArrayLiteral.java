package net.sourceforge.kolmafia.textui.parsetree;

import java.util.List;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;

public class ArrayLiteral extends AggregateLiteral {
  private final List<Evaluable> values;

  public ArrayLiteral(AggregateType type, final List<Evaluable> values) {
    super(new AggregateType(type));
    this.values = values;

    type = (AggregateType) this.getType();
    int size = type.getSize();

    // If size == -1, we are creating a map.
    // Unexpected, but it will work
    if (size < 0) {
      return;
    }

    // If size == 0, we are creating an array whose size is specified
    // by the number of values. Change the size in the type.
    if (size == 0) {
      type.setSize(values.size());
      return;
    }

    // If size > 0, we are creating an array whose size is known at
    // compile time. If the count of values is <= that size, all is
    // well.  But if not, still no problem, since we will only store
    // the correct number of values.
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    AggregateType type = (AggregateType) this.type;

    this.aggr = (AggregateValue) this.type.initialValue();

    int index = 0;
    int size = type.getSize();
    for (Evaluable val : this.values) {
      if (size >= 0 && index >= size) {
        break;
      }

      Value key = DataTypes.makeIntValue(index++);
      this.aggr.aset(key, val.execute(interpreter));
    }

    return this.aggr;
  }

  @Override
  public int count() {
    if (this.aggr != null) {
      return this.aggr.count();
    }
    return this.values.size();
  }
}
