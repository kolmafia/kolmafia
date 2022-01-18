package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.List;
import net.sourceforge.kolmafia.textui.AshRuntime;
import org.eclipse.lsp4j.Location;

public class VariableReference extends Evaluable implements Comparable<VariableReference> {
  public final Variable target;

  public VariableReference(final Location location, final Variable target) {
    super(location);
    this.target = target;
  }

  @Override
  public Type getType() {
    return this.target.getBaseType();
  }

  @Override
  public Type getRawType() {
    return this.target.getType();
  }

  public String getName() {
    return this.target.getName();
  }

  public List<Evaluable> getIndices() {
    return null;
  }

  @Override
  public int compareTo(final VariableReference o) {
    return this.target.getName().compareTo(o.target.getName());
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    return this.target.getValue(interpreter);
  }

  public Value getValue(AshRuntime interpreter) {
    return this.target.getValue(interpreter);
  }

  public void forceValue(final Value targetValue) {
    this.target.forceValue(targetValue);
  }

  public Value setValue(final AshRuntime interpreter, final Value targetValue) {
    return this.setValue(interpreter, targetValue, null);
  }

  public Value setValue(AshRuntime interpreter, final Value targetValue, final Operator oper) {
    Value newValue = targetValue;
    if (oper != null) {
      Value currentValue = this.target.getValue(interpreter);
      newValue = oper.applyTo(interpreter, currentValue, targetValue);
    }
    if (newValue != null) {
      this.target.setValue(interpreter, newValue);
    }
    return newValue;
  }

  @Override
  public String toString() {
    return this.target.getName();
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<VARREF> " + this.getName());
  }

  public static VariableReference badVariableReference(final Location location) {
    return VariableReference.badVariableReference(location, new Type.BadType(null, null));
  }

  public static VariableReference badVariableReference(final Location location, final Type type) {
    return new VariableReference(location, new Variable.BadVariable(null, type, null));
  }
}
