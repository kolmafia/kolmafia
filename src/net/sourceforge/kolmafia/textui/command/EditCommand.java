package net.sourceforge.kolmafia.textui.command;

import java.io.File;
import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;

public class EditCommand extends AbstractCommand {
  public EditCommand() {
    this.usage = " <filename> - launch external editor for a script or map file.";
  }

  @Override
  public void run(final String command, final String parameters) {
    if (parameters.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No filename specified.");
      return;
    }

    List<File> scriptMatches = KoLmafiaCLI.findScriptFile(parameters);

    if (scriptMatches.size() > 1) {
      // too many matches, error
      RequestLogger.printList(scriptMatches);
      RequestLogger.printLine();
      KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "] has too many matches.");
      return;
    }

    File scriptFile = scriptMatches.size() == 1 ? scriptMatches.get(0) : null;

    if (scriptFile == null) {
      scriptFile = new File(KoLConstants.DATA_LOCATION, parameters);
      if (!scriptFile.exists()) {
        if (parameters.indexOf("/") != -1
            || parameters.indexOf("\\")
                != -1) { // Let user explicitly give the top-level directory,
          // as in "edit data/mymap.txt".
          scriptFile = new File(KoLConstants.ROOT_LOCATION, parameters);
        } else { // Assume scripts folder for bare filename
          scriptFile = new File(KoLConstants.SCRIPT_LOCATION, parameters);
        }
      }
    }

    if (scriptFile.isDirectory()) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Can't edit a directory!");
      return;
    }

    String editor = Preferences.getString("externalEditor");
    String path = scriptFile.getAbsolutePath();

    if (editor.equals("")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "No external editor specified in Preferences.");
      RequestLogger.printLine("Full path to this file is " + path);
      return;
    }

    RequestLogger.printLine("Launching editor for " + path);

    try {
      Runtime.getRuntime().exec(new String[] {editor, path});
    } catch (Exception e) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Editor launch failed: " + e);
    }
  }
}
