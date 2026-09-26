package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.listener.ListenerRegistry;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class DebugRequestCommand extends AbstractCommand {
  public DebugRequestCommand() {
    this.usage =
        " [on] | off | ? | note | trace [ [on] | off | ? ] | ash [ [on] | off ] | listener [ [on] | off ] - start or stop logging of debugging data.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    String[] split = parameters.split(" ");
    String command = split[0];

    switch (command) {
      case "":
      case "on":
        RequestLogger.openDebugLog();
        break;
      case "off":
        RequestLogger.closeDebugLog();
        break;
      case "?":
        if (RequestLogger.isDebugging()) {
          KoLmafia.updateDisplay("Debugging is on.");
        } else {
          KoLmafia.updateDisplay("Debugging is off.");
        }
        break;
      case "trace":
        command = split.length < 2 ? "" : split[1];
        switch (command) {
          case "":
          case "on":
            RequestLogger.openTraceStream();
            break;
          case "off":
            RequestLogger.closeTraceStream();
            break;
          case "?":
            if (RequestLogger.isTracing()) {
              KoLmafia.updateDisplay("Tracing is on.");
            } else {
              KoLmafia.updateDisplay("Tracing is off.");
            }
            break;
        }
        break;
      case "ash":
        command = split.length < 2 ? "" : split[1];
        if (command.equals("") || command.equals("on")) {
          ScriptRuntime.openTraceStream();
        } else if (command.equals("off")) {
          ScriptRuntime.closeTraceStream();
        }
        break;
      case "listener":
        command = split.length < 2 ? "" : split[1];
        if (command.equals("") || command.equals("on")) {
          ListenerRegistry.setLogging(true);
        } else if (command.equals("off")) {
          ListenerRegistry.setLogging(false);
        }
        break;
      case "note":
        String debugNote = parameters.substring(command.length()).trim();
        if (debugNote.equals("")) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "debug note must include text to add to the debug log.");
          return;
        }
        java.util.Date noteTime = new java.util.Date();
        if (RequestLogger.isDebugging()) {
          RequestLogger.updateDebugLog(
              "-----User Note: " + noteTime + "-----\n" + debugNote + "\n-----");
        } else {
          RequestLogger.openDebugLog();
          RequestLogger.updateDebugLog(
              "-----User Note: " + noteTime + "-----\n" + debugNote + "\n-----");
          RequestLogger.closeDebugLog();
        }
        break;
      default:
        KoLmafia.updateDisplay("I don't know how to debug " + command);
        break;
    }
  }
}
