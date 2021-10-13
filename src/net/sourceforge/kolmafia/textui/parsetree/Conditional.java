package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public abstract class Conditional extends Command {
  public Scope scope;
  private final Evaluable condition;

  public Conditional(final Location location, final Scope scope, final Evaluable condition) {
    super(location);
    this.scope = scope;
    this.condition = condition;
  }

  public Scope getScope() {
    return this.scope;
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

    if (conditionResult.intValue() == 1) {
      Value result = this.scope.execute(interpreter);

      interpreter.traceUnindent();

      if (interpreter.getState() != ScriptRuntime.State.NORMAL) {
        return result;
      }

      return DataTypes.TRUE_VALUE;
    }

    interpreter.traceUnindent();
    return DataTypes.FALSE_VALUE;
  }
}
