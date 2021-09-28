package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.AshRuntime;

public class TypeInitializer extends Value {
  public Type type;

  public TypeInitializer(final Type type) {
    this.type = type;
  }

  @Override
  public Type getType() {
    return this.type;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    return this.type.initialValue();
  }

  @Override
  public String toString() {
    return "<initial value>";
  }
}
