package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.Arrays;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class SortBy extends Command {
  private final VariableReference aggregate;
  private final Variable indexvar, valuevar;
  private final Evaluable expr;

  // For runtime error messages
  String fileName;
  int lineNumber;

  public SortBy(
      final Location location,
      final VariableReference aggregate,
      final Variable indexvar,
      final Variable valuevar,
      final Evaluable expr,
      final Parser parser) {
    super(location);
    this.aggregate = aggregate;
    this.indexvar = indexvar;
    this.valuevar = valuevar;
    this.expr = expr;
    this.fileName = parser.getShortFileName();
    this.lineNumber = parser.getLineNumber();
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

    AggregateValue map = (AggregateValue) this.aggregate.execute(interpreter);
    interpreter.captureValue(map);

    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      interpreter.traceUnindent();
      return null;
    }

    Value[] keys = map.keys();
    Pair[] values = new Pair[keys.length];

    for (int i = 0; i < keys.length; ++i) {
      Value index = keys[i];
      this.indexvar.setValue(interpreter, index);
      Value value = map.aref(index, interpreter);
      this.valuevar.setValue(interpreter, value);
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Element #" + i + ": " + index + " = " + value);
      }
      Value sortkey = this.expr.execute(interpreter);
      if (interpreter.getState() == ScriptRuntime.State.EXIT) {
        interpreter.traceUnindent();
        return null;
      }
      interpreter.captureValue(sortkey);
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Key = " + sortkey);
      }
      values[i] = new Pair(sortkey, value);
    }

    try {
      Arrays.sort(values);
    } catch (IllegalArgumentException e) {
      interpreter.setLineAndFile(this.fileName, this.lineNumber);
      throw interpreter.runtimeException("Illegal argument exception during sort");
    }

    for (int i = 0; i < keys.length; ++i) {
      Value index = keys[i];
      map.aset(index, values[i].value, interpreter);
    }

    interpreter.traceUnindent();
    return DataTypes.VOID_VALUE;
  }

  @Override
  public String toString() {
    return "sort";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<SORT>");
    this.aggregate.print(stream, indent + 1);
    this.expr.print(stream, indent + 1);
  }

  private static class Pair implements Comparable<Pair> {
    public Value key, value;

    public Pair(Value key, Value value) {
      this.key = key;
      this.value = value;
    }

    public int compareTo(Pair o) {
      return this.key.compareTo(o.key);
    }
  }
}
