package net.sourceforge.kolmafia.swingui.menu;

import java.io.File;
import java.net.URI;
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
    URI baseURI = KoLConstants.ROOT_LOCATION.getAbsoluteFile().toURI();
    URI fileURI = file.getAbsoluteFile().toURI();

    URI relativeURI = baseURI.relativize(fileURI);

    String relativePath = relativeURI.getPath();

    while (relativePath.startsWith("./")) {
      relativePath = relativePath.substring(2);
    }

    if (relativePath.startsWith(KoLConstants.SCRIPT_DIRECTORY)) {
      return relativePath.substring(KoLConstants.SCRIPT_DIRECTORY.length());
    } else if (relativePath.startsWith(KoLConstants.PLOTS_DIRECTORY)) {
      return relativePath.substring(KoLConstants.PLOTS_DIRECTORY.length());
    } else if (relativePath.startsWith(KoLConstants.RELAY_DIRECTORY)) {
      return relativePath.substring(KoLConstants.RELAY_DIRECTORY.length());
    } else {
      return file.getAbsolutePath();
    }
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
