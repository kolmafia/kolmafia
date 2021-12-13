package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public abstract class ScriptState extends Command {
  private final ScriptRuntime.State state;

  public ScriptState(final Location location, final ScriptRuntime.State state) {
    super(location);
    this.state = state;
  }

  @Override
  public String toString() {
    return this.state.toString();
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    if (ScriptRuntime.isTracing()) {
      interpreter.traceIndent();
      interpreter.trace(this.toString());
      interpreter.traceUnindent();
    }
    interpreter.setState(this.state);
    return DataTypes.VOID_VALUE;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<COMMAND " + this.state + ">");
  }

  @Override
  public boolean assertBarrier() {
    return true;
  }

  public static final class BadScriptState extends ScriptState {
    public BadScriptState(final Location location) {
      super(location, ScriptRuntime.State.NORMAL);
    }

    @Override
    public Value execute(final AshRuntime interpreter) {
      return null;
    }

    @Override
    public boolean assertBarrier() {
      return false;
    }
  }
}
