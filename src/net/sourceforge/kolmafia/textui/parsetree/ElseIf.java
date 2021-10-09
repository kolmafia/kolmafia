package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;
import org.eclipse.lsp4j.Location;

public class ElseIf extends Conditional {
  public ElseIf(final Location location, final Scope scope, final Value condition) {
    super(location, scope, condition);
  }

  @Override
  public String toString() {
    return "else if";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<ELSE IF>");
    this.getCondition().print(stream, indent + 1);
    this.getScope().print(stream, indent + 1);
  }
}
