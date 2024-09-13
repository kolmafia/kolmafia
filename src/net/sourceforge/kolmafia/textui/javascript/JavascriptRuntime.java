package net.sourceforge.kolmafia.textui.javascript;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.AbstractRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue.MonsterProxy;
import net.sourceforge.kolmafia.textui.parsetree.Symbol;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.textui.parsetree.VariableReference;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.NativePromise;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.commonjs.module.Require;

public class JavascriptRuntime extends AbstractRuntime {
  public static final String DEFAULT_RUNTIME_LIBRARY_NAME = "__runtimeLibrary__";
  public static final Pattern CAMEL_CASER = Pattern.compile("(?<=[A-Za-z0-9])_([A-Za-z0-9])");

  static final Set<JavascriptRuntime> runningRuntimes = ConcurrentHashMap.newKeySet();
  static final ContextFactory contextFactory = new ObservingContextFactory();
  static final Map<String, Storage> storedSessions = new HashMap<>();
  private File scriptFile = null;
  private String scriptString = null;

  private Scriptable currentTopScope = null;
  private Scriptable currentStdLib = null;

  public static void clearSessionStorage() {
    storedSessions.clear();
  }

  public static String toCamelCase(String name) {
    if (name == null) {
      return null;
    }

    StringBuilder result = new StringBuilder();

    Matcher m = CAMEL_CASER.matcher(name);

    int previous_index = 0;

    while (m.find()) {
      result.append(name, previous_index, m.start()).append(m.group(1).toUpperCase());
      previous_index = m.end();
    }

    if (previous_index < name.length()) {
      result.append(name, previous_index, name.length());
    }

    return result.toString();
  }

