package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class WhileLoop extends Loop {
  private final Evaluable condition;

  public WhileLoop(final Location location, final Scope scope, final Evaluable condition) {
    super(location, scope);
    this.condition = condition;
  }

  public Evaluable getCondition() {
    return this.condition;
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
    }

    while (true) {
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Test: " + this.condition);
      }

      Value conditionResult = this.condition.execute(interpreter);
      interpreter.captureValue(conditionResult);

      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + conditionResult);
      }

      if (conditionResult == null) {
        interpreter.traceUnindent();
        return null;
      }

      if (conditionResult.intValue() != 1) {
        break;
      }

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
    }

    interpreter.traceUnindent();
    return DataTypes.VOID_VALUE;
  }

  @Override
  public boolean assertBarrier() {
    return this.condition.evaluatesTo(DataTypes.TRUE_VALUE) && !this.getScope().assertBreakable();
  }

  @Override
  public String toString() {
    return "while";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<WHILE>");
    this.getCondition().print(stream, indent + 1);
    this.getScope().print(stream, indent + 1);
  }
}
