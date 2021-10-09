package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;

public class MirrorLogCommand extends AbstractCommand {
  public MirrorLogCommand() {
    this.usage = " [<filename>] - stop [or start] logging to an additional file.";
  }

  @Override
  public void run(final String command, String parameters) {
    if (command.indexOf("end") != -1
        || command.indexOf("stop") != -1
        || command.indexOf("close") != -1
        || parameters.length() == 0
        || parameters.equals("end")
        || parameters.equals("stop")
        || parameters.equals("close")) {
      RequestLogger.closeMirror();
      KoLmafia.updateDisplay("Mirror stream closed.");
    } else {
      if (!parameters.endsWith(".txt")) {
        parameters += ".txt";
      }

      RequestLogger.openMirror(parameters);
    }
  }
}
