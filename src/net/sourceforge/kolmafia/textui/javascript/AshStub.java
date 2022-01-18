package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.Function.MatchType;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

public abstract class AshStub extends BaseFunction {
  private static final long serialVersionUID = 1L;

  protected final ScriptRuntime controller;
  protected final String ashFunctionName;

  public AshStub(
      Scriptable scope, Scriptable prototype, ScriptRuntime controller, String ashFunctionName) {
    super(scope, prototype);
    this.controller = controller;
    this.ashFunctionName = ashFunctionName;
  }

  @Override
  public String getFunctionName() {
    return JavascriptRuntime.toCamelCase(ashFunctionName);
  }

  protected abstract FunctionList getAllFunctions();

  protected abstract Value execute(Function function, List<Value> ashArgs);

  private Function findMatchingFunction(List<Value> ashArgs) {
    Function function = null;
    Function[] libraryFunctions = getAllFunctions().findFunctions(ashFunctionName);

    MatchType[] matchTypes = {MatchType.EXACT, MatchType.BASE, MatchType.COERCE};
    for (MatchType matchType : matchTypes) {
      for (Function testFunction : libraryFunctions) {
        // Check for match with no vararg, then match with vararg.
        if (testFunction.paramsMatch(ashArgs, matchType, /* vararg = */ false)
            || testFunction.paramsMatch(ashArgs, matchType, /* vararg = */ true)) {
          function = testFunction;
          break;
        }
      }
      if (function != null) {
        break;
      }
    }

    return function;
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    JavascriptRuntime.checkInterrupted();

    ValueConverter coercer = new ValueConverter(cx, scope);

    // Find library function matching arguments, in two stages.
    // First, designate any arguments where we can't determine type from JS as ANY_TYPE.
    List<Value> ashArgs = new ArrayList<>();
    for (final Object original : args) {
      Value coerced = coercer.fromJava(original);
      if (coerced == null) {
        coerced = new Value(DataTypes.ANY_TYPE);
      }
      ashArgs.add(coerced);
    }
    Function function = findMatchingFunction(ashArgs);

    if (function == null) {
      throw controller.runtimeException(Parser.undefinedFunctionMessage(ashFunctionName, ashArgs));
    }

    // Second, infer the type for any missing arguments from the closest function match.
    for (int i = 0; i < ashArgs.size(); i++) {
      Object original = args[i];
      Value coerced = coercer.fromJava(original);
      if (coerced == null) {
        // Try again, this time with a type hint.
        coerced = coercer.fromJava(original, function.getVariableReferences().get(i).getType());
        if (coerced == null) {
          throw controller.runtimeException("Could not coerce argument to valid ASH value.");
        }
      }
    }
    function = findMatchingFunction(ashArgs);

    if (function == null) {
      throw controller.runtimeException(Parser.undefinedFunctionMessage(ashFunctionName, ashArgs));
    }

    Value ashReturnValue = execute(function, ashArgs);

    // Some functions will interrupt code execution on failure. In ASH this is mitigated by
    // capturing the return
    // value of those functions. In JavaScript we don't want this behaviour at all
    if (!KoLmafia.refusesContinue() && ashReturnValue != null) {
      this.controller.setState(ScriptRuntime.State.NORMAL);
      KoLmafia.forceContinue();
    }

    Object returnValue = coercer.asJava(ashReturnValue);

    JavascriptRuntime.checkInterrupted();

    if (returnValue instanceof Value
        && ((Value) returnValue).asProxy() instanceof ProxyRecordValue) {
      returnValue = EnumeratedWrapper.wrap(scope, returnValue.getClass(), (Value) returnValue);
    } else if (!(returnValue instanceof Scriptable)) {
      returnValue = Context.javaToJS(returnValue, scope);
    }

    if (returnValue instanceof NativeJavaObject) {
      throw controller.runtimeException("ASH function returned native Java object.");
    }

    return returnValue;
  }
}
