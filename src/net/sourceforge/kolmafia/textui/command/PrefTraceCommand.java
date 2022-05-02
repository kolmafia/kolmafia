package net.sourceforge.kolmafia.textui.command;

import java.util.ArrayList;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

public class PrefTraceCommand extends AbstractCommand {
  private static final ArrayList<PreferenceListener> audience =
      new ArrayList<>(); // keeps listeners from being GC'd

  public PrefTraceCommand() {
    this.usage = " <name> [, <name>]... - watch changes to indicated preferences";
  }

  @Override
  public synchronized void run(String command, final String parameters) {
    if (audience.size() != 0) {
      for (PreferenceListener listener : audience) {
        listener.unregister();
      }
      audience.clear();
      RequestLogger.printLine("Previously watched prefs have been cleared.");
    }

    if (parameters.equals("")) {
      return;
    }

    String[] prefList = parameters.split("\\s*,\\s*");
    for (String pref : prefList) {
      audience.add(new PreferenceListener(pref));
    }
  }

  private static class PreferenceListener implements Listener {
    String name;

    public PreferenceListener(String name) {
      this.name = name;
      PreferenceListenerRegistry.registerPreferenceListener(name, this);
      this.update();
    }

    public void unregister() {
      PreferenceListenerRegistry.unregisterPreferenceListener(name, this);
    }

    @Override
    public void update() {
      String msg = "ptrace: " + this.name + " = " + Preferences.getString(this.name);
      RequestLogger.updateSessionLog(msg);
      if (RequestLogger.isDebugging()) {
        StaticEntity.printStackTrace(msg);
        // msg also gets displayed in CLI
      } else {
        RequestLogger.printLine(msg);
      }
    }
  }
}
