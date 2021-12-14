package net.sourceforge.kolmafia.textui.parsetree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.textui.Parser;

public class Scope extends BasicScope {
  private final ArrayList<Command> commands;
  private int barrier = BasicScope.BARRIER_NONE;
  private boolean breakable = false;

  public Scope(VariableList variables, final BasicScope parentScope) {
    super(variables, parentScope);
    this.commands = new ArrayList<>();
  }

  public Scope(final BasicScope parentScope) {
    super(parentScope);
    this.commands = new ArrayList<>();
  }

  public Scope(FunctionList functions, VariableList variables, TypeList types) {
    super(functions, variables, types, null);
    this.commands = new ArrayList<>();
  }

  @Override
  public void addCommand(final Command c, final Parser p) {
    if (c == null) {
      // We shouldn't be called with no command, but don't do
      // the wrong thing with one.
      return;
    }

    this.commands.add(c);

    if (this.barrier == BasicScope.BARRIER_NONE && c.assertBarrier()) {
      this.barrier = BasicScope.BARRIER_SEEN;
    } else if (this.barrier == BasicScope.BARRIER_SEEN
        && !(c
            instanceof
            FunctionReturn)) { // A return statement after a barrier is temporarily allowed,
      // since they were previously required in some cases that didn't
      // really need them.
      this.barrier = BasicScope.BARRIER_PAST;
      p.warning("WARNING: Unreachable code");
    }

    if (!this.breakable) {
      this.breakable = c.assertBreakable();
    }
  }

  public List<Command> getCommandList() {
    return this.commands;
  }

  @Override
  public Iterator<Command> getCommands() {
    return this.commands.iterator();
  }

  @Override
  public boolean assertBarrier() {
    return this.barrier >= BasicScope.BARRIER_SEEN;
  }

  @Override
  public boolean assertBreakable() {
    return this.breakable;
  }
}
