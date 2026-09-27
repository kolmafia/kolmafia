package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import org.eclipse.lsp4j.Location;

public abstract class Expression extends Evaluable {
  Evaluable lhs;
  Evaluable rhs;

  public Expression(final Location location) {
    super(location);
  }

  public Evaluable getLeftHandSide() {
    return this.lhs;
  }

  public Evaluable getRightHandSide() {
    return this.rhs;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    this.lhs.print(stream, indent + 1);
    if (this.rhs != null) {
      this.rhs.print(stream, indent + 1);
    }
  }
}
