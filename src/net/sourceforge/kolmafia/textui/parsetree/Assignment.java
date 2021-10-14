package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Range;

public class Assignment extends Evaluable {
  private final VariableReference lhs;
  private final Evaluable rhs;
  private final Operator oper;

  public Assignment(final VariableReference lhs, final Evaluable rhs) {
    this(lhs, rhs, null);
  }

  public Assignment(final VariableReference lhs, final Evaluable rhs, final Operator oper) {
    super(
        rhs == null
            ? lhs.getLocation()
            : new Location(
                lhs.getLocation().getUri(),
                new Range(
                    lhs.getLocation().getRange().getStart(),
                    rhs.getLocation().getRange().getEnd())));
    this.lhs = lhs;
    this.rhs = rhs;
    this.oper = oper;
  }

  public VariableReference getLeftHandSide() {
    return this.lhs;
  }

  public Evaluable getRightHandSide() {
    return this.rhs == null
        ? Value.locate(this.lhs.getLocation(), this.lhs.getType().initialValueExpression())
        : this.rhs;
  }

  @Override
  public Type getType() {
    return this.lhs.getType();
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
      return null;
    }

    Value value;

    if (this.rhs == null) {
      value = this.lhs.getType().initialValue();
    } else {
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Eval: " + this.rhs);
      }

      value = this.rhs.execute(interpreter);
      interpreter.captureValue(value);

      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Set: " + value);
      }
      interpreter.traceUnindent();
    }

    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      return null;
    }

    Value newValue;
    if (this.lhs.getType().equals(DataTypes.TYPE_STRING)) {
      newValue = this.lhs.setValue(interpreter, value.toStringValue(), oper);
    } else if (this.lhs.getType().equals(DataTypes.TYPE_INT)) {
      newValue = this.lhs.setValue(interpreter, value.toIntValue(), oper);
    } else if (this.lhs.getType().equals(DataTypes.TYPE_FLOAT)) {
      newValue = this.lhs.setValue(interpreter, value.toFloatValue(), oper);
    } else if (this.lhs.getType().equals(DataTypes.TYPE_BOOLEAN)) {
      newValue = this.lhs.setValue(interpreter, value.toBooleanValue(), oper);
    } else {
      newValue = this.lhs.setValue(interpreter, value);
    }

    return newValue;
  }

  @Override
  public String toString() {
    return this.rhs == null ? this.lhs.getName() : this.lhs.getName() + " = " + this.rhs;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<ASSIGN " + this.lhs.getName() + ">");
    VariableReference lhs = this.getLeftHandSide();
    Parser.printIndices(lhs.getIndices(), stream, indent + 1);
    if (this.oper != null) {
      AshRuntime.indentLine(stream, indent);
      stream.println("<OPER " + this.oper.operator + "=>");
    }
    this.getRightHandSide().print(stream, indent + 1);
  }
}
