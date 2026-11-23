package net.sourceforge.kolmafia.swingui.button;

import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.swingui.JComponentUtilities;

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

    @Override
    public void run() {

      KoLmafiaCLI.DEFAULT_SHELL.executeLine(this.scriptPath);
    }
  }
}
