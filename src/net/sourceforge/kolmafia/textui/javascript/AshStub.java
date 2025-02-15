package net.sourceforge.kolmafia.textui.javascript;

import java.util.Arrays;
import java.util.List;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Function;
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

  @Override
  public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
    JavascriptRuntime.checkInterrupted();

    ScriptableValueConverter coercer = new ScriptableValueConverter(cx, scope);
    Value ashReturnValue;
    try {
      var functionWithArgs =
          coercer.findMatchingFunctionConvertArgs(getAllFunctions(), ashFunctionName, args);
      if (functionWithArgs == null || functionWithArgs.function() == null) {
        // Convert arguments as best we can, and return undefined.
        var ashArgsGuess =
            Arrays.stream(args)
                .map(
                    (arg) -> {
                      try {
                        Value value = coercer.fromJava(arg);
                        return value == null ? new Value(DataTypes.ANY_TYPE) : value;
                      } catch (ValueConverter.ValueConverterException e) {
                        return new Value(DataTypes.ANY_TYPE);
                      }
                    })
                .toList();
        throw controller.runtimeException(
            Parser.undefinedFunctionMessage(ashFunctionName, ashArgsGuess));
      }

      ashReturnValue = execute(functionWithArgs.function(), functionWithArgs.ashArgs());
    } catch (ValueConverter.ValueConverterException e) {
      throw controller.runtimeException(e.getMessage());
    }

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
