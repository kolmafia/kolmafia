package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode.TypedNode;

public abstract class Evaluable extends Command implements TypedNode {
  public Type getRawType() {
    return this.getType();
  }

  public abstract String toString();

  public String toQuotedString() {
    return this.toString();
  }

  public boolean evaluatesTo(final Value value) {
    return this instanceof Value.LocatedValue && ((Value.LocatedValue) this).value == value;
  }
}
