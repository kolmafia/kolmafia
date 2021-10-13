package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class ForEachLoop extends Loop {
  private final List<VariableReference> variableReferences;
  private final Evaluable aggregate;

  // For runtime error messages
  String fileName;
  int lineNumber;

  public ForEachLoop(
      final Location location,
      final Scope scope,
      final List<VariableReference> variableReferences,
      final Evaluable aggregate,
      final Parser parser) {
    super(location, scope);
    this.variableReferences = variableReferences;
    this.aggregate = aggregate;
    this.fileName = parser.getShortFileName();
    this.lineNumber = parser.getLineNumber();
  }

  public List<VariableReference> getVariableReferences() {
    return this.variableReferences;
  }

  public Evaluable getAggregate() {
    return this.aggregate;
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

    // Evaluate the aggref to get the slice
    AggregateValue slice = (AggregateValue) this.aggregate.execute(interpreter);
    interpreter.captureValue(slice);
    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      interpreter.traceUnindent();
      return null;
    }

    // Iterate over the slice with bound keyvar

    ListIterator<VariableReference> it = this.variableReferences.listIterator();
    Value retval = this.executeSlice(interpreter, slice, it, it.next());

    if (interpreter.getState() == ScriptRuntime.State.BREAK) {
      interpreter.setState(ScriptRuntime.State.NORMAL);
    }

    return retval;
  }

  private Value executeSlice(
      final AshRuntime interpreter,
      final AggregateValue slice,
      final ListIterator<VariableReference> it,
      final VariableReference variable) {
    // Get the next key variable
    VariableReference nextVariable = it.hasNext() ? it.next() : null;

    // If the slice is an AggregateLiteral, must execute it to
    // initialize the values.
    if (slice instanceof AggregateLiteral) {
      slice.execute(interpreter);
    }

    // Get an iterator over the keys for the slice
    Iterator<Value> keys = slice.iterator();

    int stackPos = interpreter.iterators.size();
    interpreter.iterators.add(null); // key
    interpreter.iterators.add(slice); // map
    interpreter.iterators.add(keys); // iterator

    // While there are further keys
    while (keys.hasNext()) {
      // Get current key
      Value key;

      try {
        key = keys.next();
        interpreter.iterators.set(stackPos, key);
      } catch (ConcurrentModificationException e) {
        interpreter.setLineAndFile(this.fileName, this.lineNumber);
        throw interpreter.runtimeException("Map modified within foreach");
      }

      // Bind variable to key
      variable.setValue(interpreter, key);

      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Key: " + key);
      }

      // If there are more indices to bind, recurse
      Value result;
      if (nextVariable != null) {
        Value nextSlice = slice.aref(key, interpreter);
        if (nextVariable.getType() instanceof AggregateType) {
          // typedef, for example
          nextVariable.setValue(interpreter, nextSlice);
          result = super.execute(interpreter);
        } else if (nextSlice instanceof AggregateValue) {
          interpreter.traceIndent();
          result = this.executeSlice(interpreter, (AggregateValue) nextSlice, it, nextVariable);
        } else // value var instead of key var
        {
          nextVariable.setValue(interpreter, nextSlice);
          result = super.execute(interpreter);
        }
      } else {
        // Otherwise, execute scope
        result = super.execute(interpreter);
      }

      if (interpreter.getState() == ScriptRuntime.State.NORMAL) {
        continue;
      }

      if (nextVariable != null) {
        it.previous();
      }

      interpreter.traceUnindent();
      interpreter.iterators.remove(stackPos + 2);
      interpreter.iterators.remove(stackPos + 1);
      interpreter.iterators.remove(stackPos);
      return result;
    }

    if (nextVariable != null) {
      it.previous();
    }

    interpreter.traceUnindent();
    interpreter.iterators.remove(stackPos + 2);
    interpreter.iterators.remove(stackPos + 1);
    interpreter.iterators.remove(stackPos);
    return DataTypes.VOID_VALUE;
  }

  @Override
  public String toString() {
    return "foreach";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<FOREACH>");

    for (VariableReference current : this.getVariableReferences()) {
      current.print(stream, indent + 1);
    }

    this.getAggregate().print(stream, indent + 1);
    this.getScope().print(stream, indent + 1);
  }
}
