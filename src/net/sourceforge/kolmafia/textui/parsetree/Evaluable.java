package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode.TypedNode;
import net.sourceforge.kolmafia.textui.parsetree.Value.LocatedValue;

public abstract class Evaluable extends Command implements TypedNode {
  public Type getRawType() {
    return this.getType();
  }

  public abstract String toString();

  public String toQuotedString() {
    return this.toString();
  }

  /**
   * @returns if this is a {@link LocatedValue}, and if its {@link LocatedValue#value} is the *EXACT
   *     SAME* as {@code value}
   */
  public boolean evaluatesTo(final Value value) {
    return this instanceof LocatedValue && ((LocatedValue) this).value == value;
  }
}
