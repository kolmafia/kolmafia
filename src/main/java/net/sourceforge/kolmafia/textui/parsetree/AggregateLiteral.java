package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.AshRuntime;

public abstract class AggregateLiteral extends AggregateValue {
  protected AggregateValue aggr = null;

  public AggregateLiteral(final AggregateType type) {
    super(type);
  }

  @Override
  public Value aref(final Value key, final AshRuntime interpreter) {
    return null;
  }

  @Override
  public void aset(final Value key, final Value val, final AshRuntime interpreter) {}

  @Override
  public Value remove(final Value key, final AshRuntime interpreter) {
    return null;
  }

  @Override
  public void clear() {}

  @Override
  public Value[] keys() {
    return new Value[0];
  }

  @Override
  public boolean contains(final Value index) {
    return false;
  }
}
