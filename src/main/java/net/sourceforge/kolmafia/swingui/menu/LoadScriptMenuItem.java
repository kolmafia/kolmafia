package net.sourceforge.kolmafia.swingui.menu;

import java.io.File;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.swingui.CommandDisplayFrame;
import net.sourceforge.kolmafia.swingui.listener.ThreadedListener;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.ScriptMRUList;

/**
 * In order to keep the user interface from freezing (or at least appearing to freeze), this
 * internal class is used to process the request for loading a script.
 */
public class LoadScriptMenuItem extends ThreadedMenuItem {

  private final String scriptPath;
  private boolean updateMRU = false;

  public LoadScriptMenuItem() {
    this("Load script...", null);
    this.updateMRU = true;
  }

  public LoadScriptMenuItem(final File file) {
    this(getRelativePath(file), getRelativePath(file));
  }

  public LoadScriptMenuItem(final String scriptName, final String scriptPath) {
    super(scriptName);
    this.scriptPath = scriptPath;
    this.addActionListener(new LoadScriptListener());
  }

  public String getScriptPath() {
    return this.scriptPath;
  }

  public static final String getRelativePath(File file) {
    return ScriptMRUList.getRelativePath(file);
  }

  private class LoadScriptListener extends ThreadedListener {
    private String executePath;

    @Override
    protected void execute() {
      this.executePath = LoadScriptMenuItem.this.scriptPath;

      if (this.executePath == null) {
        File input = InputFieldUtilities.chooseInputFile(KoLConstants.SCRIPT_LOCATION, null);
        if (input == null) {
          return;
        }
        this.executePath = getRelativePath(input);
      }

      if (this.executePath == null) {
        return;
      }

      KoLmafia.forceContinue();

      if (this.hasShiftModifier()) {
        CommandDisplayFrame.executeCommand("edit " + this.executePath);
      } else if (updateMRU) {
        CommandDisplayFrame.executeCommand("call " + this.executePath);
        KoLConstants.scriptMRUList.addItem(this.executePath);
      } else {
        CommandDisplayFrame.executeCommand(this.executePath);
      }
    }
  }
}
