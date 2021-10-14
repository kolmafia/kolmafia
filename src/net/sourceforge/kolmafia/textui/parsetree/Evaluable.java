package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode.TypedNode;
import net.sourceforge.kolmafia.textui.parsetree.Value.Constant;

public abstract class Evaluable extends Command implements TypedNode {
  public Type getRawType() {
    return this.getType();
  }

  public abstract String toString();

  public String toQuotedString() {
    return this.toString();
  }

  /**
   * @returns if this is a {@link Constant}, and if its {@link Constant#value} is the *EXACT SAME*
   *     as {@code value}
   */
  public boolean evaluatesTo(final Value value) {
    return this instanceof Constant && ((Constant) this).value == value;
  }
}
