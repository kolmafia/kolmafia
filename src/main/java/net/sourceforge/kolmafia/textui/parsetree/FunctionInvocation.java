package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import java.util.List;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class FunctionInvocation extends FunctionCall {
  private final BasicScope scope;
  private final Evaluable name;
  private final Type type;

  public FunctionInvocation(
      final Location location,
      final BasicScope scope,
      final Type type,
      final Evaluable name,
      final List<Evaluable> params,
      final Parser parser) {
    super(location, null, params, parser);
    this.scope = scope;
    this.type = type;
    this.name = name;
  }

  @Override
  public Type getType() {
    return this.type;
  }

  @Override
  public Type getRawType() {
    return this.type;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
      return null;
    }

    interpreter.traceIndent();

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Invoke: " + this);
      interpreter.trace("Function name: " + this.name);
    }

    // Get the function name
    Value funcValue = this.name.execute(interpreter);

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + funcValue);
    }

    if (funcValue == null) {
      interpreter.traceUnindent();
      return null;
    }

    interpreter.setLineAndFile(this.fileName, this.lineNumber);

    String func = funcValue.toString();
    Function function = this.scope.findFunction(func, this.params);
    if (function == null) {
      throw interpreter.undefinedFunctionException(func, this.params);
    }

    if (!Operator.validCoercion(this.type, function.getType(), "return")) {
      throw interpreter.runtimeException(
          "Calling \""
              + func
              + "\", which returns "
              + function.getType()
              + " but "
              + this.type
              + " expected");
    }

    this.target = function;

    // Invoke it.
    Value result = super.execute(interpreter);
    interpreter.traceUnindent();

    return result;
  }

  @Override
  public String toString() {
    return "call " + this.type.toString() + " " + this.name.toString() + "()";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<INVOKE " + this.name.toString() + ">");
    this.type.print(stream, indent + 1);

    for (Evaluable current : this.params) {
      current.print(stream, indent + 1);
    }
  }
}
