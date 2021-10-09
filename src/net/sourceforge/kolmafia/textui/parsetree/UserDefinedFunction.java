package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.RuntimeLibrary;
import org.eclipse.lsp4j.Location;

public class UserDefinedFunction extends Function {
  private Scope scope;
  private final Stack<ArrayList<Value>> callStack;

  public UserDefinedFunction(
      final String name,
      final Type type,
      final List<VariableReference> variableReferences,
      final Location location) {
    super(name, type, variableReferences, location);

    this.scope = null;
    this.callStack = new Stack<>();
  }

  public void setScope(final Scope s) {
    this.scope = s;
  }

  public Scope getScope() {
    return this.scope;
  }

  private void saveBindings(AshRuntime interpreter) {
    if (this.scope == null) {
      return;
    }

    ArrayList<Value> values = new ArrayList<>();

    for (BasicScope next : this.scope.getScopes()) {
      for (Variable current : next.getVariables()) {
        if (!current.isStatic()) {
          values.add(current.getValue(interpreter));
        }
      }
    }

    this.callStack.push(values);
  }

  private void restoreBindings(AshRuntime interpreter) {
    if (this.scope == null) {
      return;
    }

    ArrayList<Value> values = this.callStack.pop();
    int i = 0;

    for (BasicScope next : this.scope.getScopes()) {
      for (Variable current : next.getVariables()) {
        if (!current.isStatic()) {
          current.forceValue(values.get(i++));
        }
      }
    }
  }

  @Override
  public Value execute(final AshRuntime interpreter, Object[] values) {
    if (StaticEntity.isDisabled(this.getName())) {
      this.printDisabledMessage(interpreter);
      return this.type.initialValue();
    }

    if (this.scope == null) {
      throw interpreter.runtimeException("Calling undefined user function: " + this.getName());
    }

    // Save current variable bindings
    this.saveBindings(interpreter);

    // Bind values to variable references
    this.bindVariableReferences(interpreter, values);

    Value result = this.scope.execute(interpreter);

    // Restore initial variable bindings
    this.restoreBindings(interpreter);

    if (result.getType().equals(this.type.getBaseType())) {
      return result;
    }

    return this.type.initialValue();
  }

  public boolean overridesLibraryFunction() {
    Function[] functions = RuntimeLibrary.functions.findFunctions(this.name);

    for (Function function : functions) {
      if (this.paramsMatch(function)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    super.print(stream, indent);
    if (this.scope != null) {
      this.scope.print(stream, indent + 1);
    }
  }
}
