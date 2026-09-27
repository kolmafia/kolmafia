package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;

public class StaticScope extends Scope {
  public StaticScope(final BasicScope parentScope) {
    super(parentScope.variables, parentScope);
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    if (!this.executed) {
      return super.execute(interpreter);
    }

    return DataTypes.VOID_VALUE;
  }

  @Override
  public boolean addVariable(final Variable v) {
    v.markStatic();
    return super.addVariable(v);
  }
}
