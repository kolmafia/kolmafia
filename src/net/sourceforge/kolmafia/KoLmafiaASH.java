package net.sourceforge.kolmafia;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.NamespaceInterpreter;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.utilities.ChoiceUtilities;
import net.sourceforge.kolmafia.utilities.FileUtilities;

public abstract class KoLmafiaASH {
  private static final HashMap<File, Long> TIMESTAMPS = new HashMap<>();
  private static final HashMap<File, ScriptRuntime> INTERPRETERS = new HashMap<>();

  public static final AshRuntime NAMESPACE_INTERPRETER = new NamespaceInterpreter();

  public static void logScriptExecution(
      final String prefix, final String scriptName, ScriptRuntime script) {
    KoLmafiaASH.logScriptExecution(prefix, scriptName, "", script);
  }

  public static void logScriptExecution(
      final String prefix, final String scriptName, final String postfix, ScriptRuntime script) {
    boolean isDebugging = RequestLogger.isDebugging();
    boolean isTracing = RequestLogger.isTracing();
    boolean scriptIsTracing = ScriptRuntime.isTracing();

    if (!isDebugging && !isTracing && !scriptIsTracing) {
      return;
    }

    String message = prefix + scriptName + postfix;

    if (isDebugging) {
      RequestLogger.updateDebugLog(message);
    }

    if (isTracing) {
      RequestLogger.trace(message);
    }

    if (scriptIsTracing) {
      script.trace(message);
    }
  }

  public static boolean getClientHTML(final RelayRequest request) {
    String script = request.getBasePath();
    String field1 = null;
    String field2 = null;

    switch (script) {
      case "place.php" -> field1 = request.getFormField("whichplace");
      case "shop.php" -> field1 = request.getFormField("whichshop");
      case "campground.php" -> {
        field1 = request.getFormField("action");
        if (field1 != null && field1.equals("workshed")) {
          var workshedItem = CampgroundRequest.getCurrentWorkshedItem();
          if (workshedItem != null) {
            field2 = field1 + "." + workshedItem.getItemId();
          }
        }
      }
      case "choice.php" -> field1 = String.valueOf(ChoiceUtilities.extractChoice(request));
    }

    String scriptName = script.substring(0, script.length() - 4);

    return (field2 != null
            && KoLmafiaASH.getClientHTML(request, scriptName + "." + field2 + ".php"))
        || (field1 != null
            && KoLmafiaASH.getClientHTML(request, scriptName + "." + field1 + ".php"))
        || KoLmafiaASH.getClientHTML(request, script);
  }

  public static File getRelayFile(final String script) {
    return new File(KoLConstants.RELAY_LOCATION, script);
  }

  private static boolean getClientHTML(final RelayRequest request, String script) {
    var name = script.substring(0, script.length() - 4);

    var file =
        Stream.of("ash", "js")
            .map(
                extension -> {
                  var testScript = name + "." + extension;

                  if (FileUtilities.internalRelayScriptExists(testScript)) {
                    FileUtilities.loadLibrary(
                        KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, script);
                  }

                  return getRelayFile(testScript);
                })
            .filter(File::exists)
            .findFirst()
            .orElse(null);

    return file != null && KoLmafiaASH.getClientHTML(request, file);
  }

  private static boolean getClientHTML(final RelayRequest request, final File toExecute) {
    ScriptRuntime relayScript = KoLmafiaASH.getInterpreter(toExecute);
    if (relayScript == null) {
      return false;
    }

    // Ezandora has graciously maintained a library called Choice-Overrides which allows script
    // maintainers to publish
    // relay overrides of specific choices without fighting over the ownership of choice.ash. Now
    // that we support
    // short-circuiting straight to choice.[choicenumber].ash, scripts no longer need to use this.

    // However, in order to reliably determine the choice number, Choice-Overrides has to run
    // visit_url() and parse the
    // page text. On a second call, visit_url() is blank, so these overrides need to be passed the
    // page text. This means
    // that choice.[choicenumber].ash files do not have the normal signature of a relay override
    // script (i.e. they rely
    // on a string parameter to main). Thus, in order to be backwards-compatible, we defer to
    // choice.ash when a specific
    // choice override is encountered that expects a string argument to be supplied.
    var isChoice = request.getBasePath().equals("choice.php");
    if (isChoice && relayScript.getNumberOfArgumentsToMain() > 0) {
      return getClientHTML(request, new File(KoLConstants.RELAY_DIRECTORY, "choice.ash"));
    }

    synchronized (relayScript) {
      // We are synchronized, so no other thread is in this
      // relay script, but this thread could be inside it: if
      // KoL redirects to the same page (but with different
      // arguments), the same script will want to handle the
      // redirection.

      if (relayScript.getRelayRequest() != null) {
        return false;
      }

      KoLmafiaASH.logScriptExecution(
          "Starting relay script: ", toExecute.getName(), "", relayScript);

      RelayRequest relayRequest = new RelayRequest(false);
      relayRequest.cloneURLString(request);

      relayScript.initializeRelayScript(relayRequest);
      StringBuffer serverReplyBuffer = relayScript.getServerReplyBuffer();

      relayScript.execute("main", null, true);

      if (serverReplyBuffer.isEmpty()
          && relayRequest.responseText != null
          && !relayRequest.responseText.isEmpty()) {
        serverReplyBuffer.append(relayRequest.responseText);
      }

      var written = serverReplyBuffer.length();
      if (written != 0) {
        String response = serverReplyBuffer.toString();
        request.pseudoResponse("HTTP/1.1 200 OK", response);
      }

      relayScript.finishRelayScript();
      KoLmafiaASH.logScriptExecution(
          "Finished relay script: ", toExecute.getName(), " (" + written + " bytes)", relayScript);

      return written != 0;
    }
  }

