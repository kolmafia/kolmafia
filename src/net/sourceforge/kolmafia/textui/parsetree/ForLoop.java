package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class ForLoop extends Loop {
  private final VariableReference variable;
  private final Evaluable initial;
  private final Evaluable last;
  private final Evaluable increment;
  private final int direction;
  private final String fileName;
  private final int lineNumber;

  public ForLoop(
      final Location location,
      final Scope scope,
      final VariableReference variable,
      final Evaluable initial,
      final Evaluable last,
      final Evaluable increment,
      final int direction,
      final Parser parser) {
    super(location, scope);
    this.variable = variable;
    this.initial = initial;
    this.last = last;
    this.increment = increment;
    this.direction = direction;
    this.fileName = parser.getShortFileName();
    this.lineNumber = parser.getLineNumber();
  }

  public VariableReference getVariable() {
    return this.variable;
  }

  public Evaluable getInitial() {
    return this.initial;
  }

  public Evaluable getLast() {
    return this.last;
  }

  public Evaluable getIncrement() {
    return this.increment;
  }

  public int getDirection() {
    return this.direction;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
      return null;
    }

    interpreter.traceIndent();

    if (ScriptRuntime.isTracing()) {
      interpreter.trace(this.toString());
      interpreter.trace("Initial: " + this.initial);
    }

    // Get the initial value
    Value initialValue = this.initial.execute(interpreter);
    interpreter.captureValue(initialValue);

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + initialValue);
    }

    if (initialValue == null) {
      interpreter.traceUnindent();
      return null;
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Last: " + this.last);
    }

    // Get the final value
    Value lastValue = this.last.execute(interpreter);
    interpreter.captureValue(lastValue);

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + lastValue);
    }

    if (lastValue == null) {
      interpreter.traceUnindent();
      return null;
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Increment: " + this.increment);
    }

    // Get the increment
    Value incrementValue = this.increment.execute(interpreter);
    interpreter.captureValue(incrementValue);

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + incrementValue);
    }

    if (incrementValue == null) {
      interpreter.traceUnindent();
      return null;
    }

    long current = initialValue.intValue();
    long increment = incrementValue.intValue();
    long end = lastValue.intValue();

    boolean up;

    if (this.direction > 0) {
      up = true;
    } else if (this.direction < 0) {
      up = false;
    } else {
      up = current <= end;
    }

    if (up && increment < 0 || !up && increment > 0) {
      increment = -increment;
    }

    // Make sure the loop will eventually terminate

    if (current != end && increment == 0) {
      throw interpreter.runtimeException(
          "Start not equal to end and increment equals 0", this.fileName, this.lineNumber);
    }

    while (up && current <= end || !up && current >= end) {
      // Bind variable to current value
      this.variable.setValue(interpreter, new Value(current));

      // Execute the scope
      Value result = super.execute(interpreter);

      if (interpreter.getState() == ScriptRuntime.State.BREAK) {
        interpreter.setState(ScriptRuntime.State.NORMAL);
        interpreter.traceUnindent();
        return DataTypes.VOID_VALUE;
      }

      if (interpreter.getState() != ScriptRuntime.State.NORMAL) {
        interpreter.traceUnindent();
        return result;
      }

      // Calculate next value
      current += increment;
    }

    interpreter.traceUnindent();
    return DataTypes.VOID_VALUE;
  }

  @Override
  public String toString() {
    return "for";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    int direction = this.getDirection();
    stream.println("<FOR " + (direction < 0 ? "downto" : direction > 0 ? "upto" : "to") + " >");
    this.getVariable().print(stream, indent + 1);
    this.getInitial().print(stream, indent + 1);
    this.getLast().print(stream, indent + 1);
    this.getIncrement().print(stream, indent + 1);
    this.getScope().print(stream, indent + 1);
  }
}
