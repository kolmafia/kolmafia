package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.parsetree.ParseTreeNode.TypedNode;
import net.sourceforge.kolmafia.textui.parsetree.Value.Constant;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;

public abstract class Evaluable extends Command implements TypedNode {
  public Evaluable(final Location location) {
    super(location);
  }

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

  public final void growLocation(final Location location) {
    if (location == null) {
      return;
    }

    if (this.getLocation() == null
        || this.startIsEqualOrSmaller(location) && this.endIsEqualOrGreater(location)) {
      this.setLocation(location);
    }
  }

  private boolean startIsEqualOrSmaller(final Location location) {
    Position ourStart = this.getLocation().getRange().getStart();
    Position theirStart = location.getRange().getStart();

    return theirStart.getLine() < ourStart.getLine()
        || theirStart.getLine() == ourStart.getLine()
            && theirStart.getCharacter() <= ourStart.getCharacter();
  }

  private boolean endIsEqualOrGreater(final Location location) {
    Position ourEnd = this.getLocation().getRange().getEnd();
    Position theirEnd = location.getRange().getEnd();

    return ourEnd.getLine() < theirEnd.getLine()
        || ourEnd.getLine() == theirEnd.getLine()
            && ourEnd.getCharacter() <= theirEnd.getCharacter();
  }
}
