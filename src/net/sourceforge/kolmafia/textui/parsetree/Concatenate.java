package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ArrayList;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class Concatenate extends Expression {
  private final ArrayList<Value> strings;

  public Concatenate(final Value lhs, final Value rhs) {
    this.strings = new ArrayList<Value>();
    strings.add(lhs);
    strings.add(rhs);
  }

  @Override
  public Type getType() {
    return DataTypes.STRING_TYPE;
  }

  public void addString(final Value string) {
    strings.add(string);
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Concatenate:");
    }

    StringBuilder buffer = new StringBuilder();

    int count = 0;

    for (Value arg : this.strings) {
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Arg " + (++count) + ": " + arg);
      }

      Value value = arg.execute(interpreter);
      interpreter.captureValue(value);
      if (value == null) {
        value = DataTypes.VOID_VALUE;
      }

      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + value.toQuotedString());
      }
      interpreter.traceUnindent();

      if (interpreter.getState() == ScriptRuntime.State.EXIT) {
        interpreter.traceUnindent();
        return null;
      }

      String string = value.toStringValue().toString();
      buffer.append(string);
    }

    Value result = new Value(buffer.toString());

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("<- " + result);
    }

    interpreter.traceUnindent();
    return result;
  }

  @Override
  public String toString() {
    StringBuilder output = new StringBuilder("(");
    int count = 0;

    for (Value string : this.strings) {
      if (count++ > 0) {
        output.append(" + ");
      }
      output.append(string.toQuotedString());
    }

    output.append(")");
    return output.toString();
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<CONCATENATE>");
    for (Value string : this.strings) {
      string.print(stream, indent + 1);
    }
  }
}
