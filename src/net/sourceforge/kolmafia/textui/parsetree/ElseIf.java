package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;

public class ElseIf extends Conditional {
  public ElseIf(final Scope scope, final Evaluable condition) {
    super(scope, condition);
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
