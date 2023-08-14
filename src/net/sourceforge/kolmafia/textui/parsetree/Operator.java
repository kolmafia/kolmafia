package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.DataTypes.TypeSpec;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import org.eclipse.lsp4j.Location;

public class Operator extends Command {
  final String operator;

  // For runtime error messages
  private final String fileName;
  private final int lineNumber;

  public Operator(final Location location, final String operator, final Parser parser) {
    super(location);
    this.operator = operator;
    this.fileName = parser.getShortFileName();
    this.lineNumber = parser.getLineNumber();
  }

  public boolean equals(final String op) {
    return this.operator.equals(op);
  }

  public boolean precedes(final Operator oper) {
    return this.operStrength() > oper.operStrength();
  }

  private int operStrength() {
    if (this.operator.equals(Parser.POST_INCREMENT)
        || this.operator.equals(Parser.POST_DECREMENT)) {
      return 14;
    }

    if (this.operator.equals("!")
        || this.operator.equals("~")
        || this.operator.equals("contains")
        || this.operator.equals("remove")
        || this.operator.equals(Parser.PRE_INCREMENT)
        || this.operator.equals(Parser.PRE_DECREMENT)) {
      return 13;
    }

    if (this.operator.equals("**")) {
      return 12;
    }

    if (this.operator.equals("*") || this.operator.equals("/") || this.operator.equals("%")) {
      return 11;
    }

    if (this.operator.equals("+") || this.operator.equals("-")) {
      return 10;
    }

    if (this.operator.equals("<<") || this.operator.equals(">>") || this.operator.equals(">>>")) {
      return 9;
    }

    if (this.operator.equals("<")
        || this.operator.equals(">")
        || this.operator.equals("<=")
        || this.operator.equals(">=")) {
      return 8;
    }

    if (this.operator.equals("==")
        || this.operator.equals(Parser.APPROX)
        || this.operator.equals("!=")) {
      return 7;
    }

    if (this.operator.equals("&")) {
      return 6;
    }

    if (this.operator.equals("^")) {
      return 5;
    }

    if (this.operator.equals("|")) {
      return 4;
    }

    if (this.operator.equals("&&")) {
      return 3;
    }

    if (this.operator.equals("||")) {
      return 2;
    }

    if (this.operator.equals("?") || this.operator.equals(":")) {
      return 1;
    }

    return -1;
  }

  public boolean isArithmetic() {
    return this.operator.equals("+")
        || this.operator.equals("-")
        || this.operator.equals("*")
        || this.operator.equals("/")
        || this.operator.equals("%")
        || this.operator.equals("**")
        || this.operator.equals(Parser.PRE_INCREMENT)
        || this.operator.equals(Parser.PRE_DECREMENT)
        || this.operator.equals(Parser.POST_INCREMENT)
        || this.operator.equals(Parser.POST_DECREMENT);
  }

  public boolean isBoolean() {
    return this.operator.equals("&&") || this.operator.equals("||");
  }

  public boolean isLogical() {
    return this.operator.equals("&")
        || this.operator.equals("|")
        || this.operator.equals("^")
        || this.operator.equals("~")
        || this.operator.equals("&=")
        || this.operator.equals("^=")
        || this.operator.equals("|=");
  }

  public boolean isInteger() {
    return this.operator.equals("<<")
        || this.operator.equals(">>")
        || this.operator.equals(">>>")
        || this.operator.equals("<<=")
        || this.operator.equals(">>=")
        || this.operator.equals(">>>=");
  }

  public boolean isComparison() {
    return this.operator.equals("==")
        || this.operator.equals(Parser.APPROX)
        || this.operator.equals("!=")
        || this.operator.equals("<")
        || this.operator.equals(">")
        || this.operator.equals("<=")
        || this.operator.equals(">=");
  }

  @Override
  public String toString() {
    return this.operator;
  }

  public boolean validCoercion(Type lhs, Type rhs) {
    TypeSpec ltype = lhs.getBaseType().getType();
    TypeSpec rtype = rhs.getBaseType().getType();

    if (lhs.isBad() || rhs.isBad()) {
      // BadNode's are only generated through errors, which
      // means one was already generated about this type.
      return true;
    }

    if (this.isInteger()) {
      return (ltype == TypeSpec.INT && rtype == TypeSpec.INT);
    }
    if (this.isBoolean()) {
      return ltype == rtype && (ltype == TypeSpec.BOOLEAN);
    }
    if (this.isLogical()) {
      return ltype == rtype && (ltype == TypeSpec.INT || ltype == TypeSpec.BOOLEAN);
    }
    return Operator.validCoercion(lhs, rhs, this.toString());
  }

