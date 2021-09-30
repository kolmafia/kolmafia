package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.RequestLogger;

public class PlayerSnapshotCommand extends AbstractCommand {
  public PlayerSnapshotCommand() {
    this.usage =
        " [status],[equipment],[effects],[<etc>.] - record data, \"log snapshot\" for all common data.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.equals("snapshot")) {
      this.snapshot("moon, status, equipment, skills, effects, modifiers");
      return;
    }

    this.snapshot(parameters);
  }

  private void snapshot(final String parameters) {
    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

    RequestLogger.getDebugStream().println();
    RequestLogger.getDebugStream().println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

    StringBuffer title = new StringBuffer("Player Snapshot");

    int leftIndent = (46 - title.length()) / 2;
    for (int i = 0; i < leftIndent; ++i) {
      title.insert(0, ' ');
    }

    RequestLogger.updateSessionLog(title.toString());
    RequestLogger.updateSessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

    RequestLogger.getDebugStream().println(title.toString());
    RequestLogger.getDebugStream().println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

    String[] options = parameters.split("\\s*,\\s*");

    for (int i = 0; i < options.length; ++i) {
      RequestLogger.updateSessionLog();
      RequestLogger.updateSessionLog(" > " + options[i]);

      ShowDataCommand.show(options[i], true);
    }

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

    RequestLogger.getDebugStream().println();
    RequestLogger.getDebugStream().println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog();

    RequestLogger.getDebugStream().println();
    RequestLogger.getDebugStream().println();
  }
}
