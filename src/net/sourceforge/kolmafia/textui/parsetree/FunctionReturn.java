package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class FunctionReturn extends Command {
  private final Value returnValue;
  private final Type expectedType;

  public FunctionReturn(final Location location, final Value returnValue, final Type expectedType) {
    super(location);
    this.returnValue = returnValue;
    this.expectedType = expectedType;
  }

  public Type getType() {
    if (this.expectedType != null) {
      return this.expectedType;
    }

    if (this.returnValue == null) {
      return DataTypes.VOID_TYPE;
    }

    return this.returnValue.getType();
  }

  public Value getExpression() {
    return this.returnValue;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
    }

    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      return null;
    }

    if (this.returnValue == null) {
      interpreter.setState(ScriptRuntime.State.RETURN);
      return null;
    }

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Eval: " + this.returnValue);
    }

    Value result = this.returnValue.execute(interpreter);
    interpreter.captureValue(result);

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Returning: " + result);
    }
    interpreter.traceUnindent();

    if (result == null) {
      return null;
    }

    if (interpreter.getState() != ScriptRuntime.State.EXIT) {
      interpreter.setState(ScriptRuntime.State.RETURN);
    }

    if (this.expectedType == null) {
      return result;
    }

    if (this.expectedType.equals(DataTypes.TYPE_STRING)) {
      return result.toStringValue();
    }

    if (this.expectedType.equals(DataTypes.TYPE_FLOAT)) {
      return result.toFloatValue();
    }

    if (this.expectedType.equals(DataTypes.TYPE_INT)) {
      return result.toIntValue();
    }

    return result;
  }

  @Override
  public String toString() {
    return "return " + this.returnValue;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<RETURN " + this.getType() + ">");
    if (!this.getType().equals(DataTypes.TYPE_VOID)) {
      this.returnValue.print(stream, indent + 1);
    }
  }

  @Override
  public boolean assertBarrier() {
    return true;
  }
}