  public static boolean validCoercion(Type lhs, Type rhs, final String oper) {
    // Resolve aliases
    lhs = lhs.getBaseType();
    rhs = rhs.getBaseType();

    if (lhs.isBad() || rhs.isBad()) {
      // BadNode's are only generated through errors, which
      // means one was already generated about this type.
      return true;
    }

    if (oper == null) {
      return lhs.getType() == rhs.getType();
    }

    // "oper" is either a standard operator or is a special name:
    //
    // "parameter" - value used as a function parameter
    //	lhs = parameter type, rhs = expression type
    //
    // "return" - value returned as function value
    //	lhs = function return type, rhs = expression type
    //
    // "assign" - value
    //	lhs = variable type, rhs = expression type

    // The "contains" operator requires an aggregate on the left
    // and the correct index type on the right.

    if (oper.equals("contains")) {
      return lhs.getType() == TypeSpec.AGGREGATE
          && validCoercion(((AggregateType) lhs).getIndexType().getBaseType(), rhs, "==");
    }

    // If the types are equal, no coercion is necessary
    if (lhs.equals(rhs)) {
      return true;
    }

    if (lhs.equals(DataTypes.ANY_TYPE)) {
      return true;
    }

    // Noncoercible strings only accept strings
    if (lhs.equals(DataTypes.STRICT_STRING_TYPE)) {
      return rhs.equals(TypeSpec.STRING) || rhs.equals(TypeSpec.BUFFER);
    }

    // Anything coerces to a string
    if (lhs.equals(TypeSpec.STRING)) {
      return true;
    }

    // Anything coerces to a string for concatenation
    if (oper.equals("+") && rhs.equals(TypeSpec.STRING)) {
      return true;
    }

    // Int coerces to float
    if ((lhs.equals(TypeSpec.INT) && rhs.equals(TypeSpec.FLOAT))
        || (lhs.equals(TypeSpec.FLOAT) && rhs.equals(TypeSpec.INT))) {
      return true;
    }

    if ((lhs.equals(TypeSpec.PATH) && rhs.equals(TypeSpec.INT))
        || (lhs.equals(TypeSpec.INT) && rhs.equals(TypeSpec.PATH))) {
      return true;
    }

    if (lhs.equals(TypeSpec.PATH) && rhs.equals(TypeSpec.STRING)) {
      return true;
    }

    return false;
  }

  private Value compareValues(final AshRuntime interpreter, Value leftValue, Value rightValue) {
    var c =
        this.operator.equals(Parser.APPROX)
            ? leftValue.compareToIgnoreCase(rightValue)
            : leftValue.compareTo(rightValue);

    var result =
        switch (this.operator) {
              case "==" -> leftValue.equals(rightValue);
              case "!=" -> !leftValue.equals(rightValue);
              case Parser.APPROX -> leftValue.equalsIgnoreCase(rightValue);
              case ">=" -> leftValue.compareTo(rightValue) >= 0;
              case "<=" -> leftValue.compareTo(rightValue) <= 0;
              case ">" -> leftValue.compareTo(rightValue) > 0;
              case "<" -> leftValue.compareTo(rightValue) < 0;
              default -> false;
            }
            ? DataTypes.TRUE_VALUE
            : DataTypes.FALSE_VALUE;

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("<- " + result);
    }
    interpreter.traceUnindent();

