package net.sourceforge.kolmafia.textui.parsetree;

public class ArrowScope extends Scope {
  public ArrowScope(final VariableList variables, final BasicScope parentScope) {
    super(variables, parentScope);
  }

  @Override
  public boolean addVariable(final Variable v) {
    return this.variables.add(v);
  }

  @Override
  public Variable findVariable(final String name) {
    return this.findVariable(name, false);
  }

  @Override
  public Variable findVariable(final String name, final boolean recurse) {
    Variable current = this.variables.find(name);
    if (current != null) {
      return current;
    }
    if (recurse && this.parentScope != null) {
      return this.parentScope.findVariable(name, true);
    }
    return null;
  }
}
