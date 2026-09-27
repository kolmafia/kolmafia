package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class ScriptExit extends ScriptState {
  public ScriptExit(final Location location) {
    super(location, ScriptRuntime.State.EXIT);
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    super.execute(interpreter);
    interpreter.setExiting();
    return null;
  }

  @Override
  public boolean assertBarrier() {
    return true;
  }
}
