package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.List;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class JavaForLoop extends Loop {
  private final List<Assignment> initializers;
  private final Evaluable condition;
  private final List<Command> incrementers;

  public JavaForLoop(
      final Location location,
      final Scope scope,
      final List<Assignment> initializers,
      final Evaluable condition,
      final List<Command> incrementers) {
    super(location, scope);
    this.initializers = initializers;
    this.condition = condition;
    this.incrementers = incrementers;
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

    // For all variable references, bind to initial value
    for (Assignment initializer : initializers) {
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Initialize: " + initializer.getLeftHandSide());
      }

      Value value = initializer.execute(interpreter);
      interpreter.captureValue(value);

      if (value == null) {
        value = DataTypes.VOID_VALUE;
      }

      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + value);
      }

      if (interpreter.getState() == ScriptRuntime.State.EXIT) {
        interpreter.traceUnindent();
        return null;
      }
    }

    while (true) {
      // Test the exit condition
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

      // Execute the loop body
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

      // Execute incrementers
      for (Command incrementer : this.incrementers) {
        Value iresult = incrementer.execute(interpreter);

        // Abort processing now if command failed
        if (!KoLmafia.permitsContinue()) {
          interpreter.setState(ScriptRuntime.State.EXIT);
        }

        if (ScriptRuntime.isTracing()) {
          interpreter.trace("[" + interpreter.getState() + "] <- " + iresult.toQuotedString());
        }

        if (interpreter.getState() != ScriptRuntime.State.NORMAL) {
          interpreter.traceUnindent();
          return DataTypes.VOID_VALUE;
        }
      }
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
    stream.println("<FOR>");

    for (Assignment initializer : initializers) {
      initializer.print(stream, indent + 1);
    }
    this.getCondition().print(stream, indent + 1);
    for (Command incrementer : this.incrementers) {
      AshRuntime.indentLine(stream, indent + 1);
      stream.println("<ITERATE>");
      incrementer.print(stream, indent + 2);
    }
    this.getScope().print(stream, indent + 1);
  }
}
