package net.sourceforge.kolmafia.textui.javascript;

import java.util.Map;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.textui.AbstractRuntime;
import net.sourceforge.kolmafia.textui.ScriptException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.commonjs.module.ModuleScope;

public class JsPostLoadScript implements Script {
  // This is a slight hack to perform checks after loading a JS file. At this time, we warn folks
  // who have failed to export main() and check for script/notify/since directives.
  @Override
  public Object exec(Context cx, Scriptable scope, Scriptable thisObj) {
    Object requireObject = ScriptableObject.getProperty(scope, "require");
    Object moduleObject = ScriptableObject.getProperty(scope, "module");
    if (!(requireObject instanceof Scriptable) || !(moduleObject instanceof Scriptable)) {
      return null;
    }

    // This is the main module if require.main === module.exports.
    Object requireMain = ScriptableObject.getProperty((Scriptable) requireObject, "main");
    Object moduleExports = ScriptableObject.getProperty((Scriptable) moduleObject, "exports");
    if (!(moduleExports instanceof Scriptable)) {
      return null;
    }

    // Handle any metadata that is present.
    handleMetadata((Scriptable) moduleExports, scope);

    if (JavascriptRuntime.getValidDefaultExport(moduleExports) != Scriptable.NOT_FOUND) {
      return null;
    }

    if (requireMain != Scriptable.NOT_FOUND
        && requireMain == moduleObject
        && ScriptableObject.getProperty(scope, "main") != Scriptable.NOT_FOUND
        && ScriptableObject.getProperty((Scriptable) moduleExports, "main")
            == Scriptable.NOT_FOUND) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Warning: You defined 'main' in this script, but did not export it. "
              + "You may want to set module.exports.main = main in order for it to run.");
    }
    return null;
  }

  private void handleMetadata(Scriptable exports, Scriptable scope) {
    try {
      Object metadataObj = exports.get("__metadata", exports);
      if (metadataObj instanceof Map metadata && scope instanceof ModuleScope moduleScope) {
        String scriptFilename = moduleScope.getUri().getPath().substring(1);
        String scriptShortFilename = scriptFilename.substring(scriptFilename.lastIndexOf("/") + 1);
        String scriptName = scriptShortFilename;

        if (metadata.get("script") instanceof String _scriptName) {
          scriptName = _scriptName;
        }

        if (metadata.get("since") instanceof String since) {
          AbstractRuntime.SinceStatus response =
              AbstractRuntime.handleSince(since, scriptShortFilename);
          if (!response.status().equals("OK")) {
            throw new ScriptException(
                String.format("In <%s>: %s", scriptShortFilename, response.message()));
          }
        }

        if (metadata.get("notify") instanceof String notify) {
          AbstractRuntime.handleNotify(scriptFilename, scriptName, notify);
        }
      }
    } catch (ScriptException e) {
      throw e;
    } catch (Exception e) {
      // Apart from the ScriptException we intend to throw, never ever let this handling propagate
      // an unexpected exception.
      String errMsg = "Unexpected error while handling metadata: " + e.getMessage();
      KoLmafia.updateDisplay(errMsg);
      RequestLogger.updateSessionLog(errMsg);
    }
  }
}
