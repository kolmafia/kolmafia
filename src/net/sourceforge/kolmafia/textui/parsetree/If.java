package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class If extends Conditional {
  private final List<Conditional> elseLoops;

  public If(final Location location, final Scope scope, final Evaluable condition) {
    super(location, scope, condition);
    this.elseLoops = new ArrayList<>();
  }

  public void addElseLoop(final Conditional elseLoop) {
    this.elseLoops.add(elseLoop);
    // It would be better if we could separate the "if" from the whole "if/elseif/elseif..."
    // chain, but currently, If represents the Command all by itself, and needs to have its
    // Location, so update it.
    this.setLocation(Parser.mergeLocations(this, elseLoop));
  }

  Iterator<Conditional> getElseLoopIterator() {
    return this.elseLoops.iterator();
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    Value result = super.execute(interpreter);
    if (interpreter.getState() != ScriptRuntime.State.NORMAL || result == DataTypes.TRUE_VALUE) {
      return result;
    }

    // Conditional failed. Move to else clauses

    for (Conditional elseLoop : this.elseLoops) {
      result = elseLoop.execute(interpreter);

      if (interpreter.getState() != ScriptRuntime.State.NORMAL || result == DataTypes.TRUE_VALUE) {
        return result;
      }
    }

    return DataTypes.FALSE_VALUE;
  }

  @Override
  public String toString() {
    return "if";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<IF>");

    this.getCondition().print(stream, indent + 1);
    this.getScope().print(stream, indent + 1);

    for (Conditional currentElse : this.elseLoops) {
      currentElse.print(stream, indent);
    }
  }

  @Override
  public boolean assertBarrier() {
    // Summary: an If returns if every contained block of code
    // returns, and the final block is an Else (not an ElseIf).
    if (!this.getScope().assertBarrier()) {
      return false;
    }

    Conditional current = null;

    for (Conditional elseLoop : this.elseLoops) {
      if (!elseLoop.getScope().assertBarrier()) {
        return false;
      }
      current = elseLoop;
    }

    return current instanceof Else;
  }

  @Override
  public boolean assertBreakable() {
    if (this.getScope().assertBreakable()) {
      return true;
    }

    for (Conditional elseLoop : this.elseLoops) {
      if (elseLoop.getScope().assertBreakable()) {
        return true;
      }
    }

    return false;
  }
}