  // Convenience method so that callers can just do getInterpreter( KoLMafiaCLI.findScriptFile() )
  public static ScriptRuntime getInterpreter(List<File> findScriptFile) {
    if (findScriptFile.size() > 1) {
      RequestLogger.printList(findScriptFile);
      RequestLogger.printLine("Multiple matching scripts in your current namespace.");
      return null;
    }

    if (findScriptFile.size() == 1) {
      return getInterpreter(findScriptFile.get(0));
    }

    return null;
  }

  public static ScriptRuntime getInterpreter(final File toExecute) {
    if (toExecute == null) {
      return null;
    }

    boolean createInterpreter = !KoLmafiaASH.TIMESTAMPS.containsKey(toExecute);

    if (!createInterpreter) {
      Long timestamp = KoLmafiaASH.TIMESTAMPS.get(toExecute);
      createInterpreter = timestamp != toExecute.lastModified();
    }

    if (!createInterpreter) {
      ScriptRuntime interpreter = KoLmafiaASH.INTERPRETERS.get(toExecute);
      if (interpreter instanceof AshRuntime) {
        Map<File, Parser> imports = ((AshRuntime) interpreter).getImports();

        Iterator<Entry<File, Parser>> it = imports.entrySet().iterator();

        while (it.hasNext() && !createInterpreter) {
          Entry<File, Parser> entry = it.next();
          File file = entry.getKey();
          long timestamp = entry.getValue().getModificationTimestamp();
          createInterpreter = timestamp != file.lastModified();
        }
      }
    }

    if (createInterpreter) {
      KoLmafiaASH.TIMESTAMPS.remove(toExecute);
      ScriptRuntime interpreter;
      if (toExecute.getName().endsWith(".js")) {
        interpreter = new JavascriptRuntime(toExecute);
      } else {
        interpreter = new AshRuntime();
      }

      if (interpreter instanceof AshRuntime ashRuntime && !ashRuntime.validate(toExecute, null)) {
        return null;
      }

      KoLmafiaASH.TIMESTAMPS.put(toExecute, toExecute.lastModified());
      KoLmafiaASH.INTERPRETERS.put(toExecute, interpreter);
    }

    return KoLmafiaASH.INTERPRETERS.get(toExecute);
  }

  public static void showUserFunctions(final AshRuntime interpreter, final String filter) {
    KoLmafiaASH.showFunctions(interpreter.getFunctions(), filter.toLowerCase(), false);
  }

  public static void showExistingFunctions(final String filter) {
    KoLmafiaASH.showFunctions(RuntimeLibrary.getFunctions(), filter.toLowerCase(), true);
  }

  private static void showFunctions(
      final FunctionList functions, final String filter, boolean addLinks) {
    addLinks = addLinks && StaticEntity.isGUIRequired();

    if (functions.isEmpty()) {
      RequestLogger.printLine("No functions in your current namespace.");
      return;
    }

    for (Function func : functions) {
      boolean matches = filter.isEmpty();

      if (!matches) {
        matches = func.getName().toLowerCase().contains(filter);
      }

      if (!matches) {
        for (VariableReference ref : func.getVariableReferences()) {
          String refType = ref.getType().toString();
          matches = refType != null && refType.contains(filter);
        }
      }

      if (!matches) {
        continue;
      }

      StringBuilder description = new StringBuilder();

      description.append(func.getType());
      description.append(" ");
      if (addLinks) {
        description.append("<a href='https://wiki.kolmafia.us/index.php?title=");
        description.append(func.getName());
        description.append("'>");
      }
      description.append(func.getName());
      if (addLinks) {
        description.append("</a>");
      }
      description.append("( ");

      String sep = "";
      for (VariableReference variableReference : func.getVariableReferences()) {
        description.append(sep);
        sep = ", ";

        description.append(variableReference.getRawType());

        if (variableReference.getName() != null) {
          description.append(" ");
          description.append(variableReference.getName());
        }
      }

      description.append(" )");

      RequestLogger.printHtml(description.toString());
    }
  }

  public static void stopAllRelayInterpreters() {
    for (ScriptRuntime i : KoLmafiaASH.INTERPRETERS.values()) {
      if (i.getRelayRequest() != null) {
        i.setState(ScriptRuntime.State.EXIT);
      }
    }
  }
}
