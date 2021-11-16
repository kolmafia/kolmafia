package net.sourceforge.kolmafia.textui.parsetree;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public abstract class Loop extends Command {
  private final Scope scope;

  public Loop(final Location location, final Scope scope) {
    super(location);
    this.scope = scope;
  }

  public Scope getScope() {
    return this.scope;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    Value result = this.scope.execute(interpreter);

    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
    }

    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      return null;
    }

    if (interpreter.getState() == ScriptRuntime.State.BREAK) {
      // Stay in state; subclass exits loop
      return DataTypes.VOID_VALUE;
    }

    if (interpreter.getState() == ScriptRuntime.State.CONTINUE) {
      // Done with this iteration
      interpreter.setState(ScriptRuntime.State.NORMAL);
    }

    if (interpreter.getState() == ScriptRuntime.State.RETURN) {
      // Stay in state; subclass exits loop
      return result;
    }

    return result;
  }
}
