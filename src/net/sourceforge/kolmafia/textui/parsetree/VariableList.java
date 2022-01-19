package net.sourceforge.kolmafia.textui.parsetree;

import java.util.ArrayList;
import java.util.Iterator;

public class VariableList implements Iterable<Variable> {
  private final ArrayList<Variable> list = new ArrayList<Variable>();

  public boolean add(final Variable n) {
    if (this.find(n.getName()) != null) {
      return false;
    }

    list.add(n);
    return true;
  }

  public Variable find(final String name) {
    for (Variable variable : this.list) {
      if (variable.getName().equalsIgnoreCase(name)) {
        return variable;
      }
    }

    return null;
  }

  @Override
  public Iterator<Variable> iterator() {
    return list.iterator();
  }
}