    return result;
  }

  private Value performArithmetic(final AshRuntime interpreter, Value leftValue, Value rightValue) {
    Type ltype = leftValue.getType();
    Type rtype = rightValue.getType();
    Value result;

    // If either side is non-numeric, perform string operations
    if (ltype.isStringLike() || rtype.isStringLike()) {
      // Since we only do string concatenation, we should
      // only get here if the operator is "+".
      if (!this.operator.equals("+")) {
        throw interpreter.runtimeException(
            "Operator '" + this.operator + "' applied to string operands",
            this.fileName,
            this.lineNumber);
      }

      String string = leftValue.toStringValue().toString() + rightValue.toStringValue().toString();
      result = new Value(string);
    }

    // If either value is a float, coerce to float

    else if (ltype.equals(TypeSpec.FLOAT) || rtype.equals(TypeSpec.FLOAT)) {
      double rfloat = rightValue.toFloatValue().floatValue();
      if ((this.operator.equals("/") || this.operator.equals("%")) && rfloat == 0.0) {
        throw interpreter.runtimeException("Division by zero", this.fileName, this.lineNumber);
      }

      double lfloat = leftValue.toFloatValue().floatValue();

      double val;

      if (this.operator.equals("**")) {
        val = Math.pow(lfloat, rfloat);
        if (Double.isNaN(val) || Double.isInfinite(val)) {
          throw interpreter.runtimeException(
              "Invalid exponentiation: cannot take " + lfloat + " ** " + rfloat,
              this.fileName,
              this.lineNumber);
        }
      } else {
        val =
            this.operator.equals("+")
                ? lfloat + rfloat
                : this.operator.equals("-")
                    ? lfloat - rfloat
                    : this.operator.equals("*")
                        ? lfloat * rfloat
                        : this.operator.equals("/")
                            ? lfloat / rfloat
                            : this.operator.equals("%") ? lfloat % rfloat : 0.0;
      }

      result = DataTypes.makeFloatValue(val);
    }

    // If this is a logical operator, return an int or boolean
    else if (this.isLogical()) {
      long lint = leftValue.intValue();
      long rint = rightValue.intValue();
      long val =
          this.operator.equals("&")
              ? lint & rint
              : this.operator.equals("^")
                  ? lint ^ rint
                  : this.operator.equals("|") ? lint | rint : 0;
      result =
          ltype.equals(TypeSpec.BOOLEAN)
              ? DataTypes.makeBooleanValue(val != 0)
              : DataTypes.makeIntValue(val);
    }

    // Otherwise, perform arithmetic on integers

    else {
      long rint = rightValue.intValue();
      if ((this.operator.equals("/") || this.operator.equals("%")) && rint == 0) {
        throw interpreter.runtimeException("Division by zero", this.fileName, this.lineNumber);
      }

      long lint = leftValue.intValue();
      long val =
          this.operator.equals("+")
              ? lint + rint
              : this.operator.equals("-")
                  ? lint - rint
                  : this.operator.equals("*")
                      ? lint * rint
                      : this.operator.equals("/")
                          ? lint / rint
                          : this.operator.equals("%")
                              ? lint % rint
                              : this.operator.equals("**")
                                  ? (long) Math.pow(lint, rint)
                                  : this.operator.equals("<<")
                                      ? lint << rint
                                      : this.operator.equals(">>")
                                          ? lint >> rint
                                          : this.operator.equals(">>>") ? lint >>> rint : 0;
      result = DataTypes.makeIntValue(val);
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("<- " + result);
    }
    interpreter.traceUnindent();
    return result;
  }

  public Value applyTo(final AshRuntime interpreter, final TypedNode lhs) {
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operator: " + this.operator);
    }

    // Unary operator with special evaluation of argument
    if (this.operator.equals("remove")) {
      CompositeReference operand = (CompositeReference) lhs;
      if (ScriptRuntime.isTracing()) {
        interpreter.traceIndent();
        interpreter.trace("Operand: " + operand);
        interpreter.traceUnindent();
      }
      Value result = operand.removeKey(interpreter);
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("<- " + result);
      }
      interpreter.traceUnindent();
      return result;
    }

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operand: " + lhs);
    }

    Value leftValue = lhs.execute(interpreter);
    interpreter.captureValue(leftValue);
    if (leftValue == null) {
      leftValue = DataTypes.VOID_VALUE;
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + leftValue.toQuotedString());
    }
    interpreter.traceUnindent();

    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      interpreter.traceUnindent();
      return null;
    }

    Value result;

    // Unary Operators
    switch (this.operator) {
      case "!":
        result = DataTypes.makeBooleanValue(leftValue.intValue() == 0);
        break;
      case "~":
        long val = leftValue.intValue();
        result =
            leftValue.getType().equals(TypeSpec.BOOLEAN)
                ? DataTypes.makeBooleanValue(val == 0)
                : DataTypes.makeIntValue(~val);
        break;
      case "-":
        if (lhs.getType().equals(TypeSpec.INT)) {
          result = DataTypes.makeIntValue(0 - leftValue.intValue());
        } else if (lhs.getType().equals(TypeSpec.FLOAT)) {
          result = DataTypes.makeFloatValue(0.0 - leftValue.floatValue());
        } else {
          throw interpreter.runtimeException(
              "Internal error: Unary minus can only be applied to numbers",
              this.fileName,
              this.lineNumber);
        }
        break;
      case Parser.PRE_INCREMENT:
      case Parser.POST_INCREMENT:
        if (lhs.getType().equals(TypeSpec.INT)) {
          result = DataTypes.makeIntValue(leftValue.intValue() + 1);
        } else if (lhs.getType().equals(TypeSpec.FLOAT)) {
          result = DataTypes.makeFloatValue(leftValue.floatValue() + 1.0);
        } else {
          throw interpreter.runtimeException(
              "Internal error: pre/post increment can only be applied to numbers",
              this.fileName,
              this.lineNumber);
        }
        break;
      case Parser.PRE_DECREMENT:
      case Parser.POST_DECREMENT:
        if (lhs.getType().equals(TypeSpec.INT)) {
          result = DataTypes.makeIntValue(leftValue.intValue() - 1);
        } else if (lhs.getType().equals(TypeSpec.FLOAT)) {
          result = DataTypes.makeFloatValue(leftValue.floatValue() - 1.0);
        } else {
          throw interpreter.runtimeException(
              "Internal error: pre/post increment can only be applied to numbers",
              this.fileName,
              this.lineNumber);
        }
        break;
      default:
        throw interpreter.runtimeException(
            "Internal error: unknown unary operator \"" + this.operator + "\"",
            this.fileName,
            this.lineNumber);
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("<- " + result);
    }

    interpreter.traceUnindent();
    return result;
  }

  public Value applyTo(final AshRuntime interpreter, final TypedNode lhs, final TypedNode rhs) {
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operator: " + this.operator);
    }

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operand 1: " + lhs);
    }

    Value leftValue = lhs.execute(interpreter);
    interpreter.captureValue(leftValue);
    if (leftValue == null) {
      leftValue = DataTypes.VOID_VALUE;
    }
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + leftValue.toQuotedString());
    }
    interpreter.traceUnindent();

    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      interpreter.traceUnindent();
      return null;
    }

    // Unknown operator
    if (rhs == null) {
      throw interpreter.runtimeException(
          "Internal error: missing right operand.", this.fileName, this.lineNumber);
    }

    // Binary operators with optional right values
    if (this.operator.equals("||")) {
      if (leftValue.intValue() == 1) {
        if (ScriptRuntime.isTracing()) {
          interpreter.trace("<- " + DataTypes.TRUE_VALUE);
        }
        interpreter.traceUnindent();
        return DataTypes.TRUE_VALUE;
      }
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Operand 2: " + rhs);
      }
      Value rightValue = rhs.execute(interpreter);
      interpreter.captureValue(rightValue);
      if (rightValue == null) {
        rightValue = DataTypes.VOID_VALUE;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + rightValue.toQuotedString());
      }
      interpreter.traceUnindent();
      if (interpreter.getState() == ScriptRuntime.State.EXIT) {
        interpreter.traceUnindent();
        return null;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("<- " + rightValue);
      }
      interpreter.traceUnindent();
      return rightValue;
    }

    if (this.operator.equals("&&")) {
      if (leftValue.intValue() == 0) {
        interpreter.traceUnindent();
        if (ScriptRuntime.isTracing()) {
          interpreter.trace("<- " + DataTypes.FALSE_VALUE);
        }
        return DataTypes.FALSE_VALUE;
      }
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Operand 2: " + rhs);
      }
      Value rightValue = rhs.execute(interpreter);
      interpreter.captureValue(rightValue);
      if (rightValue == null) {
        rightValue = DataTypes.VOID_VALUE;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + rightValue.toQuotedString());
      }
      interpreter.traceUnindent();
      if (interpreter.getState() == ScriptRuntime.State.EXIT) {
        interpreter.traceUnindent();
        return null;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("<- " + rightValue);
      }
      interpreter.traceUnindent();
      return rightValue;
    }

    // Ensure type compatibility of operands
    if (!this.validCoercion(lhs.getType(), rhs.getType())) {
      throw interpreter.runtimeException(
          "Internal error: left hand side and right hand side do not correspond",
          this.fileName,
          this.lineNumber);
    }

    // Special binary operator: <aggref> contains <any>
    if (this.operator.equals("contains")) {
      interpreter.traceIndent();
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("Operand 2: " + rhs);
      }
      Value rightValue = rhs.execute(interpreter);
      interpreter.captureValue(rightValue);
      if (rightValue == null) {
        rightValue = DataTypes.VOID_VALUE;
      }
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("[" + interpreter.getState() + "] <- " + rightValue.toQuotedString());
      }
      interpreter.traceUnindent();
      if (interpreter.getState() == ScriptRuntime.State.EXIT) {
        interpreter.traceUnindent();
        return null;
      }
      Value result = DataTypes.makeBooleanValue(leftValue.contains(rightValue));
      if (ScriptRuntime.isTracing()) {
        interpreter.trace("<- " + result);
      }
      interpreter.traceUnindent();
      return result;
    }

    // Binary operators
    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Operand 2: " + rhs);
    }
    Value rightValue = rhs.execute(interpreter);
    interpreter.captureValue(rightValue);
    if (rightValue == null) {
      rightValue = DataTypes.VOID_VALUE;
    }
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("[" + interpreter.getState() + "] <- " + rightValue.toQuotedString());
    }
    interpreter.traceUnindent();
    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      interpreter.traceUnindent();
      return null;
    }

    // Comparison operators
    if (this.isComparison()) {
      return this.compareValues(interpreter, leftValue, rightValue);
    }

    // Arithmetic operators
    if (this.isArithmetic() || this.isLogical() || this.isInteger()) {
      return this.performArithmetic(interpreter, leftValue, rightValue);
    }

    // Unknown operator
    throw interpreter.runtimeException(
        "Internal error: unknown binary operator \"" + this.operator + "\"",
        this.fileName,
        this.lineNumber);
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    return null;
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<OPER " + this.operator + ">");
  }
}
