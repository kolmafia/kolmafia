package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.Parser;

public class SwitchScope extends BasicScope {
  private final ArrayList<Command> commands = new ArrayList<>();
  private int offset = -1;
  private int barrier = BasicScope.BARRIER_SEEN;
  private boolean breakable = false;

  public SwitchScope(final BasicScope parentScope) {
    super(parentScope);
  }

  @Override
  public void addCommand(final Command c, final Parser p) {
    this.commands.add(c);
    if (this.barrier == BasicScope.BARRIER_NONE && c.assertBarrier()) {
      this.barrier = BasicScope.BARRIER_SEEN;
    } else if (this.barrier == BasicScope.BARRIER_SEEN) {
      this.barrier = BasicScope.BARRIER_PAST;
      p.warning("WARNING: Unreachable code");
    }

    if (!this.breakable) {
      this.breakable = c.assertBreakable();
    }
  }

  public void resetBarrier() {
    this.barrier = BasicScope.BARRIER_NONE;
  }

  @Override
  public Iterator<Command> getCommands() {
    return this.commands.listIterator(this.offset);
  }

  public int commandCount() {
    return this.commands.size();
  }

  public void setOffset(final int offset) {
    this.offset = offset;
  }

  @Override
  public boolean assertBarrier() {
    return this.barrier >= BasicScope.BARRIER_SEEN;
  }

  @Override
  public boolean assertBreakable() {
    return this.breakable;
  }

  public void print(
      final PrintStream stream,
      final int indent,
      Evaluable[] tests,
      Integer[] offsets,
      int defaultIndex) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<SCOPE>");

    AshRuntime.indentLine(stream, indent + 1);
    stream.println("<VARIABLES>");

    for (Variable currentVar : this.getVariables()) {
      currentVar.print(stream, indent + 2);
    }

    AshRuntime.indentLine(stream, indent + 1);
    stream.println("<COMMANDS>");

    int commandCount = this.commands.size();
    int testIndex = 0;
    int testCount = tests.length;

    for (int index = 0; index < commandCount; ++index) {
      while (testIndex < testCount) {
        Evaluable test = tests[testIndex];
        Integer offset = offsets[testIndex];
        if (offset.intValue() != index) {
          break;
        }

        AshRuntime.indentLine(stream, indent + 1);
        stream.println("<CASE>");
        test.print(stream, indent + 2);
        testIndex++;
      }

      if (defaultIndex == index) {
        AshRuntime.indentLine(stream, indent + 1);
        stream.println("<DEFAULT>");
      }

      Command command = commands.get(index);
      command.print(stream, indent + 2);
    }
  }
}
