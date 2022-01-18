package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class CompositeReference extends VariableReference {
  private final List<Evaluable> indices;

  // Derived from indices: Final slice and index into it
  private CompositeValue slice;
  private Value index;

  // For runtime error messages
  private final String fileName;
  private final int lineNumber;

  public CompositeReference(
      final Location location,
      final Variable target,
      final List<Evaluable> indices,
      final Parser parser) {
    super(location, target);
    this.indices = indices;
    this.fileName = parser.getShortFileName();
    this.lineNumber = parser.getLineNumber();
  }

  @Override
  public Type getType() {
    Type type = this.target.getType().getBaseType();

    for (Evaluable current : this.indices) {
      type = type.asProxy();

      if (type instanceof CompositeType) {
        type = ((CompositeType) type).getDataType(current).getBaseType();
      } else if (!type.isBad()) {
        type = new Type.BadType(null, null);
      }
    }
    return type;
  }

  @Override
  public Type getRawType() {
    Type type = this.target.getType();

    for (Evaluable current : this.indices) {
      type = type.getBaseType().asProxy();

      if (type instanceof CompositeType) {
        type = ((CompositeType) type).getDataType(current);
      } else if (!type.isBad()) {
        type = new Type.BadType(null, null);
      }
    }
    return type;
  }

  @Override
  public String getName() {
    return this.target.getName() + "[]";
  }

  @Override
  public List<Evaluable> getIndices() {
    return this.indices;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    interpreter.setLineAndFile(this.fileName, this.lineNumber);
    return this.getValue(interpreter);
  }

  // Evaluate all the indices and step through the slices.
  //
  // When done, this.slice has the final slice and this.index has
  // the final evaluated index.

  private boolean getSlice(final AshRuntime interpreter) {
    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
      return false;
    }

    this.slice = (CompositeValue) Value.asProxy(this.target.getValue(interpreter));
    this.index = null;

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("AREF: " + this.slice.toString());
    }

    Iterator<Evaluable> it = this.indices.iterator();

    for (int i = 0; it.hasNext(); ++i) {
      Evaluable exp = it.next();

      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Key #" + (i + 1) + ": " + exp.toQuotedString());
      }

      this.index = exp.execute(interpreter);
      interpreter.captureValue(this.index);
      if (this.index == null) {
        this.index = DataTypes.VOID_VALUE;
      }

      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + this.index.toQuotedString());
      }
      interpreter.traceUnindent();

      if (interpreter.getState() == ScriptRuntime.State.EXIT) {
        interpreter.traceUnindent();
        return false;
      }

      if (it.hasNext()) {
        CompositeValue result =
            (CompositeValue) Value.asProxy(this.slice.aref(this.index, interpreter));

        // Create missing intermediate slices
        if (result == null) { // ...but don't actually save a proxy in the parent object
          Value temp = this.slice.initialValue(this.index);
          this.slice.aset(this.index, temp, interpreter);
          result = (CompositeValue) Value.asProxy(temp);
        }

        this.slice = result;

        if (ScriptRuntime.isTracing()) {
          interpreter.trace("AREF <- " + this.slice.toString());
        }
      }
    }

    interpreter.traceUnindent();

    return true;
  }

  @Override
  public synchronized Value getValue(final AshRuntime interpreter) {
    interpreter.setLineAndFile(this.fileName, this.lineNumber);
    // Iterate through indices to final slice
    if (this.getSlice(interpreter)) {
      Value result = this.slice.aref(this.index, interpreter);

      if (result == null) {
        result = this.slice.initialValue(this.index);
      }

      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("AREF <- " + result.toQuotedString());
      }
      interpreter.traceUnindent();

      return result;
    }

    return null;
  }

  @Override
  public synchronized Value setValue(
      AshRuntime interpreter, final Value targetValue, final Operator oper) {
    interpreter.setLineAndFile(this.fileName, this.lineNumber);
    // Iterate through indices to final slice
    if (this.getSlice(interpreter)) {
      Value newValue = targetValue;

      interpreter.traceIndent();

      if (oper != null) {
        Value currentValue = this.slice.aref(this.index, interpreter);

        if (currentValue == null) {
          currentValue = this.slice.initialValue(this.index);
          this.slice.aset(this.index, currentValue, interpreter);
        }

        if (ScriptRuntime.isTracing()) {
          interpreter.trace("AREF <- " + currentValue.toQuotedString());
        }

        newValue = oper.applyTo(interpreter, currentValue, targetValue);
      }

      this.slice.aset(this.index, newValue, interpreter);

      if (ScriptRuntime.isTracing()) {
        interpreter.trace("ASET: " + newValue.toQuotedString());
      }

      interpreter.traceUnindent();

      return newValue;
    }

    return null;
  }

  public synchronized Value removeKey(final AshRuntime interpreter) {
    interpreter.setLineAndFile(this.fileName, this.lineNumber);
    // Iterate through indices to final slice
    if (this.getSlice(interpreter)) {
      Value result = this.slice.remove(this.index, interpreter);
      if (result == null) {
        result = this.slice.initialValue(this.index);
      }
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("remove <- " + result.toQuotedString());
      }
      interpreter.traceUnindent();
      return result;
    }
    return null;
  }

  public boolean contains(final AshRuntime interpreter, final Value index) {
    interpreter.setLineAndFile(this.fileName, this.lineNumber);
    boolean result = false;
    // Iterate through indices to final slice
    if (this.getSlice(interpreter)) {
      result = this.slice.aref(index, interpreter) != null;
    }
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("contains <- " + result);
    }
    interpreter.traceUnindent();
    return result;
  }

  @Override
  public String toString() {
    return this.target.getName() + "[]";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<AGGREF " + this.getName() + ">");
    Parser.printIndices(this.getIndices(), stream, indent + 1);
  }
}
