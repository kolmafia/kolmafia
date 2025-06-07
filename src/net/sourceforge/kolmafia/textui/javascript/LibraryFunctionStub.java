package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.combat.Macrofier;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.LibraryFunction;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class LibraryFunctionStub extends AshStub {
  private static final long serialVersionUID = 1L;

  private final List<String> bufferFunctions = List.of("buffer_to_file", "write_ccs");

  public LibraryFunctionStub(
      Scriptable scope, Scriptable prototype, ScriptRuntime controller, String ashFunctionName) {
    super(scope, prototype, controller, ashFunctionName);
  }

  @Override
  protected FunctionList getAllFunctions() {
    return RuntimeLibrary.functions;
  }

  @Override
  protected Value execute(Function function, List<Value> ashArgs) {
    LibraryFunction ashFunction;
    if (function instanceof LibraryFunction) {
      ashFunction = (LibraryFunction) function;
    } else {
      throw controller.runtimeException(Parser.undefinedFunctionMessage(ashFunctionName, ashArgs));
    }

    List<Object> ashArgsWithInterpreter = new ArrayList<>(ashArgs.size() + 1);
    ashArgsWithInterpreter.add(controller);
    ashArgsWithInterpreter.addAll(ashArgs);

    try {
      return ashFunction.executeWithoutInterpreter(controller, ashArgsWithInterpreter.toArray());
    } catch (Throwable e) {
      // ensure the exception can be caught in Javascript
      throw Context.throwAsScriptRuntimeEx(e);
    }
  }

  private int findFunctionReference(Object[] args) {
    int index = -1;

    switch (ashFunctionName) {
      case "adventure":
      case "adv1":
        index = 2;
        break;
      case "run_combat":
        index = 0;
        break;
      default:
        return index;
    }

    return (args.length >= (index + 1) && args[index] instanceof BaseFunction) ? index : -1;
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    if (ashFunctionName.equals("to_string") && args.length == 0) {
      // Special case, since we accidentally override JS's built-in toString.
      return "[runtime library]";
    }

    // Named function references don't really make sense in JavaScript, so for any function that is
    // supplied such,
    // instead allow an actual
    int functionReferenceArgIndex = findFunctionReference(args);
    if (functionReferenceArgIndex >= 0) {
      BaseFunction callback = (BaseFunction) args[functionReferenceArgIndex];
      Macrofier.setJavaScriptMacroOverride(callback, scope, thisObj);
      args[functionReferenceArgIndex] = "[" + callback.toString() + "]";
    }

    return super.call(cx, scope, thisObj, args);
  }
}
