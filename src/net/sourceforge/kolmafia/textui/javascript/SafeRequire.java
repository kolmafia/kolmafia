package net.sourceforge.kolmafia.textui.javascript;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.UserDefinedFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

public class SafeRequire extends Require {
  private static final long serialVersionUID = 1L;

  private final Scriptable stdLib;

  public SafeRequire(Context cx, Scriptable nativeScope, Scriptable stdLib) {
    super(
        cx,
        nativeScope,
        new SoftCachingModuleScriptProvider(
            new UrlModuleSourceProvider(
                Arrays.asList(
                    KoLConstants.ROOT_LOCATION.toURI(), KoLConstants.SCRIPT_LOCATION.toURI()),
                null)),
        null,
        new MainWarningScript(),
        true);
    this.stdLib = stdLib;
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (args == null || args.length < 1 || !(args[0] instanceof String)) {
      throw new ScriptException("require() needs one argument, a string");
    }

    String path = (String) args[0];
    if (path.equals("kolmafia")) {
      return stdLib;
    } else if (path.endsWith(".ash")) {
      Scriptable exports = cx.newObject(scope);

      List<File> scriptFiles = KoLmafiaCLI.findScriptFile(path);
      List<File> validScriptFiles =
          scriptFiles.stream()
              .filter(
                  f -> {
                    try {
                      return f.getCanonicalPath()
                          .startsWith(KoLConstants.ROOT_LOCATION.getCanonicalPath());
                    } catch (IOException e) {
                      KoLmafia.updateDisplay(
                          MafiaState.ERROR, "Could not resolve path " + f.getPath());
                      return false;
                    }
                  })
              .collect(Collectors.toList());
      AshRuntime interpreter = (AshRuntime) KoLmafiaASH.getInterpreter(validScriptFiles);

      if (interpreter == null) {
        throw new ScriptException("Module \"" + path + "\" not found.");
      }

      for (Function f : interpreter.getFunctions()) {
        UserDefinedFunction userDefinedFunction = (UserDefinedFunction) f;
        String functionName = userDefinedFunction.getName();
        String functionNameCamelCase = JavascriptRuntime.toCamelCase(functionName);
        if (!ScriptableObject.hasProperty(exports, functionNameCamelCase)) {
          UserDefinedFunctionStub stub = new UserDefinedFunctionStub(interpreter, functionName);
          int attributes =
              ScriptableObject.DONTENUM | ScriptableObject.PERMANENT | ScriptableObject.READONLY;
          ScriptableObject.defineProperty(exports, functionNameCamelCase, stub, attributes);
        }
      }

      interpreter.execute(null, null);

      return exports;
    } else {
      // Require itself checks sandboxing.
      return super.call(cx, scope, thisObj, args);
    }
  }
}