  public static String capitalize(String name) {
    if (name == null) {
      return null;
    }

    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  public JavascriptRuntime(File scriptFile) {
    this.scriptFile = scriptFile;
  }

  public JavascriptRuntime(String scriptString) {
    this.scriptString = scriptString;
  }

  public static List<net.sourceforge.kolmafia.textui.parsetree.Function> getFunctions() {
    List<net.sourceforge.kolmafia.textui.parsetree.Function> functions = new ArrayList<>();
    for (net.sourceforge.kolmafia.textui.parsetree.Function libraryFunction :
        RuntimeLibrary.functions) {
      // Blacklist a number of types.
      List<Type> allTypes = new ArrayList<>();
      allTypes.add(libraryFunction.getType());
      for (VariableReference variableReference : libraryFunction.getVariableReferences()) {
        allTypes.add(variableReference.getType());
      }
      if (allTypes.contains(DataTypes.MATCHER_TYPE)) {
        continue;
      }

      functions.add(libraryFunction);
    }
    return functions;
  }

  private Scriptable initRuntimeLibrary(Context cx, Scriptable scope, File scriptFile) {
    var addToTopScope = scriptFile == null;

    Set<String> uniqueFunctionNames =
        getFunctions().stream().map(Symbol::getName).collect(Collectors.toCollection(TreeSet::new));

    Scriptable stdLib = cx.newObject(scope);

    for (String libraryFunctionName : uniqueFunctionNames) {
      String jsName = toCamelCase(libraryFunctionName);
      ScriptableObject.defineProperty(
          stdLib,
          jsName,
          new LibraryFunctionStub(
              stdLib, ScriptableObject.getFunctionPrototype(stdLib), this, libraryFunctionName),
          READONLY | PERMANENT);
      if (addToTopScope) {
        ScriptableObject.defineProperty(
            scope,
            jsName,
            new LibraryFunctionStub(
                scope, ScriptableObject.getFunctionPrototype(scope), this, libraryFunctionName),
            DONTENUM);
      }
    }

    // Initialise sessionStorage
    // Storage is sandboxed per script file. CLI scripts share a session.
    var storage =
        storedSessions.computeIfAbsent(
            scriptFile == null ? null : scriptFile.getAbsolutePath(), k -> new Storage());

    var wrapFactory = cx.getWrapFactory();
    wrapFactory.setJavaPrimitiveWrap(false);
    var jsObject = (NativeJavaObject) wrapFactory.wrap(cx, scope, storage, null);
    ScriptableObject.defineProperty(
        stdLib, "sessionStorage", jsObject, DONTENUM | READONLY | PERMANENT);

    if (addToTopScope) {
      ScriptableObject.defineProperty(scope, "sessionStorage", jsObject, DONTENUM);
    }

    ScriptableObject.defineProperty(
        scope, DEFAULT_RUNTIME_LIBRARY_NAME, stdLib, DONTENUM | READONLY | PERMANENT);
    return stdLib;
  }

  private static ScriptableObject initEnumeratedType(
      Context cx,
      Scriptable scope,
      Scriptable runtimeLibrary,
      Class<?> recordValueClass,
      Type valueType) {
    EnumeratedWrapperPrototype prototype =
        new EnumeratedWrapperPrototype(recordValueClass, valueType);
    return prototype.initToScope(cx, scope, runtimeLibrary);
  }

  private static void initEnumeratedTypes(Context cx, Scriptable scope, Scriptable runtimeLibrary) {
    var enumeratedProtos = new ArrayList<ScriptableObject>();
    for (Type valueType : DataTypes.enumeratedTypes) {
      String typeName = capitalize(valueType.getName());
      Class<?> proxyRecordValueClass = Value.class;
      for (Class<?> testProxyRecordValueClass : ProxyRecordValue.class.getDeclaredClasses()) {
        if (testProxyRecordValueClass.getSimpleName().equals(typeName + "Proxy")) {
          proxyRecordValueClass = testProxyRecordValueClass;
        }
      }

      var proto = initEnumeratedType(cx, scope, runtimeLibrary, proxyRecordValueClass, valueType);
      if (proto != null) enumeratedProtos.add(proto);
    }

    if (runtimeLibrary != null) {
      var jsArray = (NativeArray) cx.newArray(scope, enumeratedProtos.toArray());
      ScriptableObject.defineProperty(
          runtimeLibrary, "MafiaClasses", jsArray, DONTENUM | READONLY | PERMANENT);
    }
  }

  @Override
  public Value execute(
      final String functionName, final Object[] arguments, final boolean executeTopLevel) {
    if (!executeTopLevel) {
      if (currentTopScope == null) {
        throw new ScriptException(
            "Cannot run with executeTopLevel = false without running once first.");
      }
      return executeRun(functionName, arguments, false);
    }

    // TODO: Support for requesting user arguments if missing.
    Context cx = contextFactory.enterContext();

    cx.setLanguageVersion(Context.VERSION_ES6);
    cx.setOptimizationLevel(1);
    cx.setTrackUnhandledPromiseRejections(true);
    runningRuntimes.add(this);

    // TODO: Use a shared parent scope and initialize this with that as a prototype.
    // But be careful. May mess up our EnumeratedWrapper registries.
    Scriptable scope = cx.initSafeStandardObjects();
    currentTopScope = scope;

    try {
      // If executing from GCLI (and not file), add std lib to top scope.
      currentStdLib = initRuntimeLibrary(cx, scope, scriptFile);
      initEnumeratedTypes(cx, scope, currentStdLib);

      setState(State.NORMAL);
      if (ScriptRuntime.hasTopCall(cx)) {
        return executeRun(functionName, arguments, true);
      } else {
        return (Value)
            ScriptRuntime.doTopCall(
                (cx1, scope1, thisObj, args) -> executeRun(functionName, arguments, true),
                cx,
                scope,
                null,
                new Object[] {},
                false);
      }
    } catch (EvaluatorException e) {
      String escapedMessage =
          escapeHtmlInMessage(
              "JavaScript evaluator exception: " + e.getMessage() + "\n" + e.getScriptStackTrace());
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, escapedMessage);
      return null;
    } finally {
      EnumeratedWrapper.cleanup(scope);
      currentTopScope = null;
      runningRuntimes.remove(this);
      Context.exit();
    }
  }

