package net.sourceforge.kolmafia.textui.parsetree;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class LibraryFunction extends Function {
  private final Method method;
  public final String[] deprecationWarning;

  private LibraryFunction(
      final String name,
      final Type type,
      final ArrayList<VariableReference> variableReferences,
      final Method method,
      final String[] deprecationWarning) {
    super(name.toLowerCase(), type, variableReferences, null);

    this.method = method;
    this.deprecationWarning = deprecationWarning != null ? deprecationWarning : new String[0];
  }

  private LibraryFunction(
      final String name,
      final Type type,
      final Type[] params,
      final Method method,
      final String[] deprecationWarning) {
    // in JDK22 this constructor can be resolved and moved before the next constructor's `this` call
    this(name, type, buildVariableReferences(params, method), method, deprecationWarning);
  }

  public LibraryFunction(
      final String name,
      final Type type,
      final List<VariableReference> namedParams,
      final String... deprecationWarning) {
    this(
        name,
        type,
        new ArrayList<>(namedParams),
        findLibraryMethod(name, namedParams.size()),
        deprecationWarning);
  }

  public LibraryFunction(
      final String name, final Type type, final Type[] params, final String... deprecationWarning) {
    this(name, type, params, findLibraryMethod(name, params.length), deprecationWarning);
  }

  public LibraryFunction(final String name, final Type type, final Type[] params) {
    this(name, type, params, new String[] {});
  }

  private static Method findLibraryMethod(String name, int paramCount) {
    try {
      Class<?>[] args = new Class[paramCount + 1];

      args[0] = ScriptRuntime.class;
      Arrays.fill(args, 1, args.length, Value.class);

      return RuntimeLibrary.findMethod(name, args);
    } catch (Exception e) {
      // This should not happen; it denotes a coding
      // error that must be fixed before release.

      StaticEntity.printStackTrace(e, "No method found for built-in function: " + name);
      return null;
    }
  }

  private static ArrayList<VariableReference> buildVariableReferences(
      Type[] params, Method method) {
    if (method == null) {
      // if method is null, we already printed a stack trace in findLibraryMethod
      return new ArrayList<>();
    }

    var parameters = method.getParameters();

    var variableReferences = new ArrayList<VariableReference>();
    for (int i = 0; i < params.length; i++) {
      var parameter = parameters[i + 1];
      var variable = new Variable(parameter.getName(), params[i], null);
      variableReferences.add(new VariableReference(null, variable));
    }
    return variableReferences;
  }

  @Override
  public Value execute(final AshRuntime interpreter, Object[] values) {
    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
      return null;
    }

    if (StaticEntity.isDisabled(this.getName())) {
      this.printDisabledMessage(interpreter);
      return this.getType().initialValue();
    }

    if (this.method == null) {
      throw interpreter.runtimeException("Internal error: no method for " + this.getName());
    }

    try {
      // Bind values to variable references.
      // Collapse values into VarArgs array
      values = this.bindVariableReferences(interpreter, values);

      // Invoke the method
      return (Value) this.method.invoke(this, values);
    } catch (InvocationTargetException e) {
      // This is an error in the called method. Pass
      // it on up so that we'll print a stack trace.

      Throwable cause = e.getCause();
      if (cause instanceof ScriptException) {
        // Pass up exceptions intentionally generated by library
        throw (ScriptException) cause;
      }
      throw new RuntimeException(cause);
    } catch (IllegalAccessException e) {
      // This is not expected, but is an internal error in ASH
      throw new ScriptException(e);
    }
  }

  // This is necessary for calls into the runtime library from other languages.
  public Value executeWithoutInterpreter(ScriptRuntime controller, Object[] values) {
    if (StaticEntity.isDisabled(this.getName())) {
      RequestLogger.printLine("Called disabled function: " + this.getName());
      return this.getType().initialValue();
    }

    if (this.method == null) {
      throw controller.runtimeException("Internal error: no method for " + this.getName());
    }

    try {
      // Collapse values into VarArgs array
      values = this.bindVariableReferences(null, values);

      // Invoke the method
      return (Value) this.method.invoke(this, values);
    } catch (InvocationTargetException e) {
      // This is an error in the called method. Pass
      // it on up so that we'll print a stack trace.

      Throwable cause = e.getCause();
      if (cause instanceof ScriptException) {
        // Pass up exceptions intentionally generated by library
        throw (ScriptException) cause;
      }
      throw new RuntimeException(cause);
    } catch (IllegalAccessException e) {
      // This is not expected, but is an internal error in ASH
      throw new ScriptException(e);
    }
  }

  public List<String> getParameterNames() {
    return this.variableReferences.stream()
        .map(VariableReference::getName)
        .collect(Collectors.toList());
  }
}
