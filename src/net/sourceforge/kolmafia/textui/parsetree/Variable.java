package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import org.eclipse.lsp4j.Location;

public class Variable extends Symbol {
  Type type;
  Value content;
  Evaluable expression = null;
  boolean isStatic = false;

  public Variable(final Type type) {
    this(null, type, null);
  }

  public Variable(final String name, final Type type, final Location location) {
    super(name, location);
    this.type = type;
    this.content = new Value(type);
  }

  public Type getType() {
    return this.type;
  }

  public Type getBaseType() {
    return this.type.getBaseType();
  }

  public boolean isStatic() {
    return this.isStatic;
  }

  public void markStatic() {
    this.isStatic = true;
  }

  public Value getValue(final AshRuntime interpreter) {
    if (this.expression != null) {
      this.content = this.expression.execute(interpreter);
    }

    return this.content;
  }

  public Type getValueType(final AshRuntime interpreter) {
    return this.getValue(interpreter).getType();
  }

  public Object rawValue(final AshRuntime interpreter) {
    return this.getValue(interpreter).rawValue();
  }

  public long intValue(final AshRuntime interpreter) {
    return this.getValue(interpreter).intValue();
  }

  public Value toStringValue(final AshRuntime interpreter) {
    return this.getValue(interpreter).toStringValue();
  }

  public double floatValue(final AshRuntime interpreter) {
    return this.getValue(interpreter).floatValue();
  }

  public void setExpression(final Evaluable targetExpression) {
    this.expression = targetExpression;
  }

  public void forceValue(final Value targetValue) {
    this.content = targetValue;
    this.expression = null;
  }

  public void setValue(AshRuntime interpreter, final Value targetValue) {
    if (this.getBaseType().equals(DataTypes.ANY_TYPE)
        || this.getBaseType().equals(targetValue.getType())) {
      this.content = targetValue;
      this.expression = null;
    } else if (this.getBaseType().equals(DataTypes.TYPE_STRICT_STRING)
        || this.getBaseType().equals(DataTypes.TYPE_STRING)) {
      this.content = targetValue.toStringValue();
      this.expression = null;
    } else if (this.getBaseType().equals(DataTypes.TYPE_INT)
        && targetValue.getType().equals(DataTypes.TYPE_FLOAT)) {
      this.content = targetValue.toIntValue();
      this.expression = null;
    } else if (this.getBaseType().equals(DataTypes.TYPE_FLOAT)
        && targetValue.getType().equals(DataTypes.TYPE_INT)) {
      this.content = targetValue.toFloatValue();
      this.expression = null;
    } else {
      throw interpreter.runtimeException(
          "Internal error: Cannot assign " + targetValue.getType() + " to " + this.getType());
    }
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    return getValue(interpreter);
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<VAR " + this.getType() + " " + this.getName() + ">");
  }

  public static class BadVariable extends Variable implements BadNode {
    public BadVariable(final String name, final Type type, final Location location) {
      super(name, type, location);
    }
  }
}
