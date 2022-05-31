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
public class LoadScriptMenuItem extends DisplayFrameMenuItem {

  private final String scriptPath;

  public LoadScriptMenuItem() {
    this("Load script...", null);
  }

  public LoadScriptMenuItem(final String scriptName, final String scriptPath) {
    super(scriptName, "CommandDisplayFrame");

    this.scriptPath = scriptPath;

    this.addActionListener(new LoadScriptListener());
  }

  @Override
  public String toString() {
    return this.scriptPath == null ? this.getText() : this.scriptPath;
  }

  private class LoadScriptListener extends ThreadedListener {
    private String executePath;

    @Override
    protected void execute() {
      this.executePath = LoadScriptMenuItem.this.scriptPath;

      if (this.executePath == null) {
        try {
          SwingUtilities.invokeAndWait(
              new Runnable() {
                @Override
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
