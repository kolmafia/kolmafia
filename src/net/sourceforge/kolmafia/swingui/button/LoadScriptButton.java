package net.sourceforge.kolmafia.swingui.button;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.KoLmafiaCLI;

public class LoadScriptButton extends ThreadedButton {

  public LoadScriptButton(final int scriptId, final String scriptPath) {
    super(String.valueOf(scriptId), new LoadScriptRunnable(scriptPath));

    this.setToolTipText(scriptPath);

    JComponentUtilities.setComponentSize(this, 30, 30);
  }

  private static class LoadScriptRunnable implements Runnable {
    private final String scriptPath;

    public LoadScriptRunnable(String scriptPath) {
      this.scriptPath = scriptPath;
    }

    public void run() {

      KoLmafiaCLI.DEFAULT_SHELL.executeLine(this.scriptPath);
    }
  }
}
