package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class TernaryExpression extends Expression {
  Evaluable conditional;

  public TernaryExpression(final Evaluable conditional, final Evaluable lhs, final Evaluable rhs) {
    super(Parser.mergeLocations(conditional.getLocation(), rhs.getLocation()));
    this.conditional = conditional;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override
  public Type getType() {
    // Ternary expressions have no real operator
    return this.lhs.getType();
  }

  @Override
  public Type getRawType() {
    // Ternary expressions have no real operator
    return this.lhs.getType();
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operator: ?:");
    }

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Condition: " + conditional);
    }
    Value conditionResult = this.conditional.execute(interpreter);
    interpreter.captureValue(conditionResult);

    if (conditionResult == null) {
      conditionResult = DataTypes.VOID_VALUE;
    }
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + conditionResult.toQuotedString());
    }
    interpreter.traceUnindent();

    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      interpreter.traceUnindent();
      return null;
    }

    Evaluable expression;
    String tag;

    if (conditionResult.intValue() != 0) {
      expression = lhs;
      tag = "True value: ";
    } else {
      expression = rhs;
      tag = "False value: ";
    }

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace(tag + expression);
    }

    Value executeResult = expression.execute(interpreter);

    if (executeResult == null) {
      executeResult = DataTypes.VOID_VALUE;
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + executeResult.toQuotedString());
    }
    interpreter.traceUnindent();

    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      interpreter.traceUnindent();
      return null;
    }

    if (Operator.isStringLike(this.lhs.getType()) != Operator.isStringLike(this.rhs.getType())) {
      executeResult = executeResult.toStringValue();
    }
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("<- " + executeResult);
    }
    interpreter.traceUnindent();

    return executeResult;
  }

  @Override
  public String toString() {
    return "( "
        + this.conditional.toQuotedString()
        + " ? "
        + this.lhs.toQuotedString()
        + " : "
        + this.rhs.toQuotedString()
        + " )";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<OPER ?:>");
    this.conditional.print(stream, indent + 1);
    super.print(stream, indent);
  }
}
