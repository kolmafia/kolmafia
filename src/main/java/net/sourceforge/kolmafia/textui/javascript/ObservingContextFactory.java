package net.sourceforge.kolmafia.textui.javascript;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

public class ObservingContextFactory extends ContextFactory {
  @Override
  protected Context makeContext() {
    Context cx = super.makeContext();
    // Break every 10,000 instructions to check if we have been interrupted.
    cx.setInstructionObserverThreshold(10000);
    return cx;
  }

  @Override
  protected void observeInstructionCount(Context ctx, int instructionCount) {
    JavascriptRuntime.checkInterrupted();
  }
}
