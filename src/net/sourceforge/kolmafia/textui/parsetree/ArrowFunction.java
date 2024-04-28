package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.textui.AshRuntime;
import org.eclipse.lsp4j.Location;

public class ArrowFunction extends Function {
  private Scope scope;

  public ArrowFunction(final FunctionType type) {
    this(type, null, null);

    // Generate variables and variable references based on the parameters

    VariableList paramList = new VariableList();
    List<VariableReference> variableReferences = new ArrayList<>();
    int count = 0;
    for (Type t : type.getParameterTypes()) {
      String variableName = "V" + String.valueOf(count++);
      Variable variable = new Variable(variableName, t, null);
      paramList.add(variable);
      VariableReference variableReference = new VariableReference(null, variable);
      variableReferences.add(variableReference);
    }

    // This is the "initial value" for a FunctionType
    // Its parent is the global scope.
    this.scope = new Scope(paramList, null);
    this.setVariableReferences(variableReferences);
  }

  public ArrowFunction(
      final FunctionType type,
      final List<VariableReference> variableReferences,
      final Location location) {
    super(type, variableReferences, location);
    this.scope = null;
  }

  public void setScope(final Scope s) {
    this.scope = s;
  }

  public Scope getScope() {
    return this.scope;
  }

  @Override
  public Value execute(final AshRuntime interpreter, Object[] values) {
    if (this.scope == null) {
      throw interpreter.runtimeException("Calling undefined arrow function: " + this.getName());
    }

    // Bind values to variable references
    this.bindVariableReferences(interpreter, values);

    Value result = this.scope.execute(interpreter);
    return result;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    super.print(stream, indent);
    if (this.scope != null) {
      this.scope.print(stream, indent + 1);
    }
  }
}