  public Value executeFunction(final Scriptable scope, final Supplier<Object> callback) {
    Context cx = Context.getCurrentContext();
    Object returnValue = null;

    boolean stackOnAbort = Preferences.getBoolean("printStackOnAbort");

    try {
      returnValue = callback.get();
      cx.processMicrotasks();
      if (returnValue instanceof NativePromise promise) {
        returnValue = null;
        returnValue = resolvePromise(cx, promise);
      }
    } catch (WrappedException e) {
      Throwable unwrapped = e.getWrappedException();
      if (unwrapped instanceof ScriptException) {
        String escapedMessage = escapeHtmlInMessage("Script exception: " + unwrapped.getMessage());
        KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, escapedMessage);
      } else {
        if (stackOnAbort) {
          StringBuilder message = new StringBuilder("Internal exception");
          if (unwrapped.getMessage() != null) {
            message.append(": ").append(e.getMessage());
          }
          message.append("\n").append(e.getScriptStackTrace());
          String escapedMessage = escapeHtmlInMessage(message.toString());
          KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, escapedMessage);
        }
        StaticEntity.printStackTrace(unwrapped);
      }
    } catch (EvaluatorException e) {
      String escapedMessage =
          escapeHtmlInMessage(
              "JavaScript evaluator exception: " + e.getMessage() + "\n" + e.getScriptStackTrace());
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, escapedMessage);
    } catch (EcmaError e) {
      String escapedMessage =
          escapeHtmlInMessage(
              "JavaScript error: " + e.getErrorMessage() + "\n" + e.getScriptStackTrace());
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, escapedMessage);
    } catch (JavaScriptException e) {
      String escapedMessage =
          escapeHtmlInMessage(
              "JavaScript exception: " + e.getMessage() + "\n" + e.getScriptStackTrace());
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, escapedMessage);
    } catch (ScriptException e) {
      String escapedMessage = escapeHtmlInMessage("Script exception: " + e.getMessage());
      KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, escapedMessage);
    } finally {
      setState(State.EXIT);
    }

    cx.getUnhandledPromiseTracker()
        .process(
            o -> {
              String escapedMessage =
                  escapeHtmlInMessage("Unhandled rejected Promise: " + o.toString());
              KoLmafia.updateDisplay(KoLConstants.MafiaState.ERROR, escapedMessage);
            });

    return new ValueConverter(cx, scope).fromJava(returnValue);
  }

  private static Object resolvePromise(Context cx, NativePromise promise) {
    // there is no good way to access promise.getResult, so let the engine store it in a variable
    Scriptable promiseScope = cx.initSafeStandardObjects();
    promiseScope.put("promise", promiseScope, promise);
    cx.evaluateString(
        promiseScope,
        "promise.then((ret) => promiseReturnValue = ret, (err) => promiseRejectedValue = err)",
        "promise resolver",
        0,
        null);
    if (promiseScope.has("promiseRejectedValue", promiseScope)) {
      Object promiseRejectedValue = promiseScope.get("promiseRejectedValue", promiseScope);
      throw new JavaScriptException(promiseRejectedValue, null, 0);
    }
    if (promiseScope.has("promiseReturnValue", promiseScope)) {
      return promiseScope.get("promiseReturnValue", promiseScope);
    }
    // without an event loop, there is no way that this promise might resolve in the future
    throw new JavaScriptException("Promise did not resolve or reject", null, 0);
  }

  private Value executeRun(
      final String functionName, final Object[] arguments, final boolean executeTopLevel) {
    Context cx = Context.getCurrentContext();
    Scriptable scope = currentTopScope;
    Object[] argumentsNonNull = arguments != null ? arguments : new Object[] {};
    Object[] runArguments =
        Arrays.stream(argumentsNonNull)
            .map(
                o ->
                    o instanceof MonsterData
                        ? EnumeratedWrapper.wrap(
                            scope, MonsterProxy.class, DataTypes.makeMonsterValue((MonsterData) o))
                        : o)
            .toArray();

    return executeFunction(
        scope,
        () -> {
          Scriptable exports = null;

          if (executeTopLevel) {
            Require require = new SafeRequire(cx, scope, currentStdLib);
            if (scriptFile != null) {
              exports = require.requireMain(cx, scriptFile.toURI().toString());
            } else {
              require.install(scope);
              return cx.evaluateString(scope, scriptString, "command line", 1, null);
            }
          }
          if (functionName != null && exports != null) {
            Object defaultExport = getValidDefaultExport(exports);
            Object mainFunction =
                (defaultExport != Scriptable.NOT_FOUND)
                    ? defaultExport
                    : ScriptableObject.getProperty(exports, functionName);

            if (mainFunction instanceof Function) {
              return ((Function) mainFunction)
                  .call(cx, scope, cx.newObject(currentTopScope), runArguments);
            }
          }

          return null;
        });
  }

  public static Object getValidDefaultExport(Object exports) {
    if (!(exports instanceof NativeObject e)) return null;
    if (e.containsKey("default") && e.containsKey("__esModule")) {
      Object esm = e.get("__esModule");

      if (esm instanceof Boolean b && b) {
        return ScriptableObject.getProperty(e, "default");
      }
    }

    return Scriptable.NOT_FOUND;
  }

  public static void interruptAll() {
    for (JavascriptRuntime runtime : runningRuntimes) {
      runtime.setState(State.EXIT);
    }
  }

  public static void checkInterrupted() {
    if (Thread.interrupted()) {
      throw new JavaScriptException("Script interrupted.", null, 0);
    }
    if (!KoLmafia.permitsContinue()) {
      KoLmafia.forceContinue();
      throw new JavaScriptException("KoLmafia error: " + KoLmafia.getLastMessage(), null, 0);
    }
  }

  @Override
  public ScriptException runtimeException(final String message) {
    return new ScriptException(Context.reportRuntimeError(message).getMessage());
  }

  @Override
  public ScriptException runtimeException2(final String message1, final String message2) {
    return new ScriptException(Context.reportRuntimeError(message1 + " " + message2).getMessage());
  }

  /**
   * Escapes characters in the message that may be treated as HTML tags or entities. This allows
   * error messages to be printed to the gCLI without being inadvertently rendered as HTML.
   *
   * @param message Message that contains
   * @return Escaped string
   */
  private static String escapeHtmlInMessage(final String message) {
    return StringUtilities.getEntityEncode(message, false);
  }
}
