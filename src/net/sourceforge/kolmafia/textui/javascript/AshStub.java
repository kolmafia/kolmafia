package net.sourceforge.kolmafia.textui.javascript;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.AggregateType;
import net.sourceforge.kolmafia.textui.parsetree.Function;
import net.sourceforge.kolmafia.textui.parsetree.Function.MatchType;
import net.sourceforge.kolmafia.textui.parsetree.FunctionList;
import net.sourceforge.kolmafia.textui.parsetree.ProxyRecordValue;
import net.sourceforge.kolmafia.textui.parsetree.Type;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

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

  private Function findMatchingFunction(List<Value> ashArgs, boolean coerceAnyType) {
    Function[] libraryFunctions = getAllFunctions().findFunctions(ashFunctionName);

    if (coerceAnyType && ashArgs.stream().noneMatch(v -> v.getType() == DataTypes.ANY_TYPE)) {
      coerceAnyType = false;
    }
    List<Value> coercedArgs = coerceAnyType ? new ArrayList<>(ashArgs) : ashArgs;

    MatchType[] matchTypes = {MatchType.EXACT, MatchType.BASE, MatchType.COERCE};
    for (MatchType matchType : matchTypes) {
      for (Function testFunction : libraryFunctions) {
        if (coerceAnyType) {
          for (int i = 0; i < ashArgs.size(); i++) {
            if (ashArgs.get(i).getType() == DataTypes.ANY_TYPE) {
              Type expectedType = testFunction.getVariableReferences().get(i).getType();
              Value coerced = new Value(expectedType);
              coercedArgs.set(i, coerced);
            }
          }
        }

        if (testFunction.paramsMatch(coercedArgs, matchType)) {
          return testFunction;
        }
      }
    }

    return null;
  }

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    JavascriptRuntime.checkInterrupted();

    // strip trailing undefined arguments, allowing to pass undefined for optional arguments
    // If we ever support undefined values, this will have to become a bit more elaborate, but for
    // now every undefined value leads to an error anyway, so this is fine.
    int definedArgs = args.length;
    while (definedArgs > 0 && Undefined.isUndefined(args[definedArgs - 1])) {
      definedArgs -= 1;
    }

    ScriptableValueConverter coercer = new ScriptableValueConverter(cx, scope);

    // Find library function matching arguments, in two stages.
    // First, designate any arguments where we can't determine type (or aggregate type) from JS
    // as ANY_TYPE, which will be replaced in findMatchingFunction with the target type
    // to force a match. This is mainly relevant for empty arrays and records.
    List<Value> ashArgs = new ArrayList<>();
    for (int i = 0; i < definedArgs; i++) {
      Object original = args[i];
      if (Undefined.isUndefined(original)) {
        throw new EvaluatorException("Passing undefined to an ASH function is not supported.");
      }
      if (original == null) {
        throw new EvaluatorException("Passing null to an ASH function is not supported.");
      }
      try {
        Value coerced = coercer.fromJava(original);
        if (coerced == null
            || (coerced.getType() instanceof AggregateType agg
                && agg.getDataType().equals(DataTypes.ANY_TYPE))) {
          coerced = new Value(DataTypes.ANY_TYPE);
        }
        ashArgs.add(coerced);
      } catch (ValueConverter.ValueConverterException e) {
        throw new EvaluatorException(e.getMessage());
      }
    }
    Function function = findMatchingFunction(ashArgs, true);

    if (function == null) {
      throw controller.runtimeException(Parser.undefinedFunctionMessage(ashFunctionName, ashArgs));
    }

    // Second, infer the type for any ANY_TYPE arguments from the closest function match.
    boolean argsChanged = false;
    for (int i = 0; i < ashArgs.size(); i++) {
      if (ashArgs.get(i).getType() != DataTypes.ANY_TYPE) {
        continue;
      }
      Object original = args[i];
      // Try again, this time with a type hint.
      Type typeHint = function.getVariableReferences().get(i).getType();
      Value coerced = coercer.fromJava(original, typeHint);
      if (coerced == null || coerced.getType() == DataTypes.ANY_TYPE) {
        throw controller.runtimeException("Could not coerce argument to valid ASH value.");
      }
      ashArgs.set(i, coerced);
      argsChanged = true;
    }
    if (argsChanged) {
      function = findMatchingFunction(ashArgs, false);

      if (function == null) {
        throw controller.runtimeException(
            Parser.undefinedFunctionMessage(ashFunctionName, ashArgs));
      }
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
