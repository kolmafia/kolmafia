package net.sourceforge.kolmafia;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import net.sourceforge.kolmafia.utilities.FileUtilities;

public abstract class KoLmafiaASH {
  private static final HashMap<String, File> relayScriptMap = new HashMap<String, File>();

  private static final HashMap<File, Long> TIMESTAMPS = new HashMap<File, Long>();
  private static final HashMap<File, ScriptRuntime> INTERPRETERS = new HashMap<>();

  public static final AshRuntime NAMESPACE_INTERPRETER = new NamespaceInterpreter();

  public static final void logScriptExecution(
      final String prefix, final String scriptName, ScriptRuntime script) {
    KoLmafiaASH.logScriptExecution(prefix, scriptName, "", script);
  }

  public static final void logScriptExecution(
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

  public static final boolean getClientHTML(final RelayRequest request) {
    String script = request.getBasePath();
    String field1 = null;
    String field2 = null;

    if (script.equals("place.php")) {
      field1 = request.getFormField("whichplace");
    } else if (script.equals("shop.php")) {
      field1 = request.getFormField("whichshop");
    } else if (script.equals("campground.php")) {
      field1 = request.getFormField("action");
      if (field1 != null && field1.equals("workshed")) {
        AdventureResult workshed_item = CampgroundRequest.getCurrentWorkshedItem();
        if (workshed_item != null) {
          field2 = field1 + "." + workshed_item.getItemId();
        }
      }
    }

    String scriptName = script.substring(0, script.length() - 4);

    return (field2 != null
            && KoLmafiaASH.getClientHTML(request, scriptName + "." + field2 + ".php"))
        || (field1 != null
            && KoLmafiaASH.getClientHTML(request, scriptName + "." + field1 + ".php"))
        || KoLmafiaASH.getClientHTML(request, script);
  }

  private static boolean getClientHTML(final RelayRequest request, String script) {
    if (KoLmafiaASH.relayScriptMap.containsKey(script)) {
      File toExecute = KoLmafiaASH.relayScriptMap.get(script);
      return toExecute.exists() && KoLmafiaASH.getClientHTML(request, toExecute);
    }

    for (String extension : new String[] {".ash", ".js"}) {
      String testScript = script;
      if (!testScript.endsWith(extension)) {
        if (!testScript.endsWith(".php")) {
          continue;
        }

        testScript = testScript.substring(0, testScript.length() - 4) + extension;
      }

      if (FileUtilities.internalRelayScriptExists(testScript)) {
        FileUtilities.loadLibrary(
            KoLConstants.RELAY_LOCATION, KoLConstants.RELAY_DIRECTORY, script);
      }

      File toExecute = new File(KoLConstants.RELAY_LOCATION, testScript);
      if (toExecute.exists()) {
        return KoLmafiaASH.getClientHTML(request, toExecute);
      }
    }

    return false;
  }

  private static boolean getClientHTML(final RelayRequest request, final File toExecute) {
    ScriptRuntime relayScript = KoLmafiaASH.getInterpreter(toExecute);
    if (relayScript == null) {
      return false;
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

      relayScript.execute("main", null, true);

      StringBuffer serverReplyBuffer = relayScript.getServerReplyBuffer();

      if (serverReplyBuffer.length() == 0) {
        if (relayRequest.responseText != null && relayRequest.responseText.length() != 0) {
          serverReplyBuffer.append(relayRequest.responseText);
        }
      }

      int written = serverReplyBuffer.length();
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

  public static final ScriptRuntime getInterpreter(final File toExecute) {
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
          long timestamp = entry.getValue().getModificationDate();
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

      if (interpreter instanceof AshRuntime
          && !((AshRuntime) interpreter).validate(toExecute, null)) {
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
      boolean matches = filter.equals("");

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
      for (VariableReference var : func.getVariableReferences()) {
        description.append(sep);
        sep = ", ";

        description.append(var.getRawType());

        if (var.getName() != null) {
          description.append(" ");
          description.append(var.getName());
        }
      }

      description.append(" )");

      RequestLogger.printLine(description.toString());
    }
  }

  public static final void stopAllRelayInterpreters() {
    for (ScriptRuntime i : KoLmafiaASH.INTERPRETERS.values()) {
      if (i.getRelayRequest() != null) {
        i.setState(ScriptRuntime.State.EXIT);
      }
    }
  }
}
