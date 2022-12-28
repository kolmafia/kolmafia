package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.NamedListenerRegistry;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;

public class DebugLogMenuItem extends ThreadedMenuItem implements Listener {
  public DebugLogMenuItem() {
    super(RequestLogger.isDebugging() ? "Stop Debug Log" : "Start Debug Log");
    this.addActionListener(new DebugLogListener());
    NamedListenerRegistry.registerNamedListener("(debug)", this);
  }

  @Override
  public void update() {
    DebugLogMenuItem.this.setText(
        RequestLogger.isDebugging() ? "Stop Debug Log" : "Start Debug Log");
  }

  private static class DebugLogListener extends ThreadedListener {
    @Override
    protected void execute() {
      if (RequestLogger.isDebugging()) {
        RequestLogger.closeDebugLog();
      } else {
        RequestLogger.openDebugLog();
      }
    }
  }
}
