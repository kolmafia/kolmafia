package net.sourceforge.kolmafia.swingui.menu;

import java.io.File;
import java.io.IOException;
import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

/**
 * In order to keep the user interface from freezing (or at least appearing to freeze), this
 * internal class is used to process the request for loading a script.
 */
public class LoadScriptMenuItem extends ThreadedMenuItem {
  public LoadScriptMenuItem() {
    this("Load script...", null);
  }

  public LoadScriptMenuItem(final String scriptName, final String scriptPath) {
    super(scriptName, new LoadScriptListener(scriptPath));
  }

  private static class LoadScriptListener extends ThreadedListener {
    private final String scriptPath;
    private String executePath;

    public LoadScriptListener(String scriptPath) {
      this.scriptPath = scriptPath;
    }

    @Override
    protected void execute() {
      this.executePath = this.scriptPath;

      if (this.scriptPath == null) {
        try {
          SwingUtilities.invokeAndWait(
              new Runnable() {
                public void run() {
                  File input =
                      InputFieldUtilities.chooseInputFile(KoLConstants.SCRIPT_LOCATION, null);
                  if (input == null) {
                    return;
                  }

                  try {
                    LoadScriptListener.this.executePath = input.getCanonicalPath();
                  } catch (IOException e) {
                  }
                }
              });
        } catch (Exception e) {
        }
      }

      if (this.executePath == null) {
        return;
      }

      KoLmafia.forceContinue();

      if (this.hasShiftModifier()) {
        CommandDisplayFrame.executeCommand("edit " + this.executePath);
      } else {
        CommandDisplayFrame.executeCommand("call " + this.executePath);
      }
    }
  }
}
