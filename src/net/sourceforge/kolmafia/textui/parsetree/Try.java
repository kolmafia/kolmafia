package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class Try extends Command {
  private final Scope body, finalClause;

  public Try(final Location location, final Scope body, final Scope finalClause) {
    super(location);
    this.body = body;
    this.finalClause = finalClause;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    // We can't catch script ABORTs
    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
      return null;
    }

    Value result = DataTypes.VOID_VALUE;
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Entering try body");
    }

    try {
      result = this.body.execute(interpreter);
    } catch (Exception e) {
      // *** Here is where we would look at catch blocks and find which, if any
      // *** will handle this exception.
      //
      // If no catch block swallows this error, propagate it upwards
      interpreter.traceUnindent();
      throw e;
    } finally {
      if (this.finalClause != null) {
        ScriptRuntime.State oldState = interpreter.getState();
        boolean userAborted = StaticEntity.userAborted;
        MafiaState continuationState = StaticEntity.getContinuationState();

        KoLmafia.forceContinue();
        interpreter.setState(ScriptRuntime.State.NORMAL);

        if (ScriptRuntime.isTracing()) {
          interpreter.trace("Entering finally, saved state: " + oldState);
        }

        this.finalClause.execute(interpreter);

        // Unless the finally block aborted, restore previous state
        if (!KoLmafia.refusesContinue()) {
          interpreter.setState(oldState);
          StaticEntity.setContinuationState(continuationState);
          StaticEntity.userAborted = userAborted;
        }
      }
    }

    interpreter.traceUnindent();
    return result;
  }

  @Override
  public boolean assertBarrier() {
    return this.body.assertBarrier() || this.finalClause.assertBarrier();
  }

  @Override
  public boolean assertBreakable() {
    return this.body.assertBreakable() || this.finalClause.assertBreakable();
  }

  @Override
  public String toString() {
    return "try";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<TRY>");

    this.body.print(stream, indent + 1);

    if (this.finalClause != null) {
      AshRuntime.indentLine(stream, indent);
      stream.println("<FINALLY>");

      this.finalClause.print(stream, indent + 1);
    }
  }
}
