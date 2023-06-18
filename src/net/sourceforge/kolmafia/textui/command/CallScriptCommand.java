package net.sourceforge.kolmafia.textui.command;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.Profiler;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class CallScriptCommand extends AbstractCommand {
  private static final Pattern ASHNAME_PATTERN =
      Pattern.compile("\\.ash", Pattern.CASE_INSENSITIVE);
  private static final Pattern JSNAME_PATTERN = Pattern.compile("\\.js", Pattern.CASE_INSENSITIVE);

  public CallScriptCommand() {
    this.usage = " [<number>x] <filename> | <function> [<parameters>] - check/run script.";
  }

  @Override
  public void run(final String command, final String parameters) {
    CallScriptCommand.call(command, parameters, this.callerController);
  }

  public static void call(final String command, String parameters, ScriptRuntime caller) {
    try {
      int runCount = 1;
      String[] arguments = null;

      parameters = parameters.trim();

      // See if 'parameters' match a script file name in any of the allowed directories.
      List<File> scriptMatches = KoLmafiaCLI.findScriptFile(parameters);

      // If no script was found, see if it starts with #x, which specifies a run count.
      // *** I hate the following code.
      if (scriptMatches.size() == 0) {
        String runCountString = parameters.split(" ")[0];
        boolean hasMultipleRuns = runCountString.endsWith("x");

        for (int i = 0; i < runCountString.length() - 1 && hasMultipleRuns; ++i) {
          hasMultipleRuns = Character.isDigit(runCountString.charAt(i));
        }

        if (hasMultipleRuns) {
          runCount = StringUtilities.parseInt(runCountString);
          // Fixes StringIndexOutOfBoundsException error when "x" is entered
          // as a command.  This may be addressing the symptom and not the
          // cause and but should not break x as a "repeat indicator".
          if (runCount <= 0) {
            return;
          }
          parameters = parameters.substring(parameters.indexOf(" ")).trim();
          scriptMatches = KoLmafiaCLI.findScriptFile(parameters);
        }
      }

      // If still no script was found, see if the script has arguments.
      // *** File names can have spaces. The following code assumes that
      // *** the first space separates the filename from the arguments.
      if (scriptMatches.size() == 0) {
        int spaceIndex = parameters.indexOf(" ");
        if (spaceIndex != -1) {
          String argumentString = parameters.substring(spaceIndex + 1).trim();
          if (argumentString.startsWith("(") && argumentString.endsWith(")")) {
            // Split (arg, arg, ...) into a list of arguments
            argumentString = argumentString.substring(1, argumentString.length() - 1);
            arguments = argumentString.split(",");
            for (int i = 0; i < arguments.length; i++) {
              arguments[i] = arguments[i].trim();
            }
          } else {
            // Without parentheses, the parameters are a single string
            arguments = new String[] {argumentString};
          }

          parameters = parameters.substring(0, spaceIndex);
          scriptMatches = KoLmafiaCLI.findScriptFile(parameters);
        }
      }

      // If not even that, perhaps it's the invocation of a
      // function which is defined in the ASH namespace?

      if (scriptMatches.size() == 0) {
        Value rv = KoLmafiaASH.NAMESPACE_INTERPRETER.execute(parameters, arguments);
        // A script only has a meaningful return value
        // if it succeeded.
        if (KoLmafia.permitsContinue()) {
          KoLmafia.updateDisplay("Returned: " + rv);
        }
        return;
      }

      // If we have multiple matches, punt here.

      if (scriptMatches.size() > 1) {
        // too many matches, punt
        RequestLogger.printList(scriptMatches);
        RequestLogger.printLine();
        KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "] has too many matches.");
        return;
      }

      // Else we have a single, unique match and can proceed normally.

      File scriptFile = scriptMatches.get(0);

      // In theory, you could execute EVERY script in a
      // directory, but instead, let's make it an error.

      if (scriptFile.isDirectory()) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR, scriptFile.getCanonicalPath() + " is a directory.");
        return;
      }

      // Got here so have valid script file name and not a directory.

      KoLConstants.scriptMRUList.addItemInParallel(scriptFile);

      // Allow the ".ash" or ".js" to appear anywhere in the filename
      // in a case-insensitive manner.

      if (CallScriptCommand.ASHNAME_PATTERN.matcher(scriptFile.getPath()).find()
          || CallScriptCommand.JSNAME_PATTERN.matcher(scriptFile.getPath()).find()) {
        ScriptRuntime interpreter = KoLmafiaASH.getInterpreter(scriptFile);

        if (!command.equals("call") && interpreter instanceof JavascriptRuntime) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "Cannot use command " + command + " with JavaScript scripts.");
          return;
        }

        // If there's an alternate namespace being
        // used, then be sure to switch.

        if (command.equals("validate") || command.equals("verify") || command.equals("check")) {
          if (interpreter instanceof AshRuntime) {
            RequestLogger.printLine();
            KoLmafiaASH.showUserFunctions((AshRuntime) interpreter, "");

            RequestLogger.printLine();
            RequestLogger.printLine("Script verification complete.");
          }

          return;
        }

        if (command.equals("profile")) {
          if (interpreter instanceof AshRuntime ashInterpreter) {
            Profiler prof = Profiler.create("toplevel");
            long t0 = System.nanoTime();
            prof.net0 = t0;
            ashInterpreter.profiler = prof;

            for (int i = 0; i < runCount && KoLmafia.permitsContinue(); ++i) {
              KoLmafiaASH.logScriptExecution(
                  "Starting ASH script: ", scriptFile.getName(), interpreter);
              ashInterpreter.execute("main", arguments);
              KoLmafiaASH.logScriptExecution(
                  "Finished ASH script: ", scriptFile.getName(), interpreter);
            }

            long t1 = System.nanoTime();
            prof.total = t1 - t0;
            prof.net += t1 - prof.net0;
            prof.finish();
            ashInterpreter.profiler = null;
            RequestLogger.printLine(Profiler.summary());
          }
          return;
        }

        // If there's an alternate namespace being
        // used, then be sure to switch.

        if (interpreter != null) {
          try {
            interpreter.cloneRelayScript(caller);
            interpreter.resetTracing();

            for (int i = 0; i < runCount && KoLmafia.permitsContinue(); ++i) {
              KoLmafiaASH.logScriptExecution(
                  "Starting script: ", scriptFile.getName(), interpreter);
              interpreter.execute("main", arguments);
              KoLmafiaASH.logScriptExecution(
                  "Finished script: ", scriptFile.getName(), interpreter);
            }
          } finally {
            interpreter.finishRelayScript();
          }
        }
      } else {
        if (arguments != null) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR, "You can only specify arguments for an ASH or JS script");
          return;
        }

        for (int i = 0; i < runCount && KoLmafia.permitsContinue(); ++i) {
          new KoLmafiaCLI(DataUtilities.getInputStream(scriptFile)).listenForCommands();
        }
      }
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
      return;
    }
  }
}
