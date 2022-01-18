package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode.TypedNode;
import net.sourceforge.kolmafia.textui.parsetree.Value.Constant;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.util.Ranges;

public abstract class Evaluable extends Command implements TypedNode {
  public Evaluable(final Location location) {
    super(location);
  }

  @Override
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

  public final void growLocation(final Location location) {
    if (location == null) {
      return;
    }

    if (this.getLocation() == null
        || this.getLocation().getUri().equals(location.getUri())
            && Ranges.containsRange(location.getRange(), this.getLocation().getRange())) {
      this.setLocation(location);
    }
  }
}
