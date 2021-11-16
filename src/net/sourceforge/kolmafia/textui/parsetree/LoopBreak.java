package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class LoopBreak extends ScriptState {
  public LoopBreak(final Location location) {
    super(location, ScriptRuntime.State.BREAK);
  }

  @Override
  public boolean assertBreakable() {
    return true;
  }
}
