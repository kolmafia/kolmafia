package net.sourceforge.kolmafia.textui.javascript;

import static org.mozilla.javascript.ScriptableObject.DONTENUM;
import static org.mozilla.javascript.ScriptableObject.PERMANENT;
import static org.mozilla.javascript.ScriptableObject.READONLY;

import java.util.List;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;

/**
 * Runs before a module body to move our default exports from the module's prototype chain into its
 * parent scope chain.
 *
 * <p>Rhino rejects a top-level {@code const Item = ...} if {@code Item} is visible on the scope's
 * prototype chain, where its module loader puts the global scope. Names reached through the parent
 * scope chain resolve identically but are not redeclarations, so a script can use ours or declare
 * its own.
 */
public class ScopeIsolationScript implements Script {
  /** Globals that ES makes non-configurable, so a script may not declare over them. */
  private static final List<String> RESTRICTED_GLOBALS = List.of("undefined", "NaN", "Infinity");

  private final Scriptable globalScope;

  public ScopeIsolationScript(Scriptable globalScope) {
    this.globalScope = globalScope;
  }

  @Override
  public Object exec(Context cx, Scriptable scope, Scriptable thisObj) {
    isolate(scope, globalScope);
    return null;
  }

  public static TopLevel newScriptScope(Scriptable globalScope) {
    var scope = new TopLevel();
    scope.setPrototype(globalScope);
    scope.cacheBuiltins(globalScope, false);
    isolate(scope, globalScope);
    return scope;
  }

  /**
   * Reparents a script scope so the global scope is a parent rather than a prototype. The scope
   * must already have cached its builtins.
   */
  private static void isolate(Scriptable scriptScope, Scriptable globalScope) {
    // Undeclared assignments land in the top scope, so give each script its own.
    var parent = new TopLevel();
    parent.setPrototype(globalScope);
    parent.cacheBuiltins(globalScope, false);

    scriptScope.setPrototype(null);
    scriptScope.setParentScope(parent);

    // Dropping the prototype lost Rhino's only reason to refuse these.
    for (var name : RESTRICTED_GLOBALS) {
      ScriptableObject.defineProperty(
          scriptScope,
          name,
          ScriptableObject.getProperty(globalScope, name),
          DONTENUM | READONLY | PERMANENT);
    }
  }
}
