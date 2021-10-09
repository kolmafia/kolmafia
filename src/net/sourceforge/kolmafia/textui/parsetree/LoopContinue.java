package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class LoopContinue extends ScriptState {
  public LoopContinue(final Location location) {
    super(location, ScriptRuntime.State.CONTINUE);
  }
}
