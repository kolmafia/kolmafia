package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;

public interface ParseTreeNode {
  public abstract Value execute(final AshRuntime interpreter);

  public abstract void print(final PrintStream stream, final int indent);

  public static interface TypedNode extends ParseTreeNode {
    public Type getType();
  }
}
